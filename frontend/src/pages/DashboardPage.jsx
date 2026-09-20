import { useEffect, useMemo, useState } from "react";
import { api } from "../api/client";
import { EmptyState, ErrorState, LoadingState } from "../components/Feedback";

function Metric({ label, value, detail, tone = "blue" }) {
  return <div className={`metric-card metric-${tone}`}><span className="metric-label">{label}</span><strong>{value}</strong><small>{detail}</small></div>;
}

export function DashboardPage() {
  const [data, setData] = useState({ products: [], warehouses: [], stocks: [] });
  const [state, setState] = useState({ loading: true, error: "" });

  const load = async () => {
    setState({ loading: true, error: "" });
    try {
      const [products, warehouses, stocks] = await Promise.all([api.products.list(), api.warehouses.list(), api.stocks.list()]);
      setData({ products, warehouses, stocks });
      setState({ loading: false, error: "" });
    } catch (error) {
      setState({ loading: false, error: error.message });
    }
  };

  useEffect(() => { load(); }, []);

  const lowStock = useMemo(() => data.stocks.filter((stock) => stock.belowMinimum), [data.stocks]);
  const warehouseTotals = useMemo(() => data.stocks.reduce((totals, product) => {
    product.warehouses.forEach((warehouse) => { totals[warehouse.warehouseName] = (totals[warehouse.warehouseName] || 0) + warehouse.quantity; });
    return totals;
  }, {}), [data.stocks]);

  if (state.loading) return <LoadingState label="Cargando resumen..." />;
  if (state.error) return <ErrorState message={state.error} onRetry={load} />;

  return <section className="page-section">
    <div className="section-heading"><div><p className="eyebrow">Visión general</p><h2>Resumen de inventario</h2><p className="muted">Consulta rápida del estado actual de la operación.</p></div><button type="button" className="button button-ghost" onClick={load}>Actualizar</button></div>
    <div className="metrics-grid">
      <Metric label="Productos" value={data.products.length} detail="en catálogo" />
      <Metric label="Bodegas" value={data.warehouses.length} detail="activas" tone="purple" />
      <Metric label="Stock bajo" value={lowStock.length} detail="productos por debajo del mínimo" tone={lowStock.length ? "orange" : "green"} />
    </div>
    <div className="dashboard-grid">
      <div className="panel dashboard-panel"><div className="panel-heading"><div><h3>Stock bajo mínimo</h3><p className="muted">Requiere atención</p></div></div>
        {lowStock.length === 0 ? <EmptyState>Todos los productos están sobre el mínimo.</EmptyState> : <div className="alert-list">{lowStock.slice(0, 5).map((stock) => <div className="alert-row" key={stock.productId}><span className="alert-dot" /><div><strong>{stock.productName}</strong><small>{stock.sku}</small></div><span className="quantity-low">{stock.totalQuantity} / {stock.minimumStock}</span></div>)}</div>}
      </div>
      <div className="panel dashboard-panel"><div className="panel-heading"><div><h3>Stock por bodega</h3><p className="muted">Unidades disponibles</p></div></div>
        {Object.keys(warehouseTotals).length === 0 ? <EmptyState>No hay existencias registradas.</EmptyState> : <div className="warehouse-list">{Object.entries(warehouseTotals).map(([name, total]) => <div className="warehouse-row" key={name}><span>{name}</span><strong>{total}</strong></div>)}</div>}
      </div>
    </div>
  </section>;
}
