package com.campusfit.api.academic.service;

import com.campusfit.api.academic.dto.AcademicCalendarEventResponse;
import com.campusfit.api.repository.AcademicCalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicCalendarServiceImpl implements AcademicCalendarService {

    private final AcademicCalendarEventRepository eventRepository;

    @Override
    public List<AcademicCalendarEventResponse> getEvents(Integer year, Long universityId) {
        if (universityId != null) {
            return eventRepository.findByUniversityIdAndYear(universityId, year)
                    .stream().map(AcademicCalendarEventResponse::from).toList();
        }
        return eventRepository.findByYear(year)
                .stream().map(AcademicCalendarEventResponse::from).toList();
    }
}
