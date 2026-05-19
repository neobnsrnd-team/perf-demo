package com.example.perfdemo.domain.order.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfOrder {
    private String orderId;
    private String customerId;
    private String customerName;
    private String orderStatus;
    private long totalAmount;
    private String orderDate;
    private String createdAt;
}
