package com.campusfit.api.calendar.service;

import com.campusfit.api.calendar.dto.CalendarItemResponse;
import com.campusfit.api.common.enums.CalendarView;
import com.campusfit.api.domain.Event;
import com.campusfit.api.domain.Task;
import com.campusfit.api.repository.EventRepository;
import com.campusfit.api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarServiceImpl implements CalendarService {

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;

    @Override
    public List<CalendarItemResponse> getCalendar(Long userId, CalendarView view, LocalDate date) {
        LocalDateTime from;
        LocalDateTime to;

        switch (view) {
            case DAY -> {
                from = date.atStartOfDay();
                to = date.plusDays(1).atStartOfDay();
            }
            case WEEK -> {
                from = date.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
                to = from.plusWeeks(1);
            }
            default -> { // MONTH
                from = date.withDayOfMonth(1).atStartOfDay();
                to = date.withDayOfMonth(1).plusMonths(1).atStartOfDay();
            }
        }

        List<CalendarItemResponse> items = new ArrayList<>();

        List<Event> events = eventRepository.findByUserIdAndStartAtBetween(userId, from, to);
        for (Event e : events) {
            items.add(new CalendarItemResponse(
                    "EVENT", e.getId(), e.getTitle(),
                    e.getCategory(), e.getStartAt(), e.getEndAt(), e.getAllDay(), e.getColor(),
                    null, null, null));
        }

        List<Task> tasks = taskRepository.findByUserId(userId);
        for (Task t : tasks) {
            if (t.getScheduledDate() != null &&
                    !t.getScheduledDate().isBefore(from.toLocalDate()) &&
                    t.getScheduledDate().isBefore(to.toLocalDate())) {
                items.add(new CalendarItemResponse(
                        "TASK", t.getId(), t.getTitle(),
                        null, null, null, null, null,
                        t.getStatus(), t.getScheduledDate(), t.getDueAt()));
            }
        }

        return items;
    }
}
