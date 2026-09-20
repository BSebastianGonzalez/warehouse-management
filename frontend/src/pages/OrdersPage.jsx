import { useEffect, useState } from "react";
import { api } from "../api/client";
import { EmptyState, ErrorState, LoadingState, Notice } from "../components/Feedback";
import { FormField } from "../components/FormField";

function OrderResult({ order, products }) {
  const productName = (id) => products.find((product) => product.id === id)?.name || `Producto #${id}`;
  const dispatched = order.status === "DISPATCHED";
  return <div className={`order-result ${dispatched ? "result-success" : "result-warning"}`}><div className="result-header"><div><span className="eyebrow">Resultado del pedido</span><h3>Pedido #{order.id}</h3></div><span className="badge">{dispatched ? "Despachado" : "Cancelado"}</span></div><p>{dispatched ? "Todas las líneas fueron atendidas con el inventario disponible." : "El pedido no se aplicó porque una o más líneas no pudieron completarse."}</p><div className="order-lines">{order.lines.map((line) => <div className="order-line-result" key={line.id}><div><strong>{productName(line.productId)}</strong><small>Solicitado: {line.requestedQuantity}</small></div>{dispatched ? <div className="dispatch-list">{line.dispatchDetails.map((detail) => <span key={detail.warehouseId}>Bodega #{detail.warehouseId}: {detail.dispatchedQuantity}</span>)}</div> : <span className="quantity-low">Faltan {line.missingQuantity}</span>}</div>)}</div></div>;
}

export function OrdersPage() {
  const [products, setProducts] = useState([]);
  const [lines, setLines] = useState([{ productId: "", requestedQuantity: 1 }]);
  const [result, setResult] = useState(null);
  const [state, setState] = useState({ loading: true, error: "" });
  useEffect(() => { api.products.list().then(setProducts).then(() => setState({ loading: false, error: "" })).catch((error) => setState({ loading: false, error: error.message })); }, []);
  const updateLine = (index, field, value) => setLines((current) => current.map((line, lineIndex) => lineIndex === index ? { ...line, [field]: value } : line));
  const submit = async (event) => {
    event.preventDefault();
    setState({ loading: false, error: "" });
    try {
      const order = await api.orders.create({ lines: lines.map((line) => ({ productId: Number(line.productId), requestedQuantity: Number(line.requestedQuantity) })) });
      setResult(order);
    } catch (error) { setState({ loading: false, error: error.message }); }
  };
  if (state.loading) return <LoadingState label="Cargando productos..." />;
  if (state.error && !products.length) return <ErrorState message={state.error} />;
  return <section className="page-section"><div className="section-heading"><div><p className="eyebrow">Operación</p><h2>Pedidos</h2><p className="muted">Registra una solicitud y conoce el resultado inmediatamente.</p></div></div>
    <div className="order-layout"><div className="panel order-form"><h3>Nuevo pedido</h3><form onSubmit={submit}>{lines.map((line, index) => <div className="order-line" key={index}><FormField label={`Producto ${index + 1}`}><select required value={line.productId} onChange={(event) => updateLine(index, "productId", event.target.value)}><option value="">Selecciona un producto</option>{products.map((product) => <option key={product.id} value={product.id}>{product.sku} · {product.name}</option>)}</select></FormField><FormField label="Cantidad"><input required min="1" type="number" value={line.requestedQuantity} onChange={(event) => updateLine(index, "requestedQuantity", event.target.value)} /></FormField>{lines.length > 1 && <button type="button" className="button button-small button-danger" onClick={() => setLines((current) => current.filter((_, lineIndex) => lineIndex !== index))}>Quitar</button>}</div>)}<div className="form-actions"><button type="button" className="button button-ghost" onClick={() => setLines((current) => [...current, { productId: "", requestedQuantity: 1 }])}>+ Agregar línea</button><button className="button button-primary">Procesar pedido</button></div>{state.error && <Notice tone="error">{state.error}</Notice>}</form></div>
      <div className="order-result-wrap">{result ? <OrderResult order={result} products={products} /> : <div className="panel empty-panel"><span className="empty-icon">☷</span><strong>El resultado aparecerá aquí</strong><span className="muted">Añade una o más líneas para procesar un pedido.</span></div>}</div>
    </div>
  </section>;
}
