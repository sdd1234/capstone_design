import apiClient from "./client";

export const listPersonalBlocks = (year, termSeason) =>
  apiClient.get("/api/v1/personal-blocks", { params: { year, termSeason } });
export const createPersonalBlock = (data) =>
  apiClient.post("/api/v1/personal-blocks", data);
export const deletePersonalBlock = (id) =>
  apiClient.delete(`/api/v1/personal-blocks/${id}`);
