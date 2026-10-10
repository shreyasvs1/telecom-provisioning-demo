---
name: provisioning-work-orders-summarizer
description: When asked to summarize, count or report on the orders and work orders currently saved by the running telecom-provisioning-demo application (for example "how many orders were rejected?" or "list the work orders"), run scripts/summarize_work_orders.py and use its output rather than querying the database or reading the code yourself.
---

Work orders are not in `data.sql`. They exist only in the running application's in-memory H2 database, so the application must be running before the script is used.

## Run it

From the project root:

```bash
python3 .claude/skills/provisioning-work-orders-summarizer/scripts/summarize_work_orders.py
```

Options:

- `--base-url http://localhost:8081` if the application is not on port 8080.
- `--status COMPLETED` (or `RECEIVED`, `REJECTED`, `FAILED`) to limit the detail sections to one status. The counts always cover every order.

## What it prints

Markdown on standard output, in three sections:

1. **Counts by status**: orders and work orders per status, with a total.
2. **Work order details**: one row per work order, with its order, dispatch group, sequence, work specs and total minutes.
3. **Orders with no work orders**: rejected and failed orders with the reason, and completed orders that needed no work.

A one-line total goes to standard error.

## Using the output

- Report the script's figures as they are. Do not recount or re-derive them.
- Work orders have no status of their own. They are grouped by the status of the order they belong to.
- The data is live and in memory. It is cleared when the application restarts, so an empty result usually means nothing has been submitted since the last start.
- The script only runs `SELECT` statements through the application's H2 console at `/h2-console`. It never changes data.

## If it fails

- **"could not reach ..."**: the application is not running at that address. Ask the user to start it with `mvn spring-boot:run`, or pass `--base-url`. Do not start or restart the application without asking, because a restart wipes the saved orders.
- **"could not log in to the database"**: the JDBC URL, user or password differs from the defaults in `application.yml`. Pass `--jdbc-url`, `--user` or `--password`.
