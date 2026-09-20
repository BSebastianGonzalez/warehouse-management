import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const API = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

async function request(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message || "Request failed");
  return body;
}

function Login({ onLogin }) {
  const [registering, setRegistering] = useState(false);
  const [form, setForm] = useState({ username: "", password: "", name: "" });
  const [error, setError] = useState("");
  const submit = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const path = registering ? "/auth/register" : "/auth/login";
      onLogin(await request(path, { method: "POST", body: JSON.stringify(form) }));
    }
    catch (e) { setError(e.message); }
  };
  return <main className="auth-card">
    <h1>Warehouse Management</h1>
    <p>{registering ? "Create the initial administrator." : "Sign in to operate inventory and orders."}</p>
    <form onSubmit={submit}>
      {registering && <label>Display name<input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>}
      <label>Username<input required value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} /></label>
      <label>Password<input required type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} /></label>
      {error && <div className="error">{error}</div>}
      <button>{registering ? "Create administrator" : "Sign in"}</button>
    </form>
    <button className="link-button" onClick={() => { setRegistering(!registering); setError(""); }}>{registering ? "Back to sign in" : "Create initial administrator"}</button>
  </main>;
}

function MovementForm({ products, warehouses, onSaved }) {
  const [type, setType] = useState("inbound");
  const [form, setForm] = useState({ productId: "", warehouseId: "", sourceWarehouseId: "", destinationWarehouseId: "", quantity: "", reference: "" });
  const [message, setMessage] = useState("");
  const submit = async (event) => {
    event.preventDefault();
    setMessage("");
    const body = { productId: Number(form.productId), quantity: Number(form.quantity), reference: form.reference || null };
    if (type === "transfers") Object.assign(body, { sourceWarehouseId: Number(form.sourceWarehouseId), destinationWarehouseId: Number(form.destinationWarehouseId) });
    else Object.assign(body, { warehouseId: Number(form.warehouseId) });
    try { await request(`/movements/${type}`, { method: "POST", body: JSON.stringify(body) }); setMessage("Movement recorded."); onSaved(); }
    catch (e) { setMessage(e.message); }
  };
  return <section className="panel"><h2>Record movement</h2><form className="grid-form" onSubmit={submit}>
    <label>Type<select value={type} onChange={e => setType(e.target.value)}><option value="inbound">Inbound</option><option value="outbound">Outbound</option><option value="transfers">Transfer</option></select></label>
    <label>Product<select required value={form.productId} onChange={e => setForm({ ...form, productId: e.target.value })}><option value="">Select product</option>{products.map(p => <option key={p.id} value={p.id}>{p.sku} - {p.name}</option>)}</select></label>
    {type === "transfers" ? <><label>Source warehouse<select required value={form.sourceWarehouseId} onChange={e => setForm({ ...form, sourceWarehouseId: e.target.value })}><option value="">Select warehouse</option>{warehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}</select></label><label>Destination warehouse<select required value={form.destinationWarehouseId} onChange={e => setForm({ ...form, destinationWarehouseId: e.target.value })}><option value="">Select warehouse</option>{warehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}</select></label></> : <label>Warehouse<select required value={form.warehouseId} onChange={e => setForm({ ...form, warehouseId: e.target.value })}><option value="">Select warehouse</option>{warehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}</select></label>}
    <label>Quantity<input required min="1" type="number" value={form.quantity} onChange={e => setForm({ ...form, quantity: e.target.value })} /></label>
    <label>Reference<input value={form.reference} onChange={e => setForm({ ...form, reference: e.target.value })} /></label>
    <button>Save movement</button>{message && <span className="notice">{message}</span>}
  </form></section>;
}

function CatalogForm({ onSaved }) {
  const [product, setProduct] = useState({ sku: "", name: "", unitOfMeasure: "", minimumStock: 10 });
  const [warehouse, setWarehouse] = useState({ name: "", location: "" });
  const [message, setMessage] = useState("");
  const submit = async (path, body, reset) => {
    try { await request(path, { method: "POST", body: JSON.stringify(body) }); setMessage("Catalog item created."); reset(); onSaved(); }
    catch (e) { setMessage(e.message); }
  };
  return <section className="panel"><h2>Catalog</h2><div className="two-columns">
    <form onSubmit={e => { e.preventDefault(); submit("/products", { ...product, minimumStock: Number(product.minimumStock) }, () => setProduct({ sku: "", name: "", unitOfMeasure: "", minimumStock: 10 })); }}>
      <strong>New product</strong><label>SKU<input required value={product.sku} onChange={e => setProduct({ ...product, sku: e.target.value })} /></label><label>Name<input required value={product.name} onChange={e => setProduct({ ...product, name: e.target.value })} /></label><label>Unit<input required value={product.unitOfMeasure} onChange={e => setProduct({ ...product, unitOfMeasure: e.target.value })} /></label><label>Minimum stock<input required min="0" type="number" value={product.minimumStock} onChange={e => setProduct({ ...product, minimumStock: e.target.value })} /></label><button>Create product</button>
    </form>
    <form onSubmit={e => { e.preventDefault(); submit("/warehouses", warehouse, () => setWarehouse({ name: "", location: "" })); }}>
      <strong>New warehouse</strong><label>Name<input required value={warehouse.name} onChange={e => setWarehouse({ ...warehouse, name: e.target.value })} /></label><label>Location<input required value={warehouse.location} onChange={e => setWarehouse({ ...warehouse, location: e.target.value })} /></label><button>Create warehouse</button>
    </form>
  </div>{message && <span className="notice">{message}</span>}</section>;
}

function OrderForm({ products, onSaved }) {
  const [lines, setLines] = useState([{ productId: "", requestedQuantity: 1 }]);
  const [message, setMessage] = useState("");
  const submit = async (event) => {
    event.preventDefault();
    try {
      const result = await request("/orders", { method: "POST", body: JSON.stringify({ lines: lines.map(l => ({ productId: Number(l.productId), requestedQuantity: Number(l.requestedQuantity) })) }) });
      setMessage(`Order ${result.id} ${result.status.toLowerCase()}.`);
      onSaved();
    } catch (e) { setMessage(e.message); }
  };
  const update = (index, field, value) => setLines(lines.map((line, i) => i === index ? { ...line, [field]: value } : line));
  return <section className="panel"><h2>Create order</h2><form onSubmit={submit}>{lines.map((line, index) => <div className="line" key={index}><label>Product<select required value={line.productId} onChange={e => update(index, "productId", e.target.value)}><option value="">Select product</option>{products.map(p => <option key={p.id} value={p.id}>{p.sku} - {p.name}</option>)}</select></label><label>Quantity<input required min="1" type="number" value={line.requestedQuantity} onChange={e => update(index, "requestedQuantity", e.target.value)} /></label>{lines.length > 1 && <button type="button" className="danger" onClick={() => setLines(lines.filter((_, i) => i !== index))}>Remove</button>}</div>)}<div><button type="button" className="secondary dark" onClick={() => setLines([...lines, { productId: "", requestedQuantity: 1 }])}>Add line</button> <button>Create order</button></div>{message && <span className="notice">{message}</span>}</form></section>;
}

function App() {
  const [admin, setAdmin] = useState(null);
  const [products, setProducts] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [stocks, setStocks] = useState([]);
  const [error, setError] = useState("");
  const load = async () => {
    try { const [p, w, s] = await Promise.all([request("/products"), request("/warehouses"), request("/stocks")]); setProducts(p); setWarehouses(w); setStocks(s); }
    catch (e) { setError(e.message); }
  };
  useEffect(() => { if (admin) load(); }, [admin]);
  if (!admin) return <Login onLogin={setAdmin} />;
  const logout = async () => { await request("/auth/logout", { method: "POST" }); setAdmin(null); };
  return <div className="app"><header><div><strong>Warehouse Management</strong><span>{admin.name}</span></div><button className="secondary" onClick={logout}>Sign out</button></header>
    <main><h1>Inventory dashboard</h1>{error && <div className="error">{error}</div>}
      <section className="panel"><h2>Stock by product and warehouse</h2><div className="table-wrap"><table><thead><tr><th>Product</th><th>Minimum</th><th>Total</th><th>Status</th><th>Warehouses</th></tr></thead><tbody>{stocks.map(s => <tr key={s.productId}><td>{s.sku}<br /><small>{s.productName}</small></td><td>{s.minimumStock}</td><td>{s.totalQuantity}</td><td><span className={s.belowMinimum ? "badge warning" : "badge"}>{s.belowMinimum ? "Below minimum" : "OK"}</span></td><td>{s.warehouses.map(w => <span className="warehouse" key={w.warehouseId}>{w.warehouseName}: {w.quantity}</span>)}</td></tr>)}</tbody></table></div></section>
      <MovementForm products={products} warehouses={warehouses} onSaved={load} />
      <CatalogForm onSaved={load} />
      <OrderForm products={products} onSaved={load} />
    </main></div>;
}

createRoot(document.getElementById("root")).render(<App />);
