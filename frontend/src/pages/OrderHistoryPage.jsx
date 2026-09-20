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

function OrderResult({ order, products }) {
  const productName = (id) => products.find((product) => product.id === id)?.name || `Producto #${id}`;
  const dispatched = order.status === "DISPATCHED";
  return <div className={`order-result ${dispatched ? "result-success" : "result-warning"}`}><div className="result-header"><div><span className="eyebrow">Resultado del pedido</span><h3>Pedido #{order.id}</h3></div><span className="badge">{dispatched ? "Despachado" : "Cancelado"}</span></div><p>{dispatched ? "Todas las líneas fueron atendidas con el inventario disponible." : "El pedido no se aplicó porque una o más líneas no pudieron completarse."}</p><div className="order-lines">{order.lines.map((line) => <div className="order-line-result" key={line.id}><div><strong>{productName(line.productId)}</strong><small>Solicitado: {line.requestedQuantity}</small></div>{dispatched ? <div className="dispatch-list">{line.dispatchDetails.map((detail) => <span key={detail.warehouseId}>Bodega #{detail.warehouseId}: {detail.dispatchedQuantity}</span>)}</div> : <span className="quantity-low">Faltan {line.missingQuantity}</span>}</div>)}</div></div>;
}

export function OrderHistoryPage() {
  const [products, setProducts] = useState([]);
  const [filters, setFilters] = useState({ status: "", administratorId: "", from: "", to: "" });
  const [result, setResult] = useState({ content: [], number: 0, totalPages: 1 });
  const [selected, setSelected] = useState(null);
  const [state, setState] = useState({ loading: true, error: "" });
  const [detailState, setDetailState] = useState({ loading: false, error: "" });

  const load = async (page = 0, nextFilters = filters) => {
    setState({ loading: true, error: "" });
    try { setResult(pageContent(await api.orders.list(nextFilters, page))); setState({ loading: false, error: "" }); }
    catch (error) { setState({ loading: false, error: error.message }); }
  };
  useEffect(() => {
    api.products.list().then(setProducts).then(() => load()).catch((error) => setState({ loading: false, error: error.message }));
  }, []);
  const showDetail = async (id) => {
    setDetailState({ loading: true, error: "" });
    try { setSelected(await api.orders.findById(id)); setDetailState({ loading: false, error: "" }); }
    catch (error) { setDetailState({ loading: false, error: error.message }); }
  };
  const update = (field, value) => setFilters((current) => ({ ...current, [field]: value }));
  return <section className="page-section">
    <div className="section-heading"><div><p className="eyebrow">Consulta</p><h2>Historial de pedidos</h2><p className="muted">Consulta pedidos ordenados del más reciente al más antiguo.</p></div></div>
    <form className="panel filters-panel" onSubmit={(event) => { event.preventDefault(); load(0, { ...filters, from: asInstant(filters.from), to: asInstant(filters.to) }); }}>
      <FormField label="Estado"><select value={filters.status} onChange={(event) => update("status", event.target.value)}><option value="">Todos</option><option value="DISPATCHED">Despachado</option><option value="CANCELLED">Cancelado</option></select></FormField>
      <FormField label="Administrador"><input value={filters.administratorId} onChange={(event) => update("administratorId", event.target.value)} inputMode="numeric" /></FormField>
      <FormField label="Desde"><input type="datetime-local" value={filters.from} onChange={(event) => update("from", event.target.value)} /></FormField>
      <FormField label="Hasta"><input type="datetime-local" value={filters.to} onChange={(event) => update("to", event.target.value)} /></FormField>
      <button className="button button-primary">Filtrar</button>
    </form>
    {state.loading ? <LoadingState label="Cargando pedidos..." /> : state.error ? <ErrorState message={state.error} onRetry={() => load(result.number)} /> : <div className="panel table-panel">
      {result.content.length === 0 ? <EmptyState>No hay pedidos para los filtros seleccionados.</EmptyState> : <div className="table-wrap"><table><thead><tr><th>Pedido</th><th>Fecha</th><th>Estado</th><th>Líneas</th><th>Solicitado</th><th>Faltante</th><th>Administrador</th><th></th></tr></thead><tbody>{result.content.map((order) => <tr key={order.id}><td>#{order.id}</td><td>{new Date(order.createdAt).toLocaleString("es-CO")}</td><td><span className={`badge ${order.status === "DISPATCHED" ? "badge-success" : "badge-warning"}`}>{order.status === "DISPATCHED" ? "Despachado" : "Cancelado"}</span></td><td>{order.lineCount}</td><td>{order.requestedQuantityTotal}</td><td>{order.missingQuantityTotal}</td><td>{order.administratorUsername || `Administrador #${order.administratorId}`}</td><td><button className="text-button" onClick={() => showDetail(order.id)}>Ver detalle</button></td></tr>)}</tbody></table></div>}
      {result.totalPages > 1 && <div className="pagination"><button className="button button-ghost button-small" disabled={result.number === 0} onClick={() => load(result.number - 1)}>Anterior</button><span>Página {result.number + 1} de {result.totalPages}</span><button className="button button-ghost button-small" disabled={result.number >= result.totalPages - 1} onClick={() => load(result.number + 1)}>Siguiente</button></div>}
    </div>}
    {detailState.loading && <LoadingState label="Cargando detalle..." />}{detailState.error && <ErrorState message={detailState.error} />}{selected && !detailState.loading && <div className="panel detail-panel"><div className="panel-heading"><div><h3>Detalle del pedido #{selected.id}</h3><p>Administrador: {selected.administratorUsername || `#${selected.administratorId}`}</p></div><button className="text-button" onClick={() => setSelected(null)}>Cerrar</button></div><OrderResult order={selected} products={products} /></div>}
  </section>;
}
