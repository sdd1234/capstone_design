import apiClient from "./client";

export const listGoals = () => apiClient.get("/api/v1/time-management/goals");
export const createGoal = (data, year, termSeason) =>
  apiClient.post("/api/v1/time-management/goals", data, {
    params: year && termSeason ? { year, termSeason } : {},
  });
export const updateGoal = (id, data, year, termSeason) =>
  apiClient.patch(`/api/v1/time-management/goals/${id}`, data, {
    params: year && termSeason ? { year, termSeason } : {},
  });
export const deleteGoal = (id) =>
  apiClient.delete(`/api/v1/time-management/goals/${id}`);

export const getBlankSlots = (year, termSeason, weekStart) =>
  apiClient.get("/api/v1/time-management/blank-slots", {
    params: { year, termSeason, ...(weekStart ? { weekStart } : {}) },
  });

export const getActivityRecommendations = (year, termSeason, weekStart) =>
  apiClient.get("/api/v1/time-management/recommendations", {
    params: { year, termSeason, ...(weekStart ? { weekStart } : {}) },
  });

export const listAppliedAllocations = (weekStart) =>
  apiClient.get("/api/v1/time-management/applied-allocations", {
    params: weekStart ? { weekStart } : {},
  });

export const getWeeklySummary = (weekStart) =>
  apiClient.get("/api/v1/time-management/applied-allocations/summary", {
    params: weekStart ? { weekStart } : {},
  });

export const applyAllocations = (items, weekStart) =>
  apiClient.put("/api/v1/time-management/applied-allocations", {
    items,
    ...(weekStart ? { weekStart } : {}),
  });

export const toggleAllocationCompleted = (allocationId, completed) =>
  apiClient.patch(
    `/api/v1/time-management/applied-allocations/${allocationId}/complete`,
    { completed },
  );

export const clearAppliedAllocationsForWeek = (weekStart) =>
  apiClient.delete("/api/v1/time-management/applied-allocations/week", {
    params: weekStart ? { weekStart } : {},
  });
