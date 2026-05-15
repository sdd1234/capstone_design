package com.campusfit.api.calendar.service;

import com.campusfit.api.calendar.dto.EventCreateRequest;
import com.campusfit.api.calendar.dto.EventResponse;
import com.campusfit.api.calendar.dto.EventUpdateRequest;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.Event;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.EventRepository;
import com.campusfit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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

    @Transactional(readOnly = true)
    public List<EventResponse> list(Long userId) {
        return eventRepository.findByUserId(userId).stream()
                .map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long userId, Long eventId) {
        return EventResponse.from(findOwned(userId, eventId));
    }

    public EventResponse update(Long userId, Long eventId, EventUpdateRequest req) {
        Event event = findOwned(userId, eventId);
        if (req.title() != null)
            event.setTitle(req.title());
        if (req.category() != null)
            event.setCategory(req.category());
        if (req.startAt() != null)
            event.setStartAt(req.startAt());
        if (req.endAt() != null)
            event.setEndAt(req.endAt());
        if (req.allDay() != null)
            event.setAllDay(req.allDay());
        if (req.description() != null)
            event.setDescription(req.description());
        if (req.remindAt() != null)
            event.setRemindAt(req.remindAt());
        if (req.color() != null)
            event.setColor(req.color());
        return EventResponse.from(event);
    }

    public void delete(Long userId, Long eventId) {
        eventRepository.delete(findOwned(userId, eventId));
    }

    private Event findOwned(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.notFound("이벤트를 찾을 수 없습니다."));
        if (!event.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }
        return event;
    }
}
