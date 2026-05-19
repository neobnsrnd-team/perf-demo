package com.example.perfdemo.domain.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "주문 + 상세 목록 응답 (N+1 시나리오)")
public class OrderWithDetailsResponseDTO {

    @Schema(description = "주문 ID")
    private String orderId;

    @Schema(description = "고객명")
    private String customerName;

    @Schema(description = "주문 상태")
    private String orderStatus;

    @Schema(description = "총 금액")
    private long totalAmount;

    @Schema(description = "주문 상세 목록")
    private List<OrderDetailResponseDTO> details;
}
