/*
 * The "Find address" button on the New order page.
 *
 * The operator types what they know into the four address fields and presses
 * the button. The matches from AddressLookup (address-lookup.js) are listed,
 * and choosing one replaces all four fields. A field the match has no value
 * for is cleared.
 *
 * Nothing is sent while the operator types: a lookup starts only on a press
 * of the button. The button never submits the order.
 *
 * The lookup controls are hidden in the page markup and revealed here, so
 * with JavaScript turned off the form looks and works as it did before.
 */
(function () {
    "use strict";

    var container = document.getElementById("address-lookup");
    if (!container || !window.AddressLookup) {
        return;
    }

    var FIELD_NAMES = ["street", "city", "state", "zip"];
    var fields = {};
    FIELD_NAMES.forEach(function (name) { fields[name] = document.getElementById(name); });

    var findButton = document.getElementById("find-address");
    var status = document.getElementById("address-lookup-status");
    var matchesPanel = document.getElementById("address-matches");
    var matchList = document.getElementById("address-match-list");
    var closeButton = document.getElementById("close-address-matches");

    var busy = false;

    function setStatus(message) {
        status.textContent = message;
    }

    function setBusy(value) {
        busy = value;
        // aria-disabled rather than disabled, so the button keeps keyboard focus during the lookup
        findButton.setAttribute("aria-disabled", value ? "true" : "false");
    }

    function typedAddress() {
        var address = {};
        FIELD_NAMES.forEach(function (name) { address[name] = fields[name].value; });
        return address;
    }

    function matchButtons() {
        return Array.prototype.slice.call(matchList.querySelectorAll("button"));
    }

    function closeMatches() {
        matchesPanel.hidden = true;
        while (matchList.firstChild) {
            matchList.removeChild(matchList.firstChild);
        }
    }

    function choose(match) {
        FIELD_NAMES.forEach(function (name) { fields[name].value = match[name]; });
        closeMatches();
        setStatus("Address filled in. Check it, and complete any empty field, before submitting.");

        var firstEmpty = FIELD_NAMES.filter(function (name) { return fields[name].value === ""; })[0];
        fields[firstEmpty || "street"].focus();
    }

    function showMatches(matches) {
        closeMatches();
        matches.forEach(function (match) {
            var item = document.createElement("li");
            var button = document.createElement("button");
            button.type = "button";
            button.className = "address-match";
            // textContent, never innerHTML: the label comes from an external service
            button.textContent = match.label;
            button.addEventListener("click", function () { choose(match); });
            item.appendChild(button);
            matchList.appendChild(item);
        });
        matchesPanel.hidden = false;
        setStatus(matches.length === 1
            ? "1 address found. Choose it, or close the list to keep what you typed."
            : matches.length + " addresses found. Choose one, or close the list to keep what you typed.");
        matchButtons()[0].focus();
    }

    function showResult(result) {
        if (result.status === "empty") {
            setStatus("Type an address first, then press Find address.");
            fields.street.focus();
        } else if (result.status === "unavailable") {
            setStatus("Address lookup is not available right now. Please type the address by hand.");
        } else if (result.matches.length === 0) {
            setStatus("No address was found. Check what you typed, or enter the address by hand.");
        } else {
            showMatches(result.matches);
        }
    }

    findButton.addEventListener("click", function () {
        if (busy) {
            return;
        }
        closeMatches();
        setBusy(true);
        setStatus("Looking up the address...");
        window.AddressLookup.find(typedAddress()).then(function (result) {
            setBusy(false);
            showResult(result);
        });
    });

    closeButton.addEventListener("click", function () {
        closeMatches();
        setStatus("");
        findButton.focus();
    });

    matchesPanel.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            event.preventDefault();
            closeMatches();
            setStatus("");
            findButton.focus();
            return;
        }

        var buttons = matchButtons();
        var current = buttons.indexOf(document.activeElement);
        var next = -1;
        if (event.key === "ArrowDown") {
            next = current < buttons.length - 1 ? current + 1 : 0;
        } else if (event.key === "ArrowUp") {
            next = current > 0 ? current - 1 : buttons.length - 1;
        } else if (event.key === "Home") {
            next = 0;
        } else if (event.key === "End") {
            next = buttons.length - 1;
        }
        if (next !== -1 && buttons.length > 0) {
            event.preventDefault();
            buttons[next].focus();
        }
    });

    container.hidden = false;
})();
