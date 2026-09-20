import { useEffect, useState } from "react";
import { api } from "../api/client";
import { EmptyState, ErrorState, LoadingState } from "../components/Feedback";

export function StockPage() {
  const [stocks, setStocks] = useState([]);
  const [state, setState] = useState({ loading: true, error: "" });
  const [filter, setFilter] = useState("all");

  const load = async () => {
    setState({ loading: true, error: "" });
    try { setStocks(await api.stocks.list()); setState({ loading: false, error: "" }); }
    catch (error) { setState({ loading: false, error: error.message }); }
  };
  useEffect(() => { load(); }, []);

  const visibleStocks = stocks.filter((stock) => filter === "all" || (filter === "low" && stock.belowMinimum) || (filter === "ok" && !stock.belowMinimum));

  if (state.loading) return <LoadingState label="Cargando existencias..." />;
  if (state.error) return <ErrorState message={state.error} onRetry={load} />;

  return <section className="page-section">
    <div className="section-heading"><div><p className="eyebrow">Consulta de inventario</p><h2>Existencias</h2><p className="muted">Compara el saldo total y el detalle por bodega.</p></div><button type="button" className="button button-ghost" onClick={load}>Actualizar</button></div>
    <div className="toolbar"><div className="filter-tabs"><button className={filter === "all" ? "filter-tab active" : "filter-tab"} onClick={() => setFilter("all")}>Todos <span>{stocks.length}</span></button><button className={filter === "low" ? "filter-tab active" : "filter-tab"} onClick={() => setFilter("low")}>Stock bajo <span>{stocks.filter((stock) => stock.belowMinimum).length}</span></button><button className={filter === "ok" ? "filter-tab active" : "filter-tab"} onClick={() => setFilter("ok")}>En nivel <span>{stocks.filter((stock) => !stock.belowMinimum).length}</span></button></div></div>
    <div className="panel table-panel">{visibleStocks.length === 0 ? <EmptyState>No hay productos en este filtro.</EmptyState> : <div className="table-wrap"><table><thead><tr><th>Producto</th><th>Stock mínimo</th><th>Total</th><th>Estado</th><th>Detalle por bodega</th></tr></thead><tbody>{visibleStocks.map((stock) => <tr key={stock.productId}><td><strong>{stock.productName}</strong><small className="table-subtitle">{stock.sku}</small></td><td>{stock.minimumStock}</td><td><strong>{stock.totalQuantity}</strong></td><td><span className={stock.belowMinimum ? "badge badge-warning" : "badge badge-success"}>{stock.belowMinimum ? "Bajo mínimo" : "En nivel"}</span></td><td><div className="stock-chips">{stock.warehouses.map((warehouse) => <span className="stock-chip" key={warehouse.warehouseId}><span>{warehouse.warehouseName}</span><strong>{warehouse.quantity}</strong></span>)}</div></td></tr>)}</tbody></table></div>}</div>
  </section>;
}
