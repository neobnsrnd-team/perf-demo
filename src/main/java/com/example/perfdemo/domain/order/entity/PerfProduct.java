package com.example.perfdemo.domain.order.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfProduct {
    private String productId;
    private String productName;
    private String category;
    private long price;
}
