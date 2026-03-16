import apiClient from "./client";

export const listTimetables = () => apiClient.get("/api/v1/timetables");
export const getTimetable = (id) => apiClient.get(`/api/v1/timetables/${id}`);
export const createTimetable = (data) =>
  apiClient.post("/api/v1/timetables", data);
export const patchTimetable = (id, data) =>
  apiClient.patch(`/api/v1/timetables/${id}`, data);
export const deleteTimetable = (id) =>
  apiClient.delete(`/api/v1/timetables/${id}`);
