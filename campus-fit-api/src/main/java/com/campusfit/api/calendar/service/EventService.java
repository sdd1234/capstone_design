package com.campusfit.api.calendar.service;

import com.campusfit.api.calendar.dto.EventCreateRequest;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.Event;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.EventRepository;
import com.campusfit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public Event create(Long userId, EventCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));

        return eventRepository.save(Event.builder()
                .user(user)
                .title(req.title())
                .category(req.category())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .allDay(Boolean.TRUE.equals(req.allDay()))
                .description(req.description())
                .remindAt(req.remindAt())
                .color(req.color())
                .build());
    }
}
