package com.example.perfdemo.domain.order.controller;

import com.example.perfdemo.domain.order.dto.response.OrderReportResponseDTO;
import com.example.perfdemo.domain.order.dto.response.OrderResponseDTO;
import com.example.perfdemo.domain.order.dto.response.OrderWithDetailsResponseDTO;
import com.example.perfdemo.domain.order.service.OrderService;
import com.example.perfdemo.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "주문 API", description = "성능 병목 분석 교육용 API (?optimized=true/false로 Before/After 전환)")
public class OrderController {

    private final OrderService orderService;
    private final OrderService orderServiceOptimized;

    public OrderController(
            @Qualifier("orderService") OrderService orderService,
            @Qualifier("orderServiceOptimized") OrderService orderServiceOptimized) {
        this.orderService = orderService;
        this.orderServiceOptimized = orderServiceOptimized;
    }

    private OrderService resolve(boolean optimized) {
        return optimized ? orderServiceOptimized : orderService;
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Scenario 1: 단건 조회",
            description = "Before: Thread.sleep(3000) 병목 / After: sleep 제거")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> findOrder(
            @PathVariable String orderId,
            @Parameter(description = "true=최적화, false=병목")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).findOrderById(orderId)));
    }

    @GetMapping("/search")
    @Operation(summary = "Scenario 2: 키워드 검색",
            description = "Before: LIKE '%keyword%' 풀스캔 / After: 인덱스 + prefix LIKE")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> searchOrders(
            @RequestParam String keyword,
            @Parameter(description = "true=최적화, false=병목")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).searchOrders(keyword)));
    }

    @GetMapping("/detail-list")
    @Operation(summary = "Scenario 3: N+1 쿼리",
            description = "Before: 1+N 건별 조회 / After: JOIN 한 방 쿼리")
    public ResponseEntity<ApiResponse<List<OrderWithDetailsResponseDTO>>> fetchOrdersWithDetails(
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "true=최적화, false=병목")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).fetchOrdersWithDetails(limit)));
    }

    @PostMapping("/process")
    @Operation(summary = "Scenario 4: 로깅 병목",
            description = "Before: 동기 대용량 로깅 / After: DEBUG 레벨 + AsyncAppender")
    public ResponseEntity<ApiResponse<String>> processOrders(
            @RequestParam(defaultValue = "1000") int count,
            @Parameter(description = "true=최적화, false=병목")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).processOrders(count)));
    }

    @GetMapping("/report")
    @Operation(summary = "Scenario 5: 쿼리 간 서비스 병목",
            description = "Before: Query→루프→Query / After: DB 집계 쿼리")
    public ResponseEntity<ApiResponse<List<OrderReportResponseDTO>>> generateOrderReport(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @Parameter(description = "true=최적화, false=병목")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).generateOrderReport(startDate, endDate)));
    }

    // ===== 시나리오 6~9: 운영 장애 시뮬레이션 (은행 업무) =====

    @GetMapping("/cached-balance")
    @Operation(summary = "Scenario 6: 계좌 잔액 캐시 조회 (메모리 누수)",
            description = "Before: static Map 무한 축적 → HeapUsed 지속 상승 / After: LRU 캐시 (100건)")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> cachedBalance(
            @Parameter(description = "계좌번호 (고객명 검색 키워드)")
            @RequestParam String accountNo,
            @Parameter(description = "true=최적화(LRU), false=병목(무한축적)")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).cachedBalance(accountNo)));
    }

    @GetMapping("/export-transactions")
    @Operation(summary = "Scenario 7: 전체 거래내역 추출 (OOM)",
            description = "Before: 10만건 전체 로드 + N+1 → OOM / After: 페이지네이션 (100건 JOIN)")
    public ResponseEntity<ApiResponse<List<OrderWithDetailsResponseDTO>>> exportTransactions(
            @Parameter(description = "true=최적화(100건), false=병목(전체)")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).exportTransactions()));
    }

    @PostMapping("/validate-account")
    @Operation(summary = "Scenario 8: 계좌번호 유효성 검증 (CPU 과점유)",
            description = "Before: 매번 Pattern.compile + 해시 체이닝 → ProcCpu 100% / After: pre-compiled regex")
    public ResponseEntity<ApiResponse<String>> validateAccount(
            @Parameter(description = "검증할 계좌 수")
            @RequestParam(defaultValue = "10000") int count,
            @Parameter(description = "true=최적화, false=병목")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).validateAccount(count)));
    }

    @PostMapping("/transfer-external")
    @Operation(summary = "Scenario 9: 타행 이체 확인 (외부 타임아웃)",
            description = "Before: 타임아웃 없는 RestTemplate → 30초 블로킹 / After: 3초 타임아웃 + 폴백")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> transferExternal(
            @Parameter(description = "이체 대상 계좌번호")
            @RequestParam String accountNo,
            @Parameter(description = "true=최적화(3초 타임아웃), false=병목(무한대기)")
            @RequestParam(defaultValue = "false") boolean optimized) {
        return ResponseEntity.ok(ApiResponse.success(resolve(optimized).transferExternal(accountNo)));
    }
}
