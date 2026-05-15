import apiClient from "./client";

export const login = (email, password) =>
  apiClient.post("/api/v1/auth/login", { email, password });

export const signup = (formData) =>
  apiClient.post("/api/v1/auth/signup", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

export const refresh = (refreshToken) =>
  apiClient.post("/api/v1/auth/refresh", { refreshToken });
