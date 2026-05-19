# perf-demo: 성능 테스트 실습 교육용 데모

## 개요

Scouter APM + JMeter를 활용한 성능 병목 분석 교육용 애플리케이션.
의도적 병목 5가지 시나리오를 `?optimized=true/false` 파라미터로 Before/After 전환하며 분석한다.

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 17 (Eclipse Adoptium) |
| Spring Boot | 3.2.1 |
| MyBatis | 3.0.3 |
| DB | H2 In-Memory  |
| API 문서 | SpringDoc OpenAPI 2.3.0 |

## 빠른 시작

```bash
# 빌드
mvn clean package -DskipTests

# 실행 (H2 인메모리 - Oracle 불필요)
java -jar target/perf-demo-1.0.0.jar

# Java 17 경로 지정 필요 시
"C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe" -jar target/perf-demo-1.0.0.jar
```

- Swagger UI: http://localhost:18081/swagger-ui.html
- H2 Console: http://localhost:18081/h2-console (JDBC URL: `jdbc:h2:mem:perfdb`, user: `sa`)
- 오프라인 Swagger: `docs/swagger-ui.html` 브라우저에서 열기

## 기동 시 자동 데이터 생성

서버 시작 시 `DataInitializer`가 테스트 데이터를 자동 생성한다 (약 23초).

| 테이블 | 건수 | 용도 |
|--------|------|------|
| PERF_PRODUCT | 1,000 | 상품 마스터 |
| PERF_ORDER | 100,000 | 주문 마스터 |
| PERF_ORDER_DETAIL | 500,000 | 주문 상세 |

## 9가지 병목 시나리오

### 기본 병목 (Baseline/Load 테스트)

| # | API | 병목 (Before) | 수정 (After) | 응답시간 |
|---|-----|---------------|--------------|----------|
| 1 | `GET /api/orders/{id}` | Thread.sleep(3s) | sleep 제거 | 3000ms → 50ms |
| 2 | `GET /api/orders/search?keyword=` | LIKE '%keyword%' 풀스캔 | prefix LIKE + LIMIT | 2000ms → 100ms |
| 3 | `GET /api/orders/detail-list` | N+1 쿼리 (1+N) | JOIN 한 방 | 2500ms → 150ms |
| 4 | `POST /api/orders/process` | 동기 대용량 로깅 | DEBUG + Async | 5000ms → 200ms |
| 5 | `GET /api/orders/report` | 앱 루프 + sleep | DB 집계 쿼리 | 1500ms → 300ms |

### 운영 장애 시뮬레이션 (Endurance/Stress/Load 테스트)

| # | API | 은행 업무 | 병목 (Before) | 수정 (After) | APM 관찰 지표 | 테스트 유형 |
|---|-----|---------|--------------|-------------|-------------|-----------|
| 6 | `GET /api/orders/cached-balance` | 계좌 잔액 캐시 조회 | static Map 무한 축적 | LRU 캐시 (100건) | HeapUsed 상승 | Endurance |
| 7 | `GET /api/orders/export-transactions` | 전체 거래내역 추출 | 10만건 전체 로드 + N+1 | 100건 JOIN | OOM Error | Stress/Peak |
| 8 | `POST /api/orders/validate-account` | 계좌번호 유효성 검증 | 매번 Pattern.compile + 해시 체이닝 | pre-compiled regex | ProcCpu 100% | Load/Stress |
| 9 | `POST /api/orders/transfer-external` | 타행 이체 확인 | 타임아웃 없는 RestTemplate (30초) | 3초 타임아웃 + 폴백 | Active Service 증가 | Load |

### 사용법

```bash
# Before (병목)
curl http://localhost:18081/api/orders/ORD00000001

# After (최적화)
curl "http://localhost:18081/api/orders/ORD00000001?optimized=true"
```

## JMeter 성능 테스트

### 연동 구조

```
┌─────────┐    HTTP 요청      ┌──────────────┐    Agent     ┌──────────┐
│  JMeter  │ ──────────────→  │  perf-demo   │ ──────────→  │ Scouter  │
│ (부하생성)│  localhost:18081  │ (Spring Boot)│              │  Server  │
└─────────┘  ←──────────────  └──────────────┘              └──────────┘
             JSON 응답                                       │ Paper UI
                                                             ↓
                                                      브라우저에서 확인
```

- **JMeter**: 부하 생성 도구. 여러 사용자(VUser)가 동시에 API를 호출하는 것을 시뮬레이션
- **perf-demo**: 테스트 대상 서버. JMeter가 보내는 HTTP 요청을 처리
- **Scouter**: APM 모니터링. XLog, TPS, 응답시간 등을 실시간 관찰

JMeter는 perf-demo와 별도 프로세스로 실행되며, HTTP 프로토콜로 통신한다.
JMX 파일은 "어떤 API를, 몇 명이, 얼마나 오래 호출할지"를 정의한 테스트 시나리오 설정 파일이다.

### JMeter 설치 및 실행

```bash
# 1. JMeter 다운로드 (5.6.3 권장)
#    https://jmeter.apache.org/download_jmeter.cgi
#    → apache-jmeter-5.6.3.zip 다운로드 후 압축 해제

# 2. JMeter GUI 실행
C:\apache-jmeter-5.6.3\bin\jmeter.bat

# 3. JMX 파일 로드
#    File → Open → jmeter/perf-demo-test-plan.jmx 선택
```

### 테스트 수행 절차

```bash
# Step 1: perf-demo 서버 기동 (서버가 먼저 떠 있어야 함!)
java -jar target/perf-demo-1.0.0.jar
# → "Test data initialization completed" 로그 확인 (약 23초)

# Step 2: JMeter에서 JMX 파일 로드
# Step 3: 원하는 테스트만 enable (우클릭 → Enable)
# Step 4: 초록색 ▶ 버튼 클릭하여 테스트 실행
# Step 5: 결과 확인 (Summary Report, Aggregate Report)
```

### 테스트 종류 (6.1 기준)

| # | 테스트 | VUser | 지속시간 | 목적 | 기본상태 |
|---|--------|-------|----------|------|----------|
| 1 | 단건 테스트 | 1 | 1회 | 스크립트 정상 동작 확인 | disabled |
| 2 | Baseline Before | 10 | 3분 | 병목 버전 기준선 측정 | disabled |
| 3 | Baseline After | 10 | 3분 | 최적화 버전 기준선 측정 | disabled |
| 4 | Load Before | 30 | 5분 | 부하 시 병목 심화 확인 | disabled |
| 5 | Load After | 30 | 5분 | 부하 시 최적화 효과 확인 | **enabled** |
| 6 | Stress | 10→20→40→80 | 단계별 2분 | 한계점(Saturation) 탐색 | disabled |
| 7 | Endurance | 20 | 30분 | 장시간 안정성, 메모리 누수 | disabled |

- **1번(단건)**부터 시작하여 스크립트 정상 동작을 먼저 확인
- 이후 2~7번을 순서대로 enable하여 실행
- Scouter Paper를 동시에 관찰하면 XLog, TPS, 응답시간 변화를 실시간으로 확인 가능

### JMX 설정 변수

JMX 파일 상단의 User Defined Variables에서 환경에 맞게 수정 가능:

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `HOST` | localhost | perf-demo 서버 주소 |
| `PORT` | 18081 | perf-demo 서버 포트 |
| `THINK_TIME` | 500 | 요청 간 대기시간 (ms) |
| `ORDER_MAX` | 100000 | 랜덤 주문ID 범위 |
| `CUST_MAX` | 5000 | 랜덤 고객번호 범위 |

## 프로젝트 구조

```
perf-demo/
├── pom.xml
├── docs/
│   ├── README.md              ← 이 파일
│   ├── openapi.json           ← OpenAPI 3.0 스펙
│   └── swagger-ui.html        ← 오프라인 Swagger UI
├── jmeter/
│   └── perf-demo-test-plan.jmx  ← JMeter 테스트 계획
├── conf/                      ← Scouter Agent 설정 (3 인스턴스)
├── sql/                       ← Oracle용 SQL (H2 모드에서는 불필요)
├── start-all.bat              ← 3 인스턴스 동시 기동
├── stop-all.bat               ← 전체 종료
└── src/main/
    ├── java/.../perfdemo/
    │   ├── domain/order/
    │   │   ├── controller/    OrderController (토글 포함)
    │   │   ├── service/       OrderService (인터페이스)
    │   │   │                  OrderServiceImpl (병목 버전)
    │   │   │                  OrderServiceOptimized (최적화 버전)
    │   │   ├── mapper/        OrderMapper, OrderQueryMapper, OrderDetailMapper
    │   │   ├── entity/        PerfOrder, PerfOrderDetail, PerfProduct
    │   │   └── dto/response/  4개 응답 DTO
    │   └── global/
    │       ├── config/        SwaggerConfig, DataInitializer
    │       ├── dto/           ApiResponse
    │       └── exception/     GlobalExceptionHandler
    └── resources/
        ├── application.yml    (H2 기본)
        ├── application-oracle.yml (Oracle 선택)
        ├── logback-spring.xml (sync/async 프로파일)
        ├── schema.sql         (H2 DDL)
        └── mapper/order/      MyBatis XML 3개
```

## 토글 메커니즘

`OrderController`에서 `@Qualifier`로 두 Service 빈을 주입받고,
`?optimized` 파라미터로 분기한다:

```java
@Qualifier("orderService") OrderService orderService,           // 병목
@Qualifier("orderServiceOptimized") OrderService optimized      // 최적화

private OrderService resolve(boolean optimized) {
    return optimized ? this.optimized : this.orderService;
}
```

## 멀티 인스턴스 (Scouter 연동)

Scouter APM으로 모니터링 시 3개 인스턴스 동시 기동:

```bash
start-all.bat
```

| 인스턴스 | 포트 | Scouter obj_name |
|---------|------|------------------|
| 서버1 | 18081 | perf-demo-1 |
| 서버2 | 18082 | perf-demo-2 |
| 서버3 | 18083 | perf-demo-3 |

