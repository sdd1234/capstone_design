package com.campusfit.api.repository;

import com.campusfit.api.domain.Event;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByUserIdAndStartAtBetween(Long userId, LocalDateTime from, LocalDateTime to);

    List<Event> findByUserId(Long userId);
}
