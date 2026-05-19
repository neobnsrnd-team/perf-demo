package com.example.perfdemo.domain.order.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfOrderDetail {
    private String detailId;
    private String orderId;
    private String productId;
    private String productName;
    private int quantity;
    private long unitPrice;
    private long lineAmount;
}
