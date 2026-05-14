package com.campusfit.api.timemanagement.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 기본 NoOp 구현 — 입력 그대로, 추론 없음.
 * ChatGPT 연동 시 OpenAiRawInputParser를 @Primary로 만들어 대체.
 */
@Service
@Primary
public class NoOpRawInputParser implements RawInputParser {
    @Override
    public Parsed parse(String rawInput) {
        return new Parsed(null, null, null, null, null);
    }
}
