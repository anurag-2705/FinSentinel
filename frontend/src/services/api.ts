import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach JWT access token to every authenticated request.
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("finsentinel_access_token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
);

export default api;