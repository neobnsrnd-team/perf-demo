package com.example.perfdemo.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Perf Demo API")
                        .description("APM 성능 병목 분석 교육용 데모 API\n\n"
                                + "모든 API에 ?optimized=true/false 파라미터로 Before/After 전환 가능\n\n"
                                + "**기본 병목 (Baseline/Load)**\n\n"
                                + "| # | API | 병목 | 수정 |\n"
                                + "|---|-----|------|------|\n"
                                + "| 1 | GET /api/orders/{id} | Thread.sleep(3s) | sleep 제거 |\n"
                                + "| 2 | GET /api/orders/search | LIKE '%keyword%' | 인덱스 + prefix LIKE |\n"
                                + "| 3 | GET /api/orders/detail-list | N+1 쿼리 | JOIN 한 방 |\n"
                                + "| 4 | POST /api/orders/process | 동기 로깅 | DEBUG + Async |\n"
                                + "| 5 | GET /api/orders/report | 앱 루프 | DB 집계 |\n\n"
                                + "**운영 장애 시뮬레이션 (Endurance/Stress/Load)**\n\n"
                                + "| # | API | 은행 업무 | 병목 | 수정 |\n"
                                + "|---|-----|---------|------|------|\n"
                                + "| 6 | GET /cached-balance | 계좌 잔액 캐시 | static Map 무한축적 | LRU 캐시 (100건) |\n"
                                + "| 7 | GET /export-transactions | 거래내역 추출 | 10만건 로드+N+1 | 100건 JOIN |\n"
                                + "| 8 | POST /validate-account | 계좌번호 검증 | Pattern.compile 반복 | pre-compiled |\n"
                                + "| 9 | POST /transfer-external | 타행 이체 확인 | 타임아웃 없음(30초) | 3초+폴백 |")
                        .version("1.0.0"));
    }
}
