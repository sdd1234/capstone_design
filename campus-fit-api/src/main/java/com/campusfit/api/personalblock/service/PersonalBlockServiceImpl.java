package com.campusfit.api.personalblock.service;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.PersonalBlock;
import com.campusfit.api.domain.User;
import com.campusfit.api.personalblock.dto.PersonalBlockCreateRequest;
import com.campusfit.api.personalblock.dto.PersonalBlockResponse;
import com.campusfit.api.repository.PersonalBlockRepository;
import com.campusfit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalBlockServiceImpl implements PersonalBlockService {

    private final PersonalBlockRepository personalBlockRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PersonalBlockResponse> list(Long userId, Integer year, TermSeason termSeason) {
        return personalBlockRepository
                .findByUserIdAndYearAndTermSeasonOrderByDayOfWeekAscStartTimeAsc(userId, year, termSeason)
                .stream().map(PersonalBlockResponse::from).toList();
    }

    @Override
    public PersonalBlockResponse create(Long userId, PersonalBlockCreateRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw BusinessException.badRequest("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        PersonalBlock block = PersonalBlock.builder()
                .user(user)
                .year(req.year())
                .termSeason(req.termSeason())
                .title(req.title())
                .dayOfWeek(req.dayOfWeek())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .color(req.color())
                .build();
        return PersonalBlockResponse.from(personalBlockRepository.save(block));
    }

    @Override
    public void delete(Long userId, Long blockId) {
        PersonalBlock b = personalBlockRepository.findById(blockId)
                .orElseThrow(() -> BusinessException.notFound("개인 일정을 찾을 수 없습니다."));
        if (!b.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }
        personalBlockRepository.delete(b);
    }
}
