import { NavLink, Outlet } from "react-router-dom";

const navigation = [
  { to: "/", label: "Resumen", icon: "▦", end: true },
  { to: "/inventory", label: "Existencias", icon: "◫" },
  { to: "/products", label: "Productos", icon: "◇" },
  { to: "/warehouses", label: "Bodegas", icon: "⌂" },
  { to: "/movements", label: "Movimientos", icon: "↔" },
  { to: "/orders", label: "Pedidos", icon: "☷" }
];

export function AppLayout({ admin, onLogout }) {
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-mark">W</span>
        <span><strong>Warehouse</strong><small>Control center</small></span>
      </div>
      <nav className="main-nav" aria-label="Navegación principal">
        <p className="nav-label">Operación</p>
        {navigation.map((item) => <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
          <span className="nav-icon" aria-hidden="true">{item.icon}</span>{item.label}
        </NavLink>)}
      </nav>
      <div className="sidebar-footer"><span className="status-dot" /> Sistema operativo</div>
    </aside>
    <div className="app-content">
      <header className="topbar">
        <div><p className="eyebrow">Panel de control</p><h1>Gestión de inventario</h1></div>
        <div className="account">
          <div className="avatar">{admin.name?.charAt(0)?.toUpperCase() || "A"}</div>
          <div><strong>{admin.name}</strong><small>Administrador</small></div>
          <button type="button" className="button button-ghost" onClick={onLogout}>Cerrar sesión</button>
        </div>
      </header>
      <main className="page-content"><Outlet /></main>
    </div>
  </div>;
}
