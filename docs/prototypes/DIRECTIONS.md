# Product design directions V02

Open `design-directions.html` using `node docs/prototypes/preview-server.cjs` at http://127.0.0.1:4179/.

- A / Copilot: balanced horizontal photo cards, blue primary action, 58/42 itinerary-map split.
- B / Explore: teal accent, larger photo grid, pill day selector, consumer travel discovery.
- C / Control: compact rows, navigation rail, higher information density, agent workspace.

All use Pretendard, a compact header, a real Google Maps iframe, mobile itinerary/map layout switching, and prominent unconnected AI edit actions. A/B/C selection is presentation-only. Day selection, AI editing, sharing, itinerary saving, Places verification, and Routes calculation are NOT implemented.

Travel data, arrival/dwell/transit times, distances, and verification statuses are fixtures. The Google map is real but itinerary markers/routes are not connected. No application credentials, API keys, production endpoints, or databases are used. External map, font, and photo hosts require network access.

Photo authors, source links, CC BY-SA licenses, and crop notice are included in the page footer. Photos are illustrative historical photos of the named places, not fetched through Places and not current verification evidence.

V01 `itinerary-preview.html` is preserved. Do not copy fixture status labels into production as actual verification results.