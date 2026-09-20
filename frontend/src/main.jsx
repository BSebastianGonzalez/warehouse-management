import React, { createContext, useContext, useState } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { api } from "./api/client";
import { AppLayout } from "./components/AppLayout";
import { FormField } from "./components/FormField";
import { Notice } from "./components/Feedback";
import "./styles.css";

const AuthContext = createContext(null);

function AuthProvider({ children }) {
  const [admin, setAdmin] = useState(null);
  const value = {
    admin,
    login: async (credentials) => {
      const result = await api.auth.login(credentials);
      setAdmin(result);
      return result;
    },
    register: (data) => api.auth.register(data),
    logout: async () => {
      await api.auth.logout();
      setAdmin(null);
    }
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

function useAuth() {
  return useContext(AuthContext);
}

function LoginPage() {
  const { admin, login, register } = useAuth();
  const navigate = useNavigate();
  const [registering, setRegistering] = useState(false);
  const [form, setForm] = useState({ username: "", password: "", name: "" });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  if (admin) {
    return <Navigate to="/" replace />;
  }

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      if (registering) {
        await register(form);
        setRegistering(false);
        setSuccess("Administrador creado. Ya puedes iniciar sesión.");
      } else {
        await login(form);
        navigate("/");
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  };

  return <main className="auth-page">
    <section className="auth-card">
      <div className="auth-brand"><span className="brand-mark">W</span><span><strong>Warehouse</strong><small>Control center</small></span></div>
      <p className="eyebrow">{registering ? "Configuración inicial" : "Bienvenido de nuevo"}</p>
      <h1>{registering ? "Crear administrador" : "Iniciar sesión"}</h1>
      <p className="muted">{registering ? "Registra la cuenta que operará el inventario." : "Accede para gestionar el inventario y los pedidos."}</p>
      <form onSubmit={submit}>
        {registering && <FormField label="Nombre"><input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></FormField>}
        <FormField label="Usuario"><input required autoComplete="username" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} /></FormField>
        <FormField label="Contraseña"><input required type="password" autoComplete={registering ? "new-password" : "current-password"} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></FormField>
        {error && <Notice tone="error">{error}</Notice>}
        {success && <Notice>{success}</Notice>}
        <button className="button button-primary button-wide" disabled={loading}>{loading ? "Procesando..." : registering ? "Crear administrador" : "Entrar"}</button>
      </form>
      <button type="button" className="text-button" onClick={() => { setRegistering(!registering); setError(""); setSuccess(""); }}>
        {registering ? "Volver al inicio de sesión" : "Crear administrador inicial"}
      </button>
    </section>
  </main>;
}

function ProtectedRoute() {
  const { admin, logout } = useAuth();
  const location = useLocation();
  return admin ? <AppLayout admin={admin} onLogout={logout} /> : <Navigate to="/login" replace state={{ from: location }} />;
}

function GuestRoute() {
  const { admin } = useAuth();
  return admin ? <Navigate to="/" replace /> : <LoginPage />;
}

function PlaceholderPage({ title, description }) {
  return <section className="page-section"><div className="section-heading"><div><p className="eyebrow">Próximamente</p><h2>{title}</h2><p className="muted">{description}</p></div></div><div className="panel empty-panel"><span className="empty-icon">✦</span><strong>Esta sección se habilitará en la siguiente fase</strong><span className="muted">La base de navegación ya está lista.</span></div></section>;
}

function AppRoutes() {
  return <Routes>
    <Route path="/login" element={<GuestRoute />} />
    <Route element={<ProtectedRoute />}>
      <Route index element={<PlaceholderPage title="Resumen de inventario" description="Una vista rápida del estado de tus productos y bodegas." />} />
      <Route path="inventory" element={<PlaceholderPage title="Existencias" description="Consulta el stock por producto y bodega." />} />
      <Route path="products" element={<PlaceholderPage title="Productos" description="Administra el catálogo de productos." />} />
      <Route path="warehouses" element={<PlaceholderPage title="Bodegas" description="Administra las bodegas de la operación." />} />
      <Route path="movements" element={<PlaceholderPage title="Movimientos" description="Registra entradas, salidas y traslados." />} />
      <Route path="orders" element={<PlaceholderPage title="Pedidos" description="Crea pedidos y revisa su resultado." />} />
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>;
}

function App() {
  return <AuthProvider><BrowserRouter><AppRoutes /></BrowserRouter></AuthProvider>;
}

createRoot(document.getElementById("root")).render(<React.StrictMode><App /></React.StrictMode>);
