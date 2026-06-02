package com.campusfit.api.personalblock.service;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.personalblock.dto.PersonalBlockCreateRequest;
import com.campusfit.api.personalblock.dto.PersonalBlockResponse;
import java.util.List;

public interface PersonalBlockService {
    List<PersonalBlockResponse> list(Long userId, Integer year, TermSeason termSeason);

    PersonalBlockResponse create(Long userId, PersonalBlockCreateRequest request);

    void delete(Long userId, Long blockId);
}
