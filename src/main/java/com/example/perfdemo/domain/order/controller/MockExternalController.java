package com.example.perfdemo.domain.order.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Scenario 9: 타행 이체 확인 Mock (외부 은행 응답 시뮬레이션)
 * - 실제 운영에서는 타행 시스템이 응답 지연/무응답하는 상황을 재현
 * - delayMs 파라미터로 응답 지연 시간 조절
 */
@Hidden
@RestController
@RequestMapping("/mock/external")
public class MockExternalController {

    @GetMapping("/transfer")
    public ResponseEntity<Map<String, Object>> verifyTransfer(
            @RequestParam String accountNo,
            @RequestParam(defaultValue = "30000") long delayMs) {

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok(Map.of(
                "resultCode", "0000",
                "resultMessage", "타행 이체 확인 완료",
                "accountNo", accountNo,
                "bankCode", "088",
                "bankName", "신한은행"
        ));
    }
}
