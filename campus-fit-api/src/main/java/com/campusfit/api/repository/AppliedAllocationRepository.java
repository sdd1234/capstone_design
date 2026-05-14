package com.campusfit.api.repository;

import com.campusfit.api.domain.AppliedAllocation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppliedAllocationRepository extends JpaRepository<AppliedAllocation, Long> {

    @Query("SELECT a FROM AppliedAllocation a JOIN FETCH a.goal WHERE a.user.id = :userId AND a.weekStart = :weekStart ORDER BY a.dayOfWeek, a.startTime")
    List<AppliedAllocation> findByUserAndWeek(@Param("userId") Long userId, @Param("weekStart") LocalDate weekStart);

    @Query("SELECT a FROM AppliedAllocation a JOIN FETCH a.goal WHERE a.user.id = :userId ORDER BY a.weekStart DESC, a.dayOfWeek, a.startTime")
    List<AppliedAllocation> findAllByUserIdWithGoal(@Param("userId") Long userId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndWeekStartAndGoalId(Long userId, LocalDate weekStart, Long goalId);

    void deleteByGoalId(Long goalId);
}
