package com.example.perfdemo.domain.order.service;

import com.example.perfdemo.domain.order.dto.response.*;
import com.example.perfdemo.domain.order.entity.PerfOrder;
import com.example.perfdemo.domain.order.mapper.OrderMapper;
import com.example.perfdemo.domain.order.mapper.OrderQueryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 최적화 버전 (After) — 각 시나리오의 병목이 해결된 버전
 */
@Slf4j
@Service("orderServiceOptimized")
public class OrderServiceOptimized implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderQueryMapper orderQueryMapper;
    private final RestTemplate restTemplateWithTimeout;

    @Value("${server.port:18081}")
    private int serverPort;

    /** Scenario 6: LRU 캐시 (최대 100건) — 오래된 항목 자동 제거 */
    private static final Map<String, List<OrderResponseDTO>> balanceLruCache =
            Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<OrderResponseDTO>> eldest) {
                    return size() > 100;
                }
            });

    /** Scenario 8: pre-compiled 계좌번호 정규식 패턴 */
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{6}$");

    public OrderServiceOptimized(
            OrderMapper orderMapper,
            OrderQueryMapper orderQueryMapper,
            @Qualifier("restTemplateWithTimeout") RestTemplate restTemplateWithTimeout) {
        this.orderMapper = orderMapper;
        this.orderQueryMapper = orderQueryMapper;
        this.restTemplateWithTimeout = restTemplateWithTimeout;
    }

    /**
     * Scenario 1: sleep 제거
     * - 3000ms → ~50ms
     */
    @Override
    public OrderResponseDTO findOrderById(String orderId) {
        PerfOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }

        // ✅ sleep 제거 (실무: 캐시/비동기/서킷브레이커 적용)
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getOrderId());
        dto.setCustomerId(order.getCustomerId());
        dto.setCustomerName(order.getCustomerName());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOrderDate(order.getOrderDate());
        return dto;
    }

    /**
     * Scenario 2: 인덱스 + prefix LIKE + FETCH FIRST
     * - 2000ms → ~100ms
     */
    @Override
    public List<OrderResponseDTO> searchOrders(String keyword) {
        // ✅ searchByKeywordFast: LIKE 'keyword%' + FETCH FIRST 100 ROWS
        return orderQueryMapper.searchByKeywordFast(keyword);
    }

    /**
     * Scenario 3: JOIN 한 방 쿼리
     * - 51쿼리 → 1쿼리, 2500ms → ~150ms
     */
    @Override
    public List<OrderWithDetailsResponseDTO> fetchOrdersWithDetails(int limit) {
        // ✅ JOIN + <collection> resultMap으로 한 번에 조회
        return orderQueryMapper.findOrdersWithDetails(limit);
    }

    /**
     * Scenario 4: 로그 레벨 변경
     * - DEBUG 레벨이므로 운영(INFO)에서는 출력 안 됨
     * - 5000ms → ~200ms
     */
    @Override
    public String processOrders(int count) {
        for (int i = 0; i < count; i++) {
            // ✅ DEBUG 레벨: 운영 환경에서 출력되지 않음 (AsyncAppender와 함께 사용)
            if (log.isDebugEnabled()) {
                log.debug("[ORDER_PROCESS] Processing order #{}", i);
            }
        }
        return "Processed " + count + " orders (optimized)";
    }

    /**
     * Scenario 5: DB 집계 쿼리로 대체
     * - 애플리케이션 루프 제거, DB에서 GROUP BY + LISTAGG
     * - 1500ms → ~300ms
     */
    @Override
    public List<OrderReportResponseDTO> generateOrderReport(String startDate, String endDate) {
        // ✅ DB에서 한 번에 집계 (애플리케이션 루프 없음)
        return orderQueryMapper.findOrderReport(startDate, endDate);
    }

    /**
     * Scenario 6: LRU 캐시 — 계좌 잔액 캐시 (최적화)
     * - LinkedHashMap LRU (max 100건) → 오래된 항목 자동 제거
     * - 인덱스 활용 검색 (searchByKeywordFast)
     * - HeapUsed 안정 유지
     */
    @Override
    public List<OrderResponseDTO> cachedBalance(String accountNo) {
        // ✅ LRU 캐시: 최대 100건만 유지
        List<OrderResponseDTO> cached = balanceLruCache.get(accountNo);
        if (cached != null) {
            log.debug("[BALANCE_CACHE] 캐시 히트: accountNo={}", accountNo);
            return cached;
        }

        List<OrderResponseDTO> result = orderQueryMapper.searchByKeywordFast(accountNo);
        balanceLruCache.put(accountNo, result);
        log.debug("[BALANCE_CACHE] 캐시 적재: accountNo={}, 총 캐시 건수={}", accountNo, balanceLruCache.size());
        return result;
    }

    /**
     * Scenario 7: 페이지네이션 — 전체 거래내역 추출 (최적화)
     * - 100건만 JOIN으로 조회 (기존 findOrdersWithDetails 재사용)
     * - OOM 위험 없음
     */
    @Override
    public List<OrderWithDetailsResponseDTO> exportTransactions() {
        // ✅ 100건만 JOIN 조회 (10만건 전체 로드 대신)
        return orderQueryMapper.findOrdersWithDetails(100);
    }

    /**
     * Scenario 8: pre-compiled regex — 계좌번호 검증 (최적화)
     * - static final Pattern (정규식 1회 컴파일)
     * - 불필요한 해시 체이닝 제거
     * - ProcCpu 최소화
     */
    @Override
    public String validateAccount(int count) {
        int validCount = 0;
        for (int i = 0; i < count; i++) {
            String accountNo = String.format("%03d-%04d-%06d", i % 100, i % 10000, i);

            // ✅ pre-compiled 패턴 (매번 컴파일하지 않음)
            if (ACCOUNT_PATTERN.matcher(accountNo).matches()) {
                validCount++;
            }
            // ✅ 불필요한 해시 체이닝 제거
        }
        return String.format("계좌번호 검증 완료: %d/%d건 유효 (optimized)", validCount, count);
    }

    /**
     * Scenario 9: 타임아웃 + 폴백 — 타행 이체 확인 (최적화)
     * - 3초 타임아웃 RestTemplate
     * - 타행 무응답 시 폴백 응답 반환
     * - Active Service 수 안정 유지
     */
    @Override
    public OrderResponseDTO transferExternal(String accountNo) {
        String url = String.format(
                "http://localhost:%d/mock/external/transfer?accountNo=%s&delayMs=30000",
                serverPort, accountNo);

        try {
            // ✅ 3초 타임아웃 → 빠른 실패
            restTemplateWithTimeout.getForObject(url, String.class);
        } catch (Exception e) {
            log.warn("[TRANSFER] 타행 응답 타임아웃, 폴백 처리: accountNo={}", accountNo);
            // ✅ 폴백: 타행 확인 실패 시 재시도 안내
            OrderResponseDTO fallback = new OrderResponseDTO();
            fallback.setOrderId("TIMEOUT");
            fallback.setCustomerName("타행 응답 지연 — 잠시 후 재시도 (계좌: " + accountNo + ")");
            fallback.setOrderStatus("PENDING_RETRY");
            return fallback;
        }

        PerfOrder order = orderMapper.selectById(accountNo);
        if (order == null) {
            OrderResponseDTO fallback = new OrderResponseDTO();
            fallback.setOrderId("N/A");
            fallback.setCustomerName("타행 이체 확인 완료 (계좌: " + accountNo + ")");
            fallback.setOrderStatus("CONFIRMED");
            return fallback;
        }

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getOrderId());
        dto.setCustomerId(order.getCustomerId());
        dto.setCustomerName(order.getCustomerName());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOrderDate(order.getOrderDate());
        return dto;
    }
}
