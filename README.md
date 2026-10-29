# VenueVista — EventPro Ticketing & Venue Management
*A product of **Infotact Solutions**, Electronic City, Bengaluru, Karnataka 560100 · support@infotact.in · +91 91249 36538*

Event ticketing and venue management with live seating charts, ticket sales, e-tickets (QR), gate check-in and sales analytics.

**Stack:** Java 21 · Spring Boot 3.3 (Web, Thymeleaf, Data JPA) · H2 (file DB) · ZXing (QR) · vanilla JS/CSS (no front-end build step)

## Run
```bash
mvn spring-boot:run          # or: mvn package && java -jar target/venuevista.jar
```
Open http://localhost:8080 — demo data (3 venues, 10 events, ~1,500 tickets of sales history) is seeded on first start.
Organizer console: http://localhost:8080/admin — `admin` / `venuevista@2026` (change in `application.properties`).
Reset data: stop the app and delete the `./data` folder.

## Features
- **Public:** event discovery (search + category filters), interactive tiered seating chart, "find best seats" for a group, live seat-taken refresh, demo checkout, QR e-tickets, ticket lookup by reference + email.
- **Organizer console:** sales analytics (revenue by day/tier/category, occupancy per event, live order feed), event CRUD with publish/cancel, venue + section/seat-layout builder, order search, gate check-in with duplicate-scan detection.
- **Integrity:** DB unique constraint on (event, section, row, seat) makes double-booking impossible even under concurrent purchases.

## Layout
`model/` entities · `repo/` Spring Data repositories · `service/` booking, analytics, catalog, QR, seed data · `web/` controllers + admin auth interceptor · `templates/` Thymeleaf · `static/` CSS/JS

## Notes
Checkout is a demo (no payment gateway). Admin auth is a simple session login suitable for a demo — put Spring Security / SSO in front before production.
