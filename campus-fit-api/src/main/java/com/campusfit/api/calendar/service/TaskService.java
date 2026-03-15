package com.campusfit.api.calendar.service;

import com.campusfit.api.calendar.dto.TaskCreateRequest;
import com.campusfit.api.calendar.dto.TaskResponse;
import com.campusfit.api.calendar.dto.TaskUpdateRequest;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.Event;
import com.campusfit.api.domain.Task;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.EventRepository;
import com.campusfit.api.repository.TaskRepository;
import com.campusfit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public Task create(Long userId, TaskCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));

        Event linkedEvent = null;
        if (req.linkedEventId() != null) {
            linkedEvent = eventRepository.findById(req.linkedEventId())
                    .orElse(null);
        }

        return taskRepository.save(Task.builder()
                .user(user)
                .title(req.title())
                .scheduledDate(req.scheduledDate())
                .dueAt(req.dueAt())
                .category(req.category())
                .remindAt(req.remindAt())
                .linkedEvent(linkedEvent)
                .build());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long userId, Long taskId) {
        return TaskResponse.from(findOwned(userId, taskId));
    }

    public TaskResponse update(Long userId, Long taskId, TaskUpdateRequest req) {
        Task task = findOwned(userId, taskId);
        if (req.title() != null)
            task.setTitle(req.title());
        if (req.status() != null)
            task.setStatus(req.status());
        if (req.scheduledDate() != null)
            task.setScheduledDate(req.scheduledDate());
        if (req.dueAt() != null)
            task.setDueAt(req.dueAt());
        if (req.category() != null)
            task.setCategory(req.category());
        if (req.remindAt() != null)
            task.setRemindAt(req.remindAt());
        if (req.linkedEventId() != null) {
            Event linked = eventRepository.findById(req.linkedEventId()).orElse(null);
            task.setLinkedEvent(linked);
        }
        return TaskResponse.from(task);
    }

    public void delete(Long userId, Long taskId) {
        taskRepository.delete(findOwned(userId, taskId));
    }

    private Task findOwned(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> BusinessException.notFound("태스크를 찾을 수 없습니다."));
        if (!task.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }
        return task;
    }
}
