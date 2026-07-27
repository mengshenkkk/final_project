# Payments Processing System (Training Project)

Minimal Spring Boot REST API for payment lifecycle processing:

`CREATED -> VALIDATED -> SENT -> COMPLETED`

`FAILED` can be reached from `CREATED`, `VALIDATED`, or `SENT`.

## Tech Stack

- Java 22
- Spring Boot 3
- Spring JDBC (`NamedParameterJdbcTemplate`)
- H2 in-memory database

## Run

```powershell
mvn spring-boot:run
```

API base URL: `http://localhost:8080/api/payments`

## Frontend (React)

A separate React app is available in `frontend-react`.

```powershell
Set-Location .\frontend-react
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

## API Endpoints

- `POST /api/payments` - create payment (idempotent by `idempotencyKey`)
- `GET /api/payments/{id}` - get payment details
- `GET /api/payments?status=CREATED|VALIDATED|SENT|COMPLETED|FAILED` - list/filter
- `GET /api/payments/{id}/history` - status transition audit trail
- `POST /api/payments/{id}/validate`
- `POST /api/payments/{id}/send`
- `POST /api/payments/{id}/complete`
- `POST /api/payments/{id}/fail`

## Example Requests

Create payment:

```powershell
$body = @'
{
  "idempotencyKey": "client-req-001",
  "sourceAccount": "12345678",
  "destinationAccount": "87654321",
  "amount": 120.50,
  "currency": "USD",
  "reference": "invoice-2026-07"
}
'@
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments" -ContentType "application/json" -Body $body
```

Progress status:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments/{paymentId}/validate"
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments/{paymentId}/send"
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments/{paymentId}/complete"
```

Mark as failed:

```powershell
$failBody = @'
{
  "errorCode": "NETWORK_ERROR",
  "errorMessage": "Destination network timeout"
}
'@
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments/{paymentId}/fail" -ContentType "application/json" -Body $failBody
```

## Notes

- Idempotency behavior: reusing the same `idempotencyKey` returns the existing payment (HTTP 200).
- Invalid transitions return `INVALID_STATUS_TRANSITION`.
- Error responses include `errorCode`, `message`, and `timestamp`.


