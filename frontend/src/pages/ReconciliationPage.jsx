import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { api } from "../api/client";
import { ErrorState, LoadingState } from "../components/Feedback";
import { FormField } from "../components/FormField";

export function ReconciliationPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [stocks, setStocks] = useState([]);
  const [selection, setSelection] = useState({ productId: searchParams.get("productId") || "", warehouseId: searchParams.get("warehouseId") || "" });
  const [result, setResult] = useState(null);
  const [state, setState] = useState({ loading: true, error: "" });

  useEffect(() => {
    api.stocks.list().then(setStocks).then(() => setState({ loading: false, error: "" })).catch((error) => setState({ loading: false, error: error.message }));
  }, []);

  const selectedStock = stocks.find((stock) => String(stock.productId) === String(selection.productId));
  const warehouses = selectedStock?.warehouses || [];
  const reconcile = async (event) => {
    event.preventDefault();
    if (!selection.productId || !selection.warehouseId) return;
    setState({ loading: true, error: "" });
    try {
      const stock = selectedStock;
      const warehouse = stock?.warehouses.find((item) => String(item.warehouseId) === String(selection.warehouseId));
      setResult({ ...(await api.stocks.reconcile(Number(selection.productId), Number(selection.warehouseId))), productName: stock?.productName, warehouseName: warehouse?.warehouseName });
      setSearchParams(selection);
      setState({ loading: false, error: "" });
    } catch (error) {
      setState({ loading: false, error: error.message });
    }
  };
  const updateProduct = (value) => {
    setSelection({ productId: value, warehouseId: "" });
    setResult(null);
  };

  if (state.loading && !stocks.length) return <LoadingState label="Cargando opciones..." />;
  if (state.error && !stocks.length) return <ErrorState message={state.error} />;
  return <section className="page-section">
    <div className="section-heading"><div><p className="eyebrow">Auditoría</p><h2>Conciliación</h2><p className="muted">Compara el saldo proyectado con el historial reconstruido.</p></div></div>
    <form className="panel filters-panel" onSubmit={reconcile}>
      <FormField label="Producto"><select required value={selection.productId} onChange={(event) => updateProduct(event.target.value)}><option value="">Selecciona un producto</option>{stocks.map((stock) => <option key={stock.productId} value={stock.productId}>{stock.sku} · {stock.productName}</option>)}</select></FormField>
      <FormField label="Bodega"><select required value={selection.warehouseId} onChange={(event) => { setSelection((current) => ({ ...current, warehouseId: event.target.value })); setResult(null); }} disabled={!selection.productId}><option value="">Selecciona una bodega</option>{warehouses.map((warehouse) => <option key={warehouse.warehouseId} value={warehouse.warehouseId}>{warehouse.warehouseName}</option>)}</select></FormField>
      <button className="button button-primary" disabled={state.loading}>Consultar conciliación</button>
    </form>
    {state.loading && <LoadingState label="Consultando conciliación..." />}
    {state.error && <ErrorState message={state.error} />}
    {result && !state.loading && <div className="panel reconciliation-panel"><div className="panel-heading"><div><h3>Conciliación de existencias</h3><p>{result.productName} · {result.warehouseName}</p></div></div><div className="reconciliation-values"><span>Saldo proyectado <strong>{result.projectedQuantity}</strong></span><span>Saldo reconstruido <strong>{result.reconstructedQuantity}</strong></span><span className={result.consistent ? "badge badge-success" : "badge badge-warning"}>{result.consistent ? "Consistente" : "Revisar diferencias"}</span></div></div>}
  </section>;
}
