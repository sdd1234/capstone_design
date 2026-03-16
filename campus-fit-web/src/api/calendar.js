import apiClient from "./client";

export const getCalendar = (view, date) =>
  apiClient.get("/api/v1/calendar", { params: { view, date } });

// Events
export const listEvents = () => apiClient.get("/api/v1/events");
export const getEvent = (id) => apiClient.get(`/api/v1/events/${id}`);
export const createEvent = (data) => apiClient.post("/api/v1/events", data);
export const updateEvent = (id, data) =>
  apiClient.patch(`/api/v1/events/${id}`, data);
export const deleteEvent = (id) => apiClient.delete(`/api/v1/events/${id}`);

// Tasks
export const listTasks = () => apiClient.get("/api/v1/tasks");
export const getTask = (id) => apiClient.get(`/api/v1/tasks/${id}`);
export const createTask = (data) => apiClient.post("/api/v1/tasks", data);
export const updateTask = (id, data) =>
  apiClient.patch(`/api/v1/tasks/${id}`, data);
export const deleteTask = (id) => apiClient.delete(`/api/v1/tasks/${id}`);
