# Quick Start Guide

## 🚀 Start Everything in 2 Steps

### Step 1: Start Backend (Terminal 1)
```bash
cd C:\learn_java\spring-jdbc-course-main\labs\final_project
mvn spring-boot:run
```
✅ Backend will start on `http://localhost:8080`

### Step 2: Start Frontend (Terminal 2)
```bash
cd C:\learn_java\spring-jdbc-course-main\labs\final_project\frontend-react
npm run dev
```
✅ Frontend will start on `http://localhost:5173`

---

## 📱 Access the Application

Open your browser to: **http://localhost:5173**

---

## ✅ Test the API (Quick Test)

Open PowerShell and run:

```powershell
# Create a payment
$payload = @{
    sourceAccount = "ACC001"
    destinationAccount = "ACC002"
    amount = "100.50"
    currency = "USD"
    reference = "Test payment"
    idempotencyKey = "test-$(Get-Random)"
} | ConvertTo-Json

$response = (Invoke-WebRequest -Uri 'http://localhost:8080/api/payments' -Method POST -Body $payload -ContentType 'application/json').Content
$response | ConvertFrom-Json | ConvertTo-Json

# List all payments
(Invoke-WebRequest -Uri 'http://localhost:8080/api/payments' -UseBasicParsing).Content | ConvertFrom-Json | ConvertTo-Json
```

---

## 📊 Database Console

While backend is running, access H2 console:
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:paymentsdb`
- **User**: `sa`
- **Password**: (blank)

---

## 🧪 Run Tests

```bash
cd C:\learn_java\spring-jdbc-course-main\labs\final_project
mvn test
```

Expected: ✅ **1/1 test passed** (BUILD SUCCESS)

---

## 📋 Payment Lifecycle States

```
CREATED → VALIDATED → SENT → COMPLETED
                  ↘
                   FAILED (can fail from any state)
```

Use the frontend UI to transition between states!

---

## 🎯 Common Operations

### Create Payment (UI)
1. Enter source account (e.g., ACC001)
2. Enter destination account (e.g., ACC002)
3. Enter amount and currency
4. Click "Create" button

### Transition Payment (UI)
1. Select payment from list
2. Click appropriate button: "Validate", "Send", "Complete", or "Fail"
3. View status changes in real-time

### View Payment History (UI)
1. Select payment from list
2. Payment history shown below with all transitions

---

## 🔧 Configuration Files

- **Backend Config**: `src/main/resources/application.yml`
- **Test Config**: `src/test/resources/application.yml`
- **Database Schema**: `src/main/resources/schema.sql`
- **Frontend API**: `frontend-react/src/api.js`

---

## ⚠️ Common Issues

| Issue | Solution |
|-------|----------|
| Port 8080 in use | Kill Java: `Get-Process java \| Stop-Process -Force` |
| Frontend won't load | Check backend is running first |
| CORS errors | Backend CORS already configured for localhost:5173 |
| Tests fail | Run `mvn clean test` |

---

**✅ Backend**: H2 In-Memory Database (runs without external setup!)  
**✅ Frontend**: React + Vite  
**✅ Status**: Ready to use!

