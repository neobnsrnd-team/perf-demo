package com.example.perfdemo.domain.order.mapper;

import com.example.perfdemo.domain.order.entity.PerfOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper {

    PerfOrder selectById(@Param("orderId") String orderId);
}
