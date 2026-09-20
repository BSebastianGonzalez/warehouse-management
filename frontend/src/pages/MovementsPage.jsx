import { useEffect, useMemo, useState } from "react";
import { api } from "../api/client";
import { ErrorState, LoadingState, Notice } from "../components/Feedback";
import { FormField } from "../components/FormField";

const movementTypes = [
  { key: "inbound", label: "Entrada", description: "Agregar unidades a una bodega." },
  { key: "outbound", label: "Salida", description: "Retirar unidades de una bodega." },
  { key: "transfer", label: "Traslado", description: "Mover unidades entre bodegas." }
];

function pageContent(result) {
  return Array.isArray(result) ? { content: result, number: 0, totalPages: 1 } : { content: result?.content || [], number: result?.number || 0, totalPages: result?.totalPages || 1 };
}

function asInstant(value) {
  return value ? `${value}:00Z` : "";
}

function MovementHistory({ products, warehouses }) {
  const [filters, setFilters] = useState({ type: "", productId: "", warehouseId: "", administratorId: "", from: "", to: "", reference: "" });
  const [result, setResult] = useState({ content: [], number: 0, totalPages: 1 });
  const [state, setState] = useState({ loading: true, error: "" });
  const [page, setPage] = useState(0);
  const load = async (nextPage = page, nextFilters = filters) => {
    setState({ loading: true, error: "" });
    try { setResult(pageContent(await api.movements.list(nextFilters, nextPage))); setPage(nextPage); setState({ loading: false, error: "" }); }
    catch (error) { setState({ loading: false, error: error.message }); }
  };
  useEffect(() => { load(0); }, []);
  const update = (field, value) => setFilters((current) => ({ ...current, [field]: value }));
  const submit = (event) => { event.preventDefault(); load(0, { ...filters, from: asInstant(filters.from), to: asInstant(filters.to) }); };
  const productName = (id) => products.find((item) => item.id === id)?.name || `Producto #${id}`;
  const warehouseName = (id) => warehouses.find((item) => item.id === id)?.name || `Bodega #${id}`;
  return <div className="history-section">
    <div className="section-heading"><div><h3>Historial de movimientos</h3><p className="muted">Los movimientos más recientes aparecen primero.</p></div></div>
    <form className="panel filters-panel" onSubmit={submit}>
      <FormField label="Tipo"><select value={filters.type} onChange={(event) => update("type", event.target.value)}><option value="">Todos</option><option value="INBOUND">Entrada</option><option value="OUTBOUND">Salida</option><option value="TRANSFER">Traslado</option></select></FormField>
      <FormField label="Producto"><select value={filters.productId} onChange={(event) => update("productId", event.target.value)}><option value="">Todos</option>{products.map((product) => <option key={product.id} value={product.id}>{product.sku} · {product.name}</option>)}</select></FormField>
      <FormField label="Bodega"><select value={filters.warehouseId} onChange={(event) => update("warehouseId", event.target.value)}><option value="">Todas</option>{warehouses.map((warehouse) => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}</select></FormField>
      <FormField label="Administrador"><input value={filters.administratorId} onChange={(event) => update("administratorId", event.target.value)} inputMode="numeric" /></FormField>
      <FormField label="Desde"><input type="datetime-local" value={filters.from} onChange={(event) => update("from", event.target.value)} /></FormField>
      <FormField label="Hasta"><input type="datetime-local" value={filters.to} onChange={(event) => update("to", event.target.value)} /></FormField>
      <FormField label="Referencia"><input value={filters.reference} onChange={(event) => update("reference", event.target.value)} /></FormField>
      <button className="button button-primary">Filtrar</button>
    </form>
    {state.loading ? <LoadingState label="Cargando historial..." /> : state.error ? <ErrorState message={state.error} onRetry={() => load(page)} /> : <div className="panel table-panel">
      {result.content.length === 0 ? <EmptyState>No hay movimientos para los filtros seleccionados.</EmptyState> : <div className="table-wrap"><table><thead><tr><th>Fecha</th><th>Tipo</th><th>Producto</th><th>Bodega</th><th>Cantidad</th><th>Referencia</th><th>Administrador</th></tr></thead><tbody>{result.content.map((movement) => <tr key={movement.id}><td>{new Date(movement.createdAt).toLocaleString("es-CO")}</td><td><span className="badge">{movement.type === "INBOUND" ? "Entrada" : movement.type === "OUTBOUND" ? "Salida" : "Traslado"}</span></td><td>{productName(movement.productId)}</td><td>{movement.type === "TRANSFER" ? `${warehouseName(movement.sourceWarehouseId)} → ${warehouseName(movement.destinationWarehouseId)}` : warehouseName(movement.sourceWarehouseId || movement.destinationWarehouseId)}</td><td>{movement.quantity}</td><td>{movement.reference || "—"}</td><td>{movement.administratorUsername || `Administrador #${movement.administratorId}`}</td></tr>)}</tbody></table></div>}
      <Pagination page={result.number} totalPages={result.totalPages} onChange={(next) => load(next)} />
    </div>}
  </div>;
}

function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return <div className="pagination"><button className="button button-ghost button-small" disabled={page === 0} onClick={() => onChange(page - 1)}>Anterior</button><span>Página {page + 1} de {totalPages}</span><button className="button button-ghost button-small" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Siguiente</button></div>;
}

export function MovementsPage() {
  const [products, setProducts] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [type, setType] = useState("inbound");
  const [form, setForm] = useState({ productId: "", warehouseId: "", sourceWarehouseId: "", destinationWarehouseId: "", quantity: "", reference: "" });
  const [state, setState] = useState({ loading: true, error: "", message: "" });

  const load = async () => { setState({ loading: true, error: "", message: "" }); try { const [productList, warehouseList] = await Promise.all([api.products.list(), api.warehouses.list()]); setProducts(productList); setWarehouses(warehouseList); setState({ loading: false, error: "", message: "" }); } catch (error) { setState({ loading: false, error: error.message, message: "" }); } };
  useEffect(() => { load(); }, []);
  const availableProducts = useMemo(() => type === "outbound" ? products : products.filter((product) => !product.discontinued), [products, type]);
  const update = (field, value) => setForm((current) => ({ ...current, [field]: value }));
  const submit = async (event) => {
    event.preventDefault();
    if (type === "transfer" && form.sourceWarehouseId === form.destinationWarehouseId) {
      setState((current) => ({ ...current, error: "La bodega de origen y destino deben ser diferentes.", message: "" }));
      return;
    }
    setState((current) => ({ ...current, error: "", message: "" }));
    try {
      const base = { productId: Number(form.productId), quantity: Number(form.quantity), reference: form.reference || null };
      if (type === "transfer") await api.movements.transfer({ ...base, sourceWarehouseId: Number(form.sourceWarehouseId), destinationWarehouseId: Number(form.destinationWarehouseId) });
      else if (type === "inbound") await api.movements.inbound({ ...base, warehouseId: Number(form.warehouseId) });
      else await api.movements.outbound({ ...base, warehouseId: Number(form.warehouseId) });
      setForm({ productId: "", warehouseId: "", sourceWarehouseId: "", destinationWarehouseId: "", quantity: "", reference: "" });
      setState((current) => ({ ...current, message: "Movimiento registrado correctamente." }));
    } catch (error) { setState((current) => ({ ...current, error: error.message })); }
  };
  if (state.loading) return <LoadingState label="Cargando opciones..." />;
  if (state.error && !products.length) return <ErrorState message={state.error} onRetry={load} />;
  return <section className="page-section"><div className="section-heading"><div><p className="eyebrow">Operación</p><h2>Movimientos</h2><p className="muted">Registra cambios de inventario con trazabilidad.</p></div></div>
    <div className="movement-layout"><div className="movement-tabs">{movementTypes.map((item) => <button type="button" key={item.key} className={type === item.key ? "movement-tab active" : "movement-tab"} onClick={() => { setType(item.key); setForm({ productId: "", warehouseId: "", sourceWarehouseId: "", destinationWarehouseId: "", quantity: "", reference: "" }); setState((current) => ({ ...current, error: "", message: "" })); }}><strong>{item.label}</strong><small>{item.description}</small></button>)}</div>
      <div className="panel movement-form"><h3>{movementTypes.find((item) => item.key === type).label}</h3><form onSubmit={submit}><FormField label="Producto"><select required value={form.productId} onChange={(event) => update("productId", event.target.value)}><option value="">Selecciona un producto</option>{availableProducts.map((product) => <option key={product.id} value={product.id}>{product.sku} · {product.name}</option>)}</select></FormField>
        {type === "transfer" ? <div className="form-row"><FormField label="Bodega de origen"><select required value={form.sourceWarehouseId} onChange={(event) => update("sourceWarehouseId", event.target.value)}><option value="">Selecciona una bodega</option>{warehouses.map((warehouse) => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}</select></FormField><FormField label="Bodega de destino"><select required value={form.destinationWarehouseId} onChange={(event) => update("destinationWarehouseId", event.target.value)}><option value="">Selecciona una bodega</option>{warehouses.map((warehouse) => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}</select></FormField></div> : <FormField label="Bodega"><select required value={form.warehouseId} onChange={(event) => update("warehouseId", event.target.value)}><option value="">Selecciona una bodega</option>{warehouses.map((warehouse) => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}</select></FormField>}
        <div className="form-row"><FormField label="Cantidad"><input required min="1" type="number" value={form.quantity} onChange={(event) => update("quantity", event.target.value)} /></FormField><FormField label="Referencia (opcional)"><input maxLength="255" value={form.reference} onChange={(event) => update("reference", event.target.value)} /></FormField></div>
        {state.error && <Notice tone="error">{state.error}</Notice>}{state.message && <Notice>{state.message}</Notice>}<button className="button button-primary">{state.message ? "Registrar otro movimiento" : "Registrar movimiento"}</button>
      </form></div>
    </div>
  </section>;
}
