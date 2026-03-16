import apiClient from "./client";

export const getMe = () => apiClient.get("/api/v1/users/me");
export const updateMe = (data) => apiClient.patch("/api/v1/users/me", data);
export const changePassword = (data) =>
  apiClient.patch("/api/v1/users/me/password", data);
