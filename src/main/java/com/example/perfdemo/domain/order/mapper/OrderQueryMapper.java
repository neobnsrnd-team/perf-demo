package com.example.perfdemo.domain.order.mapper;

import com.example.perfdemo.domain.order.dto.response.OrderReportResponseDTO;
import com.example.perfdemo.domain.order.dto.response.OrderResponseDTO;
import com.example.perfdemo.domain.order.dto.response.OrderWithDetailsResponseDTO;
import com.example.perfdemo.domain.order.entity.PerfOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderQueryMapper {

    /** Scenario 2: 느린 검색 (Full Table Scan) */
    List<OrderResponseDTO> searchByKeywordSlow(@Param("keyword") String keyword);

    /** Scenario 2: 최적화 검색 (인덱스 + prefix LIKE) */
    List<OrderResponseDTO> searchByKeywordFast(@Param("keyword") String keyword);

    /** Scenario 3: N+1용 - 주문 목록 (상세 없이) */
    List<PerfOrder> findRecentOrders(@Param("limit") int limit);

    /** Scenario 3: 최적화 - JOIN으로 한 번에 조회 */
    List<OrderWithDetailsResponseDTO> findOrdersWithDetails(@Param("limit") int limit);

    /** Scenario 5: 최적화 - DB 집계 쿼리 */
    List<OrderReportResponseDTO> findOrderReport(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /** Scenario 7: 전체 거래내역 추출 (병목 - LIMIT 없이 10만건 전체 로드) */
    List<PerfOrder> findAllOrders();
}
