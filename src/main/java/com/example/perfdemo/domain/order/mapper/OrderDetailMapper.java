package com.example.perfdemo.domain.order.mapper;

import com.example.perfdemo.domain.order.dto.response.OrderDetailResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /** Scenario 3: N+1 - 건별 조회 (병목) */
    List<OrderDetailResponseDTO> findByOrderId(@Param("orderId") String orderId);
}
