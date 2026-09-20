import { useEffect, useState } from "react";
import { api } from "../api/client";
import { EmptyState, ErrorState, LoadingState, Notice } from "../components/Feedback";
import { FormField } from "../components/FormField";

function CatalogCard({ title, children }) {
  return <div className="panel catalog-form"><h3>{title}</h3>{children}</div>;
}

export function ProductsPage() {
  const empty = { sku: "", name: "", unitOfMeasure: "", minimumStock: 10 };
  const [products, setProducts] = useState([]);
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);
  const [state, setState] = useState({ loading: true, error: "", message: "" });

  const load = async () => {
    setState({ loading: true, error: "", message: "" });
    try { setProducts(await api.products.list()); setState({ loading: false, error: "", message: "" }); }
    catch (error) { setState({ loading: false, error: error.message, message: "" }); }
  };
  useEffect(() => { load(); }, []);
  const submit = async (event) => {
    event.preventDefault();
    setState((current) => ({ ...current, error: "", message: "" }));
    try {
      const payload = { ...form, minimumStock: Number(form.minimumStock) };
      if (editingId) await api.products.update(editingId, payload);
      else await api.products.create(payload);
      setForm(empty); setEditingId(null); setState((current) => ({ ...current, message: editingId ? "Producto actualizado." : "Producto creado." })); await load();
    } catch (error) { setState((current) => ({ ...current, error: error.message })); }
  };
  const edit = (product) => { setEditingId(product.id); setForm({ sku: product.sku, name: product.name, unitOfMeasure: product.unitOfMeasure, minimumStock: product.minimumStock }); };
  const toggleDiscontinued = async (product) => {
    try { await api.products.setDiscontinued(product.id, !product.discontinued); await load(); }
    catch (error) { setState((current) => ({ ...current, error: error.message })); }
  };
  if (state.loading) return <LoadingState label="Cargando productos..." />;
  if (state.error && !products.length) return <ErrorState message={state.error} onRetry={load} />;
  return <section className="page-section"><div className="section-heading"><div><p className="eyebrow">Catálogo</p><h2>Productos</h2><p className="muted">Define los productos y su nivel mínimo de inventario.</p></div><button className="button button-ghost" onClick={load}>Actualizar</button></div>
    <div className="catalog-grid"><CatalogCard title={editingId ? "Editar producto" : "Nuevo producto"}><form onSubmit={submit}><FormField label="SKU"><input required value={form.sku} onChange={(event) => setForm({ ...form, sku: event.target.value })} /></FormField><FormField label="Nombre"><input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></FormField><FormField label="Unidad de medida"><input required value={form.unitOfMeasure} onChange={(event) => setForm({ ...form, unitOfMeasure: event.target.value })} /></FormField><FormField label="Stock mínimo"><input required min="0" type="number" value={form.minimumStock} onChange={(event) => setForm({ ...form, minimumStock: event.target.value })} /></FormField><div className="form-actions"><button className="button button-primary">{editingId ? "Guardar cambios" : "Crear producto"}</button>{editingId && <button type="button" className="button button-ghost" onClick={() => { setEditingId(null); setForm(empty); }}>Cancelar</button>}</div>{state.message && <Notice>{state.message}</Notice>}{state.error && <Notice tone="error">{state.error}</Notice>}</form></CatalogCard>
      <div className="panel table-panel"><div className="panel-heading"><div><h3>Productos registrados</h3><p className="muted">{products.length} productos</p></div></div>{products.length === 0 ? <EmptyState>No hay productos registrados.</EmptyState> : <div className="table-wrap"><table><thead><tr><th>Producto</th><th>Unidad</th><th>Mínimo</th><th>Estado</th><th /></tr></thead><tbody>{products.map((product) => <tr key={product.id}><td><strong>{product.name}</strong><small className="table-subtitle">{product.sku}</small></td><td>{product.unitOfMeasure}</td><td>{product.minimumStock}</td><td><span className={product.discontinued ? "badge badge-warning" : "badge badge-success"}>{product.discontinued ? "Descontinuado" : "Activo"}</span></td><td><div className="row-actions"><button className="button button-small button-ghost" onClick={() => edit(product)}>Editar</button><button className="button button-small button-ghost" onClick={() => toggleDiscontinued(product)}>{product.discontinued ? "Reactivar" : "Descontinuar"}</button></div></td></tr>)}</tbody></table></div>}</div>
    </div>
  </section>;
}

export function WarehousesPage() {
  const empty = { name: "", location: "" };
  const [warehouses, setWarehouses] = useState([]);
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);
  const [state, setState] = useState({ loading: true, error: "", message: "" });
  const load = async () => { setState({ loading: true, error: "", message: "" }); try { setWarehouses(await api.warehouses.list()); setState({ loading: false, error: "", message: "" }); } catch (error) { setState({ loading: false, error: error.message, message: "" }); } };
  useEffect(() => { load(); }, []);
  const submit = async (event) => { event.preventDefault(); try { if (editingId) await api.warehouses.update(editingId, form); else await api.warehouses.create(form); setForm(empty); setEditingId(null); setState((current) => ({ ...current, message: editingId ? "Bodega actualizada." : "Bodega creada.", error: "" })); await load(); } catch (error) { setState((current) => ({ ...current, error: error.message })); } };
  if (state.loading) return <LoadingState label="Cargando bodegas..." />;
  if (state.error && !warehouses.length) return <ErrorState message={state.error} onRetry={load} />;
  return <section className="page-section"><div className="section-heading"><div><p className="eyebrow">Catálogo</p><h2>Bodegas</h2><p className="muted">Administra las ubicaciones desde las que se despacha.</p></div><button className="button button-ghost" onClick={load}>Actualizar</button></div>
    <div className="catalog-grid"><CatalogCard title={editingId ? "Editar bodega" : "Nueva bodega"}><form onSubmit={submit}><FormField label="Nombre"><input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></FormField><FormField label="Ubicación"><input required value={form.location} onChange={(event) => setForm({ ...form, location: event.target.value })} /></FormField><div className="form-actions"><button className="button button-primary">{editingId ? "Guardar cambios" : "Crear bodega"}</button>{editingId && <button type="button" className="button button-ghost" onClick={() => { setEditingId(null); setForm(empty); }}>Cancelar</button>}</div>{state.message && <Notice>{state.message}</Notice>}{state.error && <Notice tone="error">{state.error}</Notice>}</form></CatalogCard>
      <div className="panel table-panel"><div className="panel-heading"><div><h3>Bodegas registradas</h3><p className="muted">{warehouses.length} bodegas</p></div></div>{warehouses.length === 0 ? <EmptyState>No hay bodegas registradas.</EmptyState> : <div className="warehouse-cards">{warehouses.map((warehouse) => <div className="warehouse-card" key={warehouse.id}><span className="warehouse-card-icon">⌂</span><div><strong>{warehouse.name}</strong><small>{warehouse.location}</small></div><button className="button button-small button-ghost" onClick={() => { setEditingId(warehouse.id); setForm({ name: warehouse.name, location: warehouse.location }); }}>Editar</button></div>)}</div>}</div>
    </div>
  </section>;
}
