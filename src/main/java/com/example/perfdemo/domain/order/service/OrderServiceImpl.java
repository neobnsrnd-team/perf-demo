package com.example.perfdemo.domain.order.service;

import com.example.perfdemo.domain.order.dto.response.*;
import com.example.perfdemo.domain.order.entity.PerfOrder;
import com.example.perfdemo.domain.order.mapper.OrderDetailMapper;
import com.example.perfdemo.domain.order.mapper.OrderMapper;
import com.example.perfdemo.domain.order.mapper.OrderQueryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 병목 버전 (Before) — 의도적으로 느리게 구현
 */
@Slf4j
@Service("orderService")
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderQueryMapper orderQueryMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final RestTemplate restTemplateNoTimeout;

    @Value("${server.port:18081}")
    private int serverPort;

    /** Scenario 6: 메모리 누수 — 계좌 잔액 캐시가 절대 제거되지 않는 static Map */
    private static final ConcurrentHashMap<String, List<OrderResponseDTO>> balanceCache = new ConcurrentHashMap<>();

    public OrderServiceImpl(
            OrderMapper orderMapper,
            OrderQueryMapper orderQueryMapper,
            OrderDetailMapper orderDetailMapper,
            @Qualifier("restTemplateNoTimeout") RestTemplate restTemplateNoTimeout) {
        this.orderMapper = orderMapper;
        this.orderQueryMapper = orderQueryMapper;
        this.orderDetailMapper = orderDetailMapper;
        this.restTemplateNoTimeout = restTemplateNoTimeout;
    }

    /**
     * Scenario 1: Thread.sleep 병목
     * - 외부 API 호출을 시뮬레이션하는 3초 sleep
     * - Scouter 프로파일에서 METHOD 구간에 3초 블로킹 표시
     */
    @Override
    public OrderResponseDTO findOrderById(String orderId) {
        PerfOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }

        // ★ 병목: 외부 API 호출 시뮬레이션
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    /**
     * Scenario 2: 느린 SQL (Full Table Scan)
     * - LIKE '%keyword%' → 인덱스 미사용, 10만건 풀스캔
     * - Scouter 프로파일에서 SQL 구간에 2초+ 표시
     */
    @Override
    public List<OrderResponseDTO> searchOrders(String keyword) {
        return orderQueryMapper.searchByKeywordSlow(keyword);
    }

    /**
     * Scenario 3: N+1 쿼리 문제
     * - findRecentOrders()로 주문 목록 조회 후
     * - 루프에서 건별 findByOrderId() 호출 (1+N 쿼리)
     * - Scouter 프로파일에서 동일 SQL N회 반복 표시
     */
    @Override
    public List<OrderWithDetailsResponseDTO> fetchOrdersWithDetails(int limit) {
        List<PerfOrder> orders = orderQueryMapper.findRecentOrders(limit);
        List<OrderWithDetailsResponseDTO> result = new ArrayList<>();

        // ★ 병목: N+1 쿼리
        for (PerfOrder order : orders) {
            OrderWithDetailsResponseDTO dto = new OrderWithDetailsResponseDTO();
            dto.setOrderId(order.getOrderId());
            dto.setCustomerName(order.getCustomerName());
            dto.setOrderStatus(order.getOrderStatus());
            dto.setTotalAmount(order.getTotalAmount());

            // 건별 상세 조회 (N번 실행)
            List<OrderDetailResponseDTO> details = orderDetailMapper.findByOrderId(order.getOrderId());
            dto.setDetails(details);

            result.add(dto);
        }
        return result;
    }

    /**
     * Scenario 4: 로깅 병목
     * - 루프 내에서 대용량 문자열을 동기 로깅
     * - File I/O 블로킹으로 전체 처리 시간 증가
     * - Scouter 프로파일에서 Logger 관련 METHOD에 시간 집중
     */
    @Override
    public String processOrders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            String payload = "Processing order #" + i + " | "
                    + "timestamp=" + System.currentTimeMillis() + " | "
                    + "data=" + "X".repeat(500);

            // ★ 병목: 대용량 문자열 동기 로깅 (매 반복마다 File I/O)
            log.info("[ORDER_PROCESS] {}", payload);

            sb.append("OK-").append(i).append(" ");
        }
        return "Processed " + count + " orders";
    }

    /**
     * Scenario 5: 쿼리 간 서비스 병목 (Mixed)
     * - Query A(빠름) → 무거운 루프 처리 → Query B(빠름)
     * - Scouter 프로파일에서 SQL → METHOD 갭 → SQL 패턴 명확
     */
    @Override
    public List<OrderReportResponseDTO> generateOrderReport(String startDate, String endDate) {
        // Query A: 빠른 조회
        List<OrderResponseDTO> orders = orderQueryMapper.searchByKeywordFast("고객");

        // ★ 병목: 애플리케이션에서 무거운 루프 처리
        long total = 0;
        for (int i = 0; i < orders.size(); i++) {
            total += orders.get(i).getTotalAmount();
            // 의도적 지연
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.debug("Calculated total in app: {}", total);

        // Query B: 빠른 조회 (실제 리포트)
        return orderQueryMapper.findOrderReport(startDate, endDate);
    }

    /**
     * Scenario 6: 메모리 누수 — 계좌 잔액 캐시 (Endurance 테스트)
     * - static ConcurrentHashMap에 조회 결과를 계속 축적
     * - 결과를 10배 복사하여 메모리 소비 가속
     * - 캐시 만료/제거 로직 없음 → HeapUsed 지속 상승
     * - Scouter: HeapUsed 차트가 GC 후에도 우상향
     */
    @Override
    public List<OrderResponseDTO> cachedBalance(String accountNo) {
        String cacheKey = "BAL_" + accountNo + "_" + System.nanoTime();

        List<OrderResponseDTO> result = orderQueryMapper.searchByKeywordSlow(accountNo);

        // ★ 병목: 결과를 10배 복사하여 메모리에 축적 (절대 제거 안 함)
        List<OrderResponseDTO> inflated = new ArrayList<>(result.size() * 10);
        for (int i = 0; i < 10; i++) {
            for (OrderResponseDTO dto : result) {
                OrderResponseDTO copy = new OrderResponseDTO();
                copy.setOrderId(dto.getOrderId());
                copy.setCustomerId(dto.getCustomerId());
                copy.setCustomerName(dto.getCustomerName());
                copy.setOrderStatus(dto.getOrderStatus());
                copy.setTotalAmount(dto.getTotalAmount());
                copy.setOrderDate(dto.getOrderDate());
                inflated.add(copy);
            }
        }
        balanceCache.put(cacheKey, inflated);
        log.info("[BALANCE_CACHE] 캐시 적재: key={}, size={}, 총 캐시 건수={}",
                cacheKey, inflated.size(), balanceCache.size());

        return result;
    }

    /**
     * Scenario 7: OOM — 전체 거래내역 추출 (Stress/Peak 테스트)
     * - 10만건 전체 거래를 메모리에 로드
     * - 건별 상세 조회 (N+1 쿼리)
     * - 각 거래에 500byte 문자열 부풀리기
     * - Scouter: GC 빈발 → OOM Error 발생
     */
    @Override
    public List<OrderWithDetailsResponseDTO> exportTransactions() {
        // ★ 병목: 10만건 전체 로드
        List<PerfOrder> allOrders = orderQueryMapper.findAllOrders();
        List<OrderWithDetailsResponseDTO> result = new ArrayList<>();

        for (PerfOrder order : allOrders) {
            OrderWithDetailsResponseDTO dto = new OrderWithDetailsResponseDTO();
            dto.setOrderId(order.getOrderId());
            dto.setCustomerName(order.getCustomerName());
            dto.setOrderStatus(order.getOrderStatus());
            dto.setTotalAmount(order.getTotalAmount());

            // ★ 병목: 건별 상세 조회 (N+1)
            List<OrderDetailResponseDTO> details = orderDetailMapper.findByOrderId(order.getOrderId());
            dto.setDetails(details);

            result.add(dto);
        }
        log.info("[EXPORT_TX] 전체 거래 {}건 추출 완료", result.size());
        return result;
    }

    /**
     * Scenario 8: CPU 과점유 — 계좌번호 유효성 검증 (Load/Stress 테스트)
     * - 매 반복마다 Pattern.compile() 호출 (정규식 재컴파일)
     * - 100회 SHA-256 해시 체이닝 (CPU 집약 연산)
     * - Scouter: ProcCpu 100% 스파이크
     */
    @Override
    public String validateAccount(int count) {
        int validCount = 0;
        for (int i = 0; i < count; i++) {
            String accountNo = String.format("%03d-%04d-%06d", i % 100, i % 10000, i);

            // ★ 병목: 매번 Pattern.compile (정규식 재컴파일)
            Pattern pattern = Pattern.compile("^\\d{3}-\\d{4}-\\d{6}$");
            if (pattern.matcher(accountNo).matches()) {
                validCount++;
            }

            // ★ 병목: 100회 SHA-256 해시 체이닝 (전자서명 검증 시뮬레이션)
            try {
                String hash = accountNo;
                for (int j = 0; j < 100; j++) {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] digest = md.digest(hash.getBytes());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digest) {
                        sb.append(String.format("%02x", b));
                    }
                    hash = sb.toString();
                }
            } catch (Exception e) {
                log.error("[VALIDATE] 해시 오류", e);
            }
        }
        return String.format("계좌번호 검증 완료: %d/%d건 유효", validCount, count);
    }

    /**
     * Scenario 9: 타행 이체 확인 — 외부 타임아웃 (Load 테스트)
     * - 타임아웃 없는 RestTemplate으로 타행 Mock API 호출
     * - Mock API는 30초간 응답하지 않음
     * - Scouter: Active Service 수 증가, APICALL 30초 표시
     */
    @Override
    public OrderResponseDTO transferExternal(String accountNo) {
        String url = String.format(
                "http://localhost:%d/mock/external/transfer?accountNo=%s&delayMs=30000",
                serverPort, accountNo);

        // ★ 병목: 타임아웃 없는 RestTemplate → 타행 무응답 시 30초 블로킹
        restTemplateNoTimeout.getForObject(url, String.class);

        // 타행 확인 후 주문 조회
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
