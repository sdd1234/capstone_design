package com.campusfit.api.academic.dto;

import com.campusfit.api.domain.CoursePrerequisite;

public record PrerequisiteResponse(
        Long courseId,
        String courseName,
        Integer credits) {
    public static PrerequisiteResponse from(CoursePrerequisite p) {
        return new PrerequisiteResponse(
                p.getPrerequisiteCourse().getId(),
                p.getPrerequisiteCourse().getName(),
                p.getPrerequisiteCourse().getCredits());
    }
}
