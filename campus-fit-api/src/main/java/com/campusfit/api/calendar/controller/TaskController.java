package com.campusfit.api.calendar.controller;

import com.campusfit.api.calendar.dto.TaskCreateRequest;
import com.campusfit.api.calendar.dto.TaskResponse;
import com.campusfit.api.calendar.dto.TaskUpdateRequest;
import com.campusfit.api.calendar.service.TaskService;
import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.domain.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "태스크(할 일) 관리", description = "할 일 등록·조회·상태 변경·삭제 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "태스크 생성", description = "할 일을 등록합니다. linkedEventId로 연관 이벤트를 연결할 수 있습니다.")
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

    @Operation(summary = "태스크 목록 조회", description = "내 할 일 전체 목록을 최신 등록순으로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(taskService.list(userId)));
    }

    @Operation(summary = "태스크 상세 조회", description = "특정 할 일의 상태·기한·카테고리 등 상세 정보를 반환합니다.")
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> get(
            @Parameter(description = "태스크 ID") @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(taskService.get(userId, taskId)));
    }

    @Operation(summary = "태스크 수정·상태 변경", description = "변경할 필드만 포함해 부분 수정합니다.\n\nstatus 값: TODO → IN_PROGRESS → DONE")
    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @Parameter(description = "태스크 ID") @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(taskService.update(userId, taskId, request)));
    }

    @Operation(summary = "태스크 삭제", description = "특정 할 일을 삭제합니다. 본인 소유 태스크만 삭제 가능합니다.")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "태스크 ID") @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        taskService.delete(userId, taskId);
        return ResponseEntity.noContent().build();
    }
}
