package com.campusfit.api.calendar.service;

import com.campusfit.api.calendar.dto.TaskCreateRequest;
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
}
