#!/usr/bin/env python3
"""
summarize_work_orders.py

Reads the orders and work orders currently held by the running
telecom-provisioning-demo application and prints a Markdown summary:

  1. Counts grouped by status (orders, and the work orders created for them)
  2. Details of every work order, with its work specs
  3. Orders that produced no work orders, with the reason

Work orders are NOT in data.sql. They exist only in the application's
in-memory H2 database (tables provisioning_request, work_order and
work_order_spec), so the application must be running. The script reads them
through the H2 console the application serves at /h2-console, using only
SELECT statements. It never changes any data.

This is meant to be run BY a skill (see provisioning-work-orders-summarizer
SKILL.md), so the summary always reflects what is actually saved rather than
a hand-maintained copy.

Usage:
    python3 scripts/summarize_work_orders.py
    python3 scripts/summarize_work_orders.py --base-url http://localhost:8081
    python3 scripts/summarize_work_orders.py --status COMPLETED
"""

import argparse
import html
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser

# Order of the lifecycle in RequestStatus.java; statuses with no orders are still listed
STATUSES = ["RECEIVED", "COMPLETED", "REJECTED", "FAILED"]

# The H2 console stops at 1000 rows unless told otherwise
MAX_ROWS = 100000

ORDERS_SQL = """
SELECT id, order_id, customer_id, request_type, status, status_detail, received_at
FROM provisioning_request
ORDER BY id
"""

WORK_ORDERS_SQL = """
SELECT w.id, w.request_id, w.work_order_number, w.dispatch_group, w.sequence_no, w.created_at,
       s.line_no, s.work_spec_code, s.duration_minutes, s.required_skill
FROM work_order w
LEFT JOIN work_order_spec s ON s.work_order_id = w.id
ORDER BY w.request_id, w.sequence_no, s.line_no
"""


class ResultSetParser(HTMLParser):
    """
    Pulls the header and rows out of the <table class="resultSet"> that the
    H2 console returns for a query. A SQL NULL is rendered as <i>null</i>
    and is returned as None.
    """

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.found_table = False
        self.columns = []
        self.rows = []
        self._in_table = False
        self._row = None
        self._cell = None
        self._cell_is_header = False
        self._cell_is_null = False

    def handle_starttag(self, tag, attrs):
        if tag == "table" and dict(attrs).get("class") == "resultSet":
            self.found_table = True
            self._in_table = True
        elif not self._in_table:
            return
        elif tag == "tr":
            self._row = []
        elif tag in ("th", "td"):
            self._cell = ""
            self._cell_is_header = tag == "th"
            self._cell_is_null = False
        elif tag == "i" and self._cell is not None:
            self._cell_is_null = True
        elif tag == "br" and self._cell is not None:
            self._cell += "\n"

    def handle_data(self, data):
        if self._cell is not None:
            self._cell += data

    def handle_endtag(self, tag):
        if not self._in_table:
            return
        if tag in ("th", "td") and self._cell is not None:
            if self._cell_is_header:
                self.columns.append(self._cell.strip().lower())
            else:
                self._row.append(None if self._cell_is_null else self._cell.strip())
            self._cell = None
        elif tag == "tr" and self._row is not None:
            if self._row:
                self.rows.append(self._row)
            self._row = None
        elif tag == "table":
            self._in_table = False


class H2Console:
    """A logged-in session with the application's H2 console."""

    def __init__(self, base_url, jdbc_url, user, password):
        self.console_url = base_url.rstrip("/") + "/h2-console"
        self.session_id = self._open_session()
        page = self._post("login.do", {
            "language": "en",
            "setting": "Generic H2 (Embedded)",
            "name": "Generic H2 (Embedded)",
            "driver": "org.h2.Driver",
            "url": jdbc_url,
            "user": user,
            "password": password,
        })
        # A successful login answers with the console's frameset; a failed one shows the login page again
        if "<frameset" not in page:
            fail(f"could not log in to the database {jdbc_url} as '{user}'. " + console_message(page, 'class="error"'))
        self._post("query.do", {"sql": f"@maxrows {MAX_ROWS}"})

    def _open_session(self):
        try:
            with urllib.request.urlopen(self.console_url + "/", timeout=10) as response:
                page = response.read().decode("utf-8", errors="replace")
        except urllib.error.HTTPError as e:
            fail(f"{self.console_url} returned HTTP {e.code}. Is the H2 console enabled "
                 "(spring.h2.console.enabled) and is this the right application?")
        except (urllib.error.URLError, OSError) as e:
            fail(f"could not reach {self.console_url} ({getattr(e, 'reason', e)}). "
                 "Start the application with 'mvn spring-boot:run', or pass --base-url.")
        match = re.search(r"jsessionid=([0-9a-fA-F]+)", page)
        if not match:
            fail(f"{self.console_url} did not look like an H2 console (no session id found).")
        return match.group(1)

    def _post(self, action, fields):
        url = f"{self.console_url}/{action}?jsessionid={self.session_id}"
        data = urllib.parse.urlencode(fields).encode("utf-8")
        try:
            with urllib.request.urlopen(url, data=data, timeout=30) as response:
                return response.read().decode("utf-8", errors="replace")
        except (urllib.error.URLError, OSError) as e:
            fail(f"request to {self.console_url}/{action} failed ({getattr(e, 'reason', e)}).")

    def query(self, sql):
        """Runs one SELECT and returns its rows as a list of dicts keyed by lower-case column name."""
        page = self._post("query.do", {"sql": " ".join(sql.split())})
        parser = ResultSetParser()
        parser.feed(page)
        if not parser.found_table:
            # Not logged in, wrong JDBC URL, or a SQL error: the console answers with a message, not a table
            fail("the H2 console did not return a result. " + console_message(page, '<div id="output">'))
        return [dict(zip(parser.columns, row)) for row in parser.rows]

    def close(self):
        try:
            self._post("logout.do", {})
        except SystemExit:
            pass


def console_message(page, marker):
    """The readable text the H2 console put after the given marker, for an error message."""
    text = html.unescape(re.sub(r"<[^>]+>", " ", page.split(marker, 1)[-1]))
    return "It said: " + " ".join(text.split())[:300]


def fail(message):
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def load_orders(console):
    """Returns every order, each with the list of work orders created for it."""
    orders = console.query(ORDERS_SQL)
    by_request_id = {}
    for order in orders:
        order["work_orders"] = []
        by_request_id[order["id"]] = order

    work_orders = {}
    for row in console.query(WORK_ORDERS_SQL):
        work_order = work_orders.get(row["id"])
        if work_order is None:
            work_order = {
                "number": row["work_order_number"],
                "dispatch_group": row["dispatch_group"],
                "sequence_no": row["sequence_no"],
                "created_at": row["created_at"],
                "specs": [],
            }
            work_orders[row["id"]] = work_order
            if row["request_id"] in by_request_id:
                by_request_id[row["request_id"]]["work_orders"].append(work_order)
        if row["work_spec_code"] is not None:
            work_order["specs"].append({
                "code": row["work_spec_code"],
                "minutes": int(row["duration_minutes"] or 0),
            })
    return orders


def cell(value):
    """Makes a value safe to put in one Markdown table cell."""
    if value is None or value == "":
        return "-"
    return str(value).replace("|", "\\|").replace("\r", "").replace("\n", "; ")


def timestamp(value):
    """2026-10-03 18:55:01.123456 -> 2026-10-03 18:55:01"""
    return value.split(".")[0] if value else value


def counts_table(orders):
    lines = [
        "## Counts by status",
        "",
        "| Status | Orders | Work orders |",
        "|---|---|---|",
    ]
    # Known statuses first, in lifecycle order, then anything unexpected found in the data
    statuses = STATUSES + sorted({o["status"] for o in orders} - set(STATUSES))
    for status in statuses:
        matching = [o for o in orders if o["status"] == status]
        lines.append(f"| {status} | {len(matching)} | {sum(len(o['work_orders']) for o in matching)} |")
    lines.append(f"| **Total** | **{len(orders)}** | **{sum(len(o['work_orders']) for o in orders)}** |")
    return "\n".join(lines)


def work_orders_table(orders):
    lines = ["## Work order details", ""]
    rows = [(order, work_order) for order in orders for work_order in order["work_orders"]]
    if not rows:
        lines.append("_No work orders found._")
        return "\n".join(lines)

    lines += [
        "| Order | Order status | Customer | Work order | Dispatch group | Seq | Work specs | Total min | Created |",
        "|---|---|---|---|---|---|---|---|---|",
    ]
    for order, work_order in rows:
        specs = ", ".join(spec["code"] for spec in work_order["specs"])
        minutes = sum(spec["minutes"] for spec in work_order["specs"])
        lines.append(
            f"| {cell(order['order_id'])} | {cell(order['status'])} | {cell(order['customer_id'])} | "
            f"{cell(work_order['number'])} | {cell(work_order['dispatch_group'])} | {cell(work_order['sequence_no'])} | "
            f"{cell(specs)} | {minutes} | {cell(timestamp(work_order['created_at']))} |"
        )
    return "\n".join(lines)


def no_work_orders_table(orders):
    lines = ["## Orders with no work orders", ""]
    rows = [order for order in orders if not order["work_orders"]]
    if not rows:
        lines.append("_Every order has at least one work order._")
        return "\n".join(lines)

    lines += [
        "| Order | Status | Customer | Request type | Reason | Received |",
        "|---|---|---|---|---|---|",
    ]
    for order in rows:
        reason = order["status_detail"]
        if not reason and order["status"] == "COMPLETED":
            reason = "Completed, but no work was needed for the requested services at this address"
        lines.append(
            f"| {cell(order['order_id'])} | {cell(order['status'])} | {cell(order['customer_id'])} | "
            f"{cell(order['request_type'])} | {cell(reason)} | {cell(timestamp(order['received_at']))} |"
        )
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "--base-url",
        default="http://localhost:8080",
        help="Where the application is running (default: http://localhost:8080)",
    )
    parser.add_argument(
        "--status",
        choices=STATUSES,
        help="Only show details for orders with this status (the counts always cover every order)",
    )
    parser.add_argument("--jdbc-url", default="jdbc:h2:mem:provisioning", help="JDBC URL of the H2 database")
    parser.add_argument("--user", default="sa", help="Database user (default: sa)")
    parser.add_argument("--password", default="", help="Database password (default: empty)")
    args = parser.parse_args()

    console = H2Console(args.base_url, args.jdbc_url, args.user, args.password)
    try:
        orders = load_orders(console)
    finally:
        console.close()

    shown = [o for o in orders if o["status"] == args.status] if args.status else orders

    print(counts_table(orders))
    print()
    print(work_orders_table(shown))
    print()
    print(no_work_orders_table(shown))

    total_work_orders = sum(len(o["work_orders"]) for o in orders)
    print(
        f"\n_{len(orders)} order(s) and {total_work_orders} work order(s) currently saved at {args.base_url}._",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
