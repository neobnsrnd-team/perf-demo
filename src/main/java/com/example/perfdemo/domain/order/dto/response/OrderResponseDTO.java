package com.example.perfdemo.domain.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "주문 응답")
public class OrderResponseDTO {

    @Schema(description = "주문 ID", example = "ORD-00001")
    private String orderId;

    @Schema(description = "고객 ID")
    private String customerId;

    @Schema(description = "고객명")
    private String customerName;

    @Schema(description = "주문 상태")
    private String orderStatus;

    @Schema(description = "총 금액")
    private long totalAmount;

    @Schema(description = "주문일")
    private String orderDate;
}
