import { useEffect, useMemo, useState } from "react";
import {
  createPayment,
  failPayment,
  getPayment,
  getPaymentHistory,
  listPayments,
  transitionPayment
} from "./api";

const initialForm = {
  idempotencyKey: "",
  sourceAccount: "",
  destinationAccount: "",
  amount: "",
  currency: "USD",
  reference: ""
};

export default function App() {
  const [form, setForm] = useState(initialForm);
  const [payments, setPayments] = useState([]);
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [history, setHistory] = useState([]);
  const [statusFilter, setStatusFilter] = useState("");
  const [paymentIdInput, setPaymentIdInput] = useState("");
  const [message, setMessage] = useState("Ready");
  const [messageType, setMessageType] = useState("info");
  const [failForm, setFailForm] = useState({
    errorCode: "NETWORK_ERROR",
    errorMessage: "Manual failure from frontend"
  });
  const [loading, setLoading] = useState(false);

  const canTransition = useMemo(() => {
    if (!selectedPayment) {
      return {};
    }
    return {
      validate: selectedPayment.status === "CREATED",
      send: selectedPayment.status === "VALIDATED",
      complete: selectedPayment.status === "SENT",
      fail: ["CREATED", "VALIDATED", "SENT"].includes(selectedPayment.status)
    };
  }, [selectedPayment]);

  useEffect(() => {
    void loadPayments();
  }, []);

  function setFeedback(text, type = "info") {
    setMessage(text);
    setMessageType(type);
  }

  async function withLoading(action) {
    try {
      setLoading(true);
      await action();
    } catch (error) {
      setFeedback(error.message, "error");
    } finally {
      setLoading(false);
    }
  }

  function onFormChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function parseAmount(amountValue) {
    const amount = Number.parseFloat(amountValue);
    if (Number.isNaN(amount)) {
      throw new Error("Amount must be a valid number");
    }
    return amount;
  }

  async function handleCreatePayment(event) {
    event.preventDefault();

    if (form.sourceAccount === form.destinationAccount) {
      setFeedback("Source and destination accounts must be different", "error");
      return;
    }

    await withLoading(async () => {
      const payload = { ...form, amount: parseAmount(form.amount) };
      const payment = await createPayment(payload);
      setSelectedPayment(payment);
      setPaymentIdInput(payment.id);
      setHistory(await getPaymentHistory(payment.id));
      setFeedback(`Payment saved with id ${payment.id}`, "success");
      setForm((prev) => ({ ...prev, amount: "", reference: "" }));
      const data = await listPayments(statusFilter || "");
      setPayments(data);
    });
  }

  async function loadPayments(status = statusFilter) {
    await withLoading(async () => {
      const data = await listPayments(status || "");
      setPayments(data);
      setFeedback(`Loaded ${data.length} payment(s)`, "info");
    });
  }

  async function loadPaymentById() {
    if (!paymentIdInput.trim()) {
      setFeedback("Enter a payment id first", "error");
      return;
    }

    await withLoading(async () => {
      const payment = await getPayment(paymentIdInput.trim());
      setSelectedPayment(payment);
      setHistory(await getPaymentHistory(payment.id));
      setFeedback(`Loaded payment ${payment.id}`, "success");
    });
  }

  async function doTransition(action) {
    if (!selectedPayment) {
      setFeedback("Load or create a payment first", "error");
      return;
    }

    await withLoading(async () => {
      const updated = await transitionPayment(selectedPayment.id, action);
      setSelectedPayment(updated);
      setHistory(await getPaymentHistory(updated.id));
      setFeedback(`Payment moved to ${updated.status}`, "success");
      const data = await listPayments(statusFilter || "");
      setPayments(data);
    });
  }

  async function doFail() {
    if (!selectedPayment) {
      setFeedback("Load or create a payment first", "error");
      return;
    }
    if (!canTransition.fail) {
      setFeedback("Current status cannot transition to FAILED", "error");
      return;
    }

    await withLoading(async () => {
      const updated = await failPayment(selectedPayment.id, failForm);
      setSelectedPayment(updated);
      setHistory(await getPaymentHistory(updated.id));
      setFeedback(`Payment moved to ${updated.status}`, "success");
      const data = await listPayments(statusFilter || "");
      setPayments(data);
    });
  }

  return (
    <main className="app">
      <h1>Payments Frontend (React)</h1>
      <p className="hint">Backend API: http://localhost:8080/api/payments</p>

      <section className="card">
        <h2>Create Payment</h2>
        <form className="grid" onSubmit={handleCreatePayment}>
          <input name="idempotencyKey" placeholder="Idempotency Key" value={form.idempotencyKey} onChange={onFormChange} required />
          <input name="sourceAccount" placeholder="Source Account (8-20 digits)" value={form.sourceAccount} onChange={onFormChange} pattern="[0-9]{8,20}" title="Source account must be 8-20 digits" required />
          <input name="destinationAccount" placeholder="Destination Account (8-20 digits)" value={form.destinationAccount} onChange={onFormChange} pattern="[0-9]{8,20}" title="Destination account must be 8-20 digits" required />
          <input name="amount" type="number" min="0.01" max="1000000" step="0.01" placeholder="Amount" value={form.amount} onChange={onFormChange} required />
          <select name="currency" value={form.currency} onChange={onFormChange}>
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
            <option value="GBP">GBP</option>
          </select>
          <input name="reference" placeholder="Reference (optional)" value={form.reference} onChange={onFormChange} />
          <button disabled={loading} type="submit">{loading ? "Creating..." : "Create"}</button>
        </form>
      </section>

      <section className="card">
        <h2>Query Payment</h2>
        <div className="row">
          <input placeholder="Payment ID" value={paymentIdInput} onChange={(e) => setPaymentIdInput(e.target.value)} />
          <button disabled={loading} onClick={loadPaymentById}>Load by ID</button>
        </div>
      </section>

      <section className="card">
        <h2>Payment List</h2>
        <div className="row">
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All Statuses</option>
            <option value="CREATED">CREATED</option>
            <option value="VALIDATED">VALIDATED</option>
            <option value="SENT">SENT</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="FAILED">FAILED</option>
          </select>
          <button disabled={loading} onClick={() => loadPayments(statusFilter)}>Refresh List</button>
        </div>
        <div className="list">
          {payments.map((payment) => (
            <button key={payment.id} className="list-item" onClick={() => { setSelectedPayment(payment); setPaymentIdInput(payment.id); }}>
              <strong>{payment.id}</strong> | {payment.amount} {payment.currency} | {payment.status}
            </button>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>Selected Payment</h2>
        {selectedPayment ? (
          <>
            <pre>{JSON.stringify(selectedPayment, null, 2)}</pre>
            <div className="row">
              <button disabled={loading || !canTransition.validate} onClick={() => doTransition("validate")}>Validate</button>
              <button disabled={loading || !canTransition.send} onClick={() => doTransition("send")}>Send</button>
              <button disabled={loading || !canTransition.complete} onClick={() => doTransition("complete")}>Complete</button>
              <button className="danger" disabled={loading || !canTransition.fail} onClick={doFail}>Fail</button>
            </div>
            <div className="grid fail-grid">
              <input
                value={failForm.errorCode}
                onChange={(e) => setFailForm((prev) => ({ ...prev, errorCode: e.target.value }))}
                placeholder="Error Code"
              />
              <input
                value={failForm.errorMessage}
                onChange={(e) => setFailForm((prev) => ({ ...prev, errorMessage: e.target.value }))}
                placeholder="Error Message"
              />
            </div>
          </>
        ) : (
          <p>No payment selected</p>
        )}
      </section>

      <section className="card">
        <h2>Status History</h2>
        {history.length === 0 ? <p>No history</p> : <pre>{JSON.stringify(history, null, 2)}</pre>}
      </section>

      <p className={`message ${messageType}`}>{loading ? "Loading..." : message}</p>
    </main>
  );
}


