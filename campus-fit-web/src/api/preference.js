import apiClient from "./client";

export const getPreference = (year, termSeason) =>
  apiClient.get("/api/v1/timetable-preferences", {
    params: { year, termSeason },
  });
export const savePreference = (data) =>
  apiClient.put("/api/v1/timetable-preferences", data);
