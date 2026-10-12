# Reporting Automation API — Spring Boot

An independent Java demonstration of reporting automation. It uses **synthetic transactions**, not code or data from any employer or client.

**Stack:** Java 21 · Spring Boot 3.4 · Spring JDBC · H2 · Jakarta Validation · JUnit 5

## Endpoints

- `POST /api/transactions` — create a synthetic transaction
- `GET /api/reports/summary?from=2026-10-01&to=2026-10-31` — grouped category totals
- `GET /api/reports/export?from=2026-10-01&to=2026-10-31` — downloadable CSV

## Run and test

```bash
mvn spring-boot:run
mvn test
curl -X POST http://localhost:8082/api/transactions -H 'Content-Type: application/json' -d '{"category":"Supplies","description":"Notebook","amount":24.50,"occurredOn":"2026-10-12"}'
curl 'http://localhost:8082/api/reports/summary?from=2026-10-01&to=2026-10-31'
curl -OJ 'http://localhost:8082/api/reports/export?from=2026-10-01&to=2026-10-31'
```

Data is kept in memory and resets on restart. The demo does not implement authentication, scheduled jobs, email delivery or streaming of very large exports.
