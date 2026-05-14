package com.campusfit.api.repository;

import com.campusfit.api.domain.ActivityGoal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityGoalRepository extends JpaRepository<ActivityGoal, Long> {
    List<ActivityGoal> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
