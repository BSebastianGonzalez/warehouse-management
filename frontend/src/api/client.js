const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

export class ApiError extends Error {
  constructor(message, status, details = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

function getErrorMessage(body, status) {
  if (body?.message) return body.message;
  if (body?.errors && typeof body.errors === "object") {
    return Object.values(body.errors).join(" ");
  }
  if (status === 401 || status === 403) return "Tu sesión no permite realizar esta operación.";
  if (status >= 500) return "El servidor no pudo completar la solicitud.";
  return "No se pudo completar la solicitud.";
}

export async function request(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });

  if (response.status === 204) return null;

  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(getErrorMessage(body, response.status), response.status, body);
  }
  return body;
}

export const api = {
  auth: {
    login: (data) => request("/auth/login", { method: "POST", body: JSON.stringify(data) }),
    register: (data) => request("/auth/register", { method: "POST", body: JSON.stringify(data) }),
    logout: () => request("/auth/logout", { method: "POST" })
  },
  products: {
    list: () => request("/products"),
    create: (data) => request("/products", { method: "POST", body: JSON.stringify(data) }),
    update: (id, data) => request(`/products/${id}`, { method: "PUT", body: JSON.stringify(data) }),
    setDiscontinued: (id, discontinued) => request(`/products/${id}/discontinued`, {
      method: "PATCH",
      body: JSON.stringify({ discontinued })
    })
  },
  warehouses: {
    list: () => request("/warehouses"),
    create: (data) => request("/warehouses", { method: "POST", body: JSON.stringify(data) }),
    update: (id, data) => request(`/warehouses/${id}`, { method: "PUT", body: JSON.stringify(data) })
  },
  stocks: {
    list: () => request("/stocks"),
    byProduct: (productId) => request(`/stocks/products/${productId}`)
  },
  movements: {
    inbound: (data) => request("/movements/inbound", { method: "POST", body: JSON.stringify(data) }),
    outbound: (data) => request("/movements/outbound", { method: "POST", body: JSON.stringify(data) }),
    transfer: (data) => request("/movements/transfers", { method: "POST", body: JSON.stringify(data) })
  },
  orders: {
    create: (data) => request("/orders", { method: "POST", body: JSON.stringify(data) }),
    findById: (id) => request(`/orders/${id}`)
  }
};
