import { useEffect, useState } from "react";
import { api } from "../api/client";
import { EmptyState, ErrorState, LoadingState } from "../components/Feedback";
import { FormField } from "../components/FormField";

function pageContent(result) {
  return Array.isArray(result) ? { content: result, number: 0, totalPages: 1 } : { content: result?.content || [], number: result?.number || 0, totalPages: result?.totalPages || 1 };
}

function asInstant(value) {
  return value ? `${value}:00Z` : "";
}

export function MovementHistoryPage() {
  const [products, setProducts] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [administrators, setAdministrators] = useState([]);
  const [filters, setFilters] = useState({ type: "", productId: "", warehouseId: "", administratorId: "", from: "", to: "" });
  const [result, setResult] = useState({ content: [], number: 0, totalPages: 1 });
  const [state, setState] = useState({ loading: true, error: "" });

  const load = async (page = 0, nextFilters = filters) => {
    setState({ loading: true, error: "" });
    try {
      setResult(pageContent(await api.movements.list(nextFilters, page)));
      setState({ loading: false, error: "" });
    } catch (error) {
      setState({ loading: false, error: error.message });
    }
  };

  useEffect(() => {
    Promise.all([
      api.products.list(),
      api.warehouses.list(),
      api.auth.administrators().catch(() => [])
    ])
      .then(([productList, warehouseList, administratorList]) => {
        setProducts(productList);
        setWarehouses(warehouseList);
        setAdministrators(administratorList);
        return load(0);
      })
      .catch((error) => setState({ loading: false, error: error.message }));
  }, []);

  const update = (field, value) => setFilters((current) => ({ ...current, [field]: value }));
  const submit = (event) => {
    event.preventDefault();
    load(0, { ...filters, from: asInstant(filters.from), to: asInstant(filters.to) });
  };
  const productName = (id) => products.find((item) => item.id === id)?.name || `Producto #${id}`;
  const warehouseName = (id) => warehouses.find((item) => item.id === id)?.name || `Bodega #${id}`;

  return <section className="page-section">
    <div className="section-heading"><div><p className="eyebrow">Consulta</p><h2>Historial de movimientos</h2><p className="muted">Los movimientos más recientes aparecen primero.</p></div></div>
    <form className="panel filters-panel" onSubmit={submit}>
      <FormField label="Tipo"><select value={filters.type} onChange={(event) => update("type", event.target.value)}><option value="">Todos</option><option value="INBOUND">Entrada</option><option value="OUTBOUND">Salida</option><option value="TRANSFER">Traslado</option></select></FormField>
      <FormField label="Producto"><select value={filters.productId} onChange={(event) => update("productId", event.target.value)}><option value="">Todos</option>{products.map((product) => <option key={product.id} value={product.id}>{product.sku} · {product.name}</option>)}</select></FormField>
      <FormField label="Bodega"><select value={filters.warehouseId} onChange={(event) => update("warehouseId", event.target.value)}><option value="">Todas</option>{warehouses.map((warehouse) => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}</select></FormField>
      <FormField label="Administrador"><select value={filters.administratorId} onChange={(event) => update("administratorId", event.target.value)}><option value="">Todos</option>{administrators.map((administrator) => <option key={administrator.id} value={administrator.id}>{administrator.name || administrator.username}</option>)}</select></FormField>
      <FormField label="Desde"><input type="datetime-local" value={filters.from} onChange={(event) => update("from", event.target.value)} /></FormField>
      <FormField label="Hasta"><input type="datetime-local" value={filters.to} onChange={(event) => update("to", event.target.value)} /></FormField>
      <button className="button button-primary">Filtrar</button>
    </form>
    {state.loading ? <LoadingState label="Cargando historial..." /> : state.error ? <ErrorState message={state.error} onRetry={() => load(result.number)} /> : <div className="panel table-panel">
      {result.content.length === 0 ? <EmptyState>No hay movimientos para los filtros seleccionados.</EmptyState> : <div className="table-wrap"><table><thead><tr><th>Fecha</th><th>Tipo</th><th>Producto</th><th>Bodega</th><th>Cantidad</th><th>Administrador</th></tr></thead><tbody>{result.content.map((movement) => <tr key={movement.id}><td>{new Date(movement.createdAt).toLocaleString("es-CO")}</td><td><span className="badge">{movement.type === "INBOUND" ? "Entrada" : movement.type === "OUTBOUND" ? "Salida" : "Traslado"}</span></td><td>{productName(movement.productId)}</td><td>{movement.type === "TRANSFER" ? `${warehouseName(movement.sourceWarehouseId)} → ${warehouseName(movement.destinationWarehouseId)}` : warehouseName(movement.sourceWarehouseId || movement.destinationWarehouseId)}</td><td>{movement.quantity}</td><td>{movement.administratorUsername || `Administrador #${movement.administratorId}`}</td></tr>)}</tbody></table></div>}
      {result.totalPages > 1 && <div className="pagination"><button className="button button-ghost button-small" disabled={result.number === 0} onClick={() => load(result.number - 1)}>Anterior</button><span>Página {result.number + 1} de {result.totalPages}</span><button className="button button-ghost button-small" disabled={result.number >= result.totalPages - 1} onClick={() => load(result.number + 1)}>Siguiente</button></div>}
    </div>}
  </section>;
}
