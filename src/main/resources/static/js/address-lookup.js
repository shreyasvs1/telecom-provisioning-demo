/*
 * Address lookup against the public Nominatim service (OpenStreetMap).
 *
 * This is the application's only real external integration, and it runs in
 * the operator's browser: the server never calls Nominatim. It is used by the
 * "Find address" button on the New order page (see address-form.js).
 *
 * Nominatim's usage policy (https://operations.osmfoundation.org/policies/nominatim/)
 * shapes what this file does:
 *   - No auto-complete: find() is only ever called when the operator asks.
 *   - At most 1 request per second: requests are spaced out below.
 *   - The request must identify the application: the browser's Referer header
 *     does that, so nothing here may suppress it (no referrerPolicy override).
 *
 * Usage:
 *   AddressLookup.find({ street, city, state, zip }).then(function (result) { ... });
 *
 * find() never rejects. It resolves to one of:
 *   { status: "empty" }                 nothing was typed, no request was sent
 *   { status: "ok", matches: [...] }    matches may be an empty list
 *   { status: "unavailable" }           Nominatim could not be reached or did not answer in time
 *
 * Each match is { label, street, city, state, zip }. A value Nominatim did not
 * supply is an empty string, so the form can clear that field.
 */
(function (root) {
    "use strict";

    var SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    var COUNTRY_CODES = ["us", "ca"];
    var MAX_MATCHES = 5;
    var TIMEOUT_MS = 8000;
    var MIN_INTERVAL_MS = 1000;

    // Fallback for the rare result that has no ISO3166-2-lvl4 code, only a name
    var STATE_CODES = {
        "alabama": "AL", "alaska": "AK", "arizona": "AZ", "arkansas": "AR", "california": "CA",
        "colorado": "CO", "connecticut": "CT", "delaware": "DE", "district of columbia": "DC",
        "florida": "FL", "georgia": "GA", "hawaii": "HI", "idaho": "ID", "illinois": "IL",
        "indiana": "IN", "iowa": "IA", "kansas": "KS", "kentucky": "KY", "louisiana": "LA",
        "maine": "ME", "maryland": "MD", "massachusetts": "MA", "michigan": "MI", "minnesota": "MN",
        "mississippi": "MS", "missouri": "MO", "montana": "MT", "nebraska": "NE", "nevada": "NV",
        "new hampshire": "NH", "new jersey": "NJ", "new mexico": "NM", "new york": "NY",
        "north carolina": "NC", "north dakota": "ND", "ohio": "OH", "oklahoma": "OK", "oregon": "OR",
        "pennsylvania": "PA", "rhode island": "RI", "south carolina": "SC", "south dakota": "SD",
        "tennessee": "TN", "texas": "TX", "utah": "UT", "vermont": "VT", "virginia": "VA",
        "washington": "WA", "west virginia": "WV", "wisconsin": "WI", "wyoming": "WY",
        "alberta": "AB", "british columbia": "BC", "manitoba": "MB", "new brunswick": "NB",
        "newfoundland and labrador": "NL", "northwest territories": "NT", "nova scotia": "NS",
        "nunavut": "NU", "ontario": "ON", "prince edward island": "PE", "quebec": "QC",
        "québec": "QC", "saskatchewan": "SK", "yukon": "YT"
    };

    var lastRequestAt = 0;

    function trimmed(value) {
        return (value === null || value === undefined) ? "" : String(value).trim();
    }

    // Everything typed across the four address fields, as one search text
    function searchText(address) {
        return [address.street, address.city, address.state, address.zip]
            .map(trimmed)
            .filter(function (part) { return part !== ""; })
            .join(", ");
    }

    function stateCode(details) {
        // "US-IL" -> "IL", "CA-ON" -> "ON"
        var iso = trimmed(details["ISO3166-2-lvl4"]);
        if (iso.indexOf("-") !== -1) {
            return iso.split("-").pop().toUpperCase();
        }
        return STATE_CODES[trimmed(details.state || details.province).toLowerCase()] || "";
    }

    function toMatch(result) {
        var details = result.address || {};
        var road = trimmed(details.road || details.pedestrian || details.residential);
        var houseNumber = trimmed(details.house_number);
        return {
            label: trimmed(result.display_name),
            street: road === "" ? "" : (houseNumber + " " + road).trim(),
            city: trimmed(details.city || details.town || details.village || details.hamlet || details.municipality),
            state: stateCode(details),
            zip: trimmed(details.postcode)
        };
    }

    function inCoveredCountry(result) {
        var code = trimmed((result.address || {}).country_code).toLowerCase();
        return COUNTRY_CODES.indexOf(code) !== -1;
    }

    function wait(ms) {
        return new Promise(function (resolve) { setTimeout(resolve, ms); });
    }

    function request(text) {
        var url = SEARCH_URL +
            "?format=jsonv2&addressdetails=1" +
            "&countrycodes=" + COUNTRY_CODES.join(",") +
            "&limit=" + MAX_MATCHES +
            "&q=" + encodeURIComponent(text);

        var controller = new AbortController();
        var timer = setTimeout(function () { controller.abort(); }, TIMEOUT_MS);
        lastRequestAt = Date.now();

        return fetch(url, { signal: controller.signal, headers: { "Accept": "application/json" } })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Nominatim answered HTTP " + response.status);
                }
                return response.json();
            })
            .then(function (results) {
                if (!Array.isArray(results)) {
                    throw new Error("Unexpected answer from Nominatim");
                }
                return { status: "ok", matches: results.filter(inCoveredCountry).map(toMatch) };
            })
            .catch(function () {
                // Network error, timeout, bad status or unreadable answer: all mean "enter it by hand"
                return { status: "unavailable" };
            })
            .then(function (outcome) {
                clearTimeout(timer);
                return outcome;
            });
    }

    function find(address) {
        var text = searchText(address || {});
        if (text === "") {
            return Promise.resolve({ status: "empty" });
        }
        var sinceLast = Date.now() - lastRequestAt;
        var delay = sinceLast >= MIN_INTERVAL_MS ? 0 : MIN_INTERVAL_MS - sinceLast;
        return wait(delay).then(function () { return request(text); });
    }

    root.AddressLookup = { find: find };
})(typeof window !== "undefined" ? window : globalThis);
