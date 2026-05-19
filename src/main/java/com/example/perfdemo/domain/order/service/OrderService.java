package com.example.perfdemo.domain.order.service;

import com.example.perfdemo.domain.order.dto.response.OrderReportResponseDTO;
import com.example.perfdemo.domain.order.dto.response.OrderResponseDTO;
import com.example.perfdemo.domain.order.dto.response.OrderWithDetailsResponseDTO;

import java.util.List;

public interface OrderService {

    /** Scenario 1: 단건 조회 */
    OrderResponseDTO findOrderById(String orderId);

    /** Scenario 2: 키워드 검색 */
    List<OrderResponseDTO> searchOrders(String keyword);

    /** Scenario 3: 주문+상세 목록 */
    List<OrderWithDetailsResponseDTO> fetchOrdersWithDetails(int limit);

    /** Scenario 4: 주문 처리 (로깅) */
    String processOrders(int count);

    /** Scenario 5: 주문 리포트 */
    List<OrderReportResponseDTO> generateOrderReport(String startDate, String endDate);

    /** Scenario 6: 계좌 잔액 캐시 조회 (메모리 누수) */
    List<OrderResponseDTO> cachedBalance(String accountNo);

    /** Scenario 7: 전체 거래내역 추출 (OOM) */
    List<OrderWithDetailsResponseDTO> exportTransactions();

    /** Scenario 8: 계좌번호 유효성 일괄 검증 (CPU) */
    String validateAccount(int count);

    /** Scenario 9: 타행 이체 확인 (외부 타임아웃) */
    OrderResponseDTO transferExternal(String accountNo);
}
