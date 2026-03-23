import apiClient from "./client";

export const getAcademicCalendarEvents = (year) =>
  apiClient.get("/api/v1/academic-calendar/events", {
    params: year ? { year } : {},
  });
export const listLectures = (params) =>
  apiClient.get("/api/v1/lectures", { params });
export const getLecture = (id) => apiClient.get(`/api/v1/lectures/${id}`);
export const getDepts = (year, termSeason) =>
  apiClient.get("/api/v1/lectures/depts", { params: { year, termSeason } });
export const getPrerequisites = (courseId) =>
  apiClient.get(`/api/v1/courses/${courseId}/prerequisites`);
export const importLectures = (formData) =>
  apiClient.post("/api/v1/admin/lectures/import", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
export const getImportHistory = () =>
  apiClient.get("/api/v1/admin/lectures/import/history");
