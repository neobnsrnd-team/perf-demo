package com.example.perfdemo.domain.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "주문 상세 응답")
public class OrderDetailResponseDTO {

    @Schema(description = "상세 ID")
    private String detailId;

    @Schema(description = "주문 ID")
    private String orderId;

    @Schema(description = "상품 ID")
    private String productId;

    @Schema(description = "상품명")
    private String productName;

    @Schema(description = "수량")
    private int quantity;

    @Schema(description = "단가")
    private long unitPrice;

    @Schema(description = "소계")
    private long lineAmount;
}
