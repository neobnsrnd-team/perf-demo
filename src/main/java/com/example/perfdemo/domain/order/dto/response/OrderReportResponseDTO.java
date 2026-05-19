package com.example.perfdemo.domain.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "주문 리포트 응답 (Mixed 병목 시나리오)")
public class OrderReportResponseDTO {

    @Schema(description = "주문 상태")
    private String orderStatus;

    @Schema(description = "건수")
    private int orderCount;

    @Schema(description = "총 금액 합계")
    private long totalAmountSum;

    @Schema(description = "상품 목록 (LISTAGG)")
    private String productNames;
}
