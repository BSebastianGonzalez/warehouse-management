export function FormField({ label, error, children, hint }) {
  return <label className="form-field">
    <span>{label}</span>
    {children}
    {hint && <small>{hint}</small>}
    {error && <small className="field-error">{error}</small>}
  </label>;
}
