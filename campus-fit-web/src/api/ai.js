import apiClient from "./client";

export const createRecommendation = (data) =>
  apiClient.post("/api/v1/ai/timetable/recommendations", data);
export const listRecommendations = () =>
  apiClient.get("/api/v1/ai/timetable/recommendations");
export const getRecommendation = (id) =>
  apiClient.get(`/api/v1/ai/timetable/recommendations/${id}`);
export const deleteRecommendation = (id) =>
  apiClient.delete(`/api/v1/ai/timetable/recommendations/${id}`);
