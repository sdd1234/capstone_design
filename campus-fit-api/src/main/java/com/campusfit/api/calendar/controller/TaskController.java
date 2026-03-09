package com.campusfit.api.calendar.controller;

import com.campusfit.api.calendar.dto.TaskCreateRequest;
import com.campusfit.api.calendar.service.TaskService;
import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.domain.Task;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @Valid @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        Task task = taskService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(Map.of(
                "id", task.getId(),
                "title", task.getTitle(),
                "status", task.getStatus().name())));
    }
}
