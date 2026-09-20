export function LoadingState({ label = "Cargando..." }) {
  return <div className="feedback feedback-loading" role="status">{label}</div>;
}

export function EmptyState({ children = "No hay información para mostrar." }) {
  return <div className="feedback feedback-empty">{children}</div>;
}

export function ErrorState({ message, onRetry }) {
  return <div className="feedback feedback-error" role="alert">
    <span>{message}</span>
    {onRetry && <button type="button" className="button button-ghost" onClick={onRetry}>Reintentar</button>}
  </div>;
}

export function Notice({ children, tone = "success" }) {
  return <div className={`notice notice-${tone}`} role={tone === "error" ? "alert" : "status"}>{children}</div>;
}
