import apiClient from "./client";

export const listVerifications = (status) =>
  apiClient.get("/api/v1/admin/student-verifications", {
    params: status ? { status } : {},
  });
export const getVerification = (id) =>
  apiClient.get(`/api/v1/admin/student-verifications/${id}`);
export const reviewVerification = (id, data) =>
  apiClient.patch(`/api/v1/admin/student-verifications/${id}`, data);
export const getVerificationFileUrl = (id) =>
  `/api/v1/admin/student-verifications/${id}/file`;
