# perf-demo 실행 가이드: Swagger UI + JMeter CLI

## 목차

1. [서버 빌드 및 기동](#1-서버-빌드-및-기동)
2. [Swagger UI 사용법](#2-swagger-ui-사용법)
3. [JMeter CLI 실행법](#3-jmeter-cli-실행법)
4. [JMX 테스트 파일 구성](#4-jmx-테스트-파일-구성)
5. [실전 테스트 수행 절차](#5-실전-테스트-수행-절차)
6. [Swagger ↔ JMeter 연동 워크플로우](#6-swagger--jmeter-연동-워크플로우)

---

## 1. 서버 빌드 및 기동

### 사전 요구사항

- Java 17 (Eclipse Adoptium 권장)
- Maven 3.x

### 빌드

```bash
cd C:\java\perf-demo
mvn clean package -DskipTests
```

### 단일 인스턴스 기동

```bash
# 기본 실행
java -jar target/perf-demo-1.0.0.jar

# Java 17 경로 직접 지정
"C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe" -jar target/perf-demo-1.0.0.jar
```

서버 시작 후 `Test data initialization completed` 로그가 나올 때까지 대기 (약 23초).
이 시간 동안 테스트 데이터가 자동 생성된다 (주문 10만건, 상세 50만건).

### 멀티 인스턴스 기동 (Scouter 연동 시)

```bash
start-all.bat    # 3개 인스턴스 동시 기동 (18081, 18082, 18083)
stop-all.bat     # 전체 종료
```

---

## 2. Swagger UI 사용법

### 접속 방법

서버 기동 후 브라우저에서 아래 URL 접속:

| 방법 | URL |
|------|-----|
| **온라인 Swagger** (서버 필요) | http://localhost:18081/swagger-ui.html |
| **오프라인 Swagger** (서버 불필요) | `docs/swagger-ui.html` 파일을 브라우저에서 직접 열기 |
| **H2 DB 콘솔** | http://localhost:18081/h2-console |

> H2 콘솔 접속 정보: JDBC URL `jdbc:h2:mem:perfdb`, User `sa`, Password 없음

### Swagger UI에서 API 테스트하기

1. 브라우저에서 http://localhost:18081/swagger-ui.html 접속
2. API 목록에서 테스트할 엔드포인트 클릭 (예: `GET /api/orders/{orderId}`)
3. **Try it out** 버튼 클릭
4. 파라미터 입력:
   - `orderId`: `ORD00000001`
   - `optimized`: `false` (병목) 또는 `true` (최적화)
5. **Execute** 버튼 클릭
6. Response body에서 결과 확인

### 9가지 시나리오 Swagger 테스트 예시

#### 기본 병목 (S1~S5)

| # | 엔드포인트 | 파라미터 |
|---|-----------|----------|
| S1 | `GET /api/orders/{orderId}` | orderId=`ORD00000001` |
| S2 | `GET /api/orders/search` | keyword=`Customer-100` |
| S3 | `GET /api/orders/detail-list` | limit=`50` |
| S4 | `POST /api/orders/process` | count=`1000` |
| S5 | `GET /api/orders/report` | startDate=`2025-06-01`, endDate=`2025-06-30` |

#### 운영 장애 시뮬레이션 (S6~S9)

| # | 엔드포인트 | 파라미터 | 주의사항 |
|---|-----------|----------|----------|
| S6 | `GET /api/orders/cached-balance` | accountNo=`Customer-100` | Before 반복 시 메모리 누수 |
| S7 | `GET /api/orders/export-transactions` | (없음) | **Before는 OOM 위험!** |
| S8 | `POST /api/orders/validate-account` | count=`10000` | Before는 CPU 100% |
| S9 | `POST /api/orders/transfer-external` | accountNo=`Customer-100` | Before는 30초 대기 |

> 모든 API에 `optimized=false`(기본값, 병목) / `optimized=true`(최적화) 토글 가능

---

## 3. JMeter CLI 실행법

### JMeter 설치

1. https://jmeter.apache.org/download_jmeter.cgi 에서 **apache-jmeter-5.6.3.zip** 다운로드
2. 원하는 경로에 압축 해제 (예: `C:\apache-jmeter-5.6.3`)

### GUI 모드 (스크립트 확인/편집용)

```bash
C:\apache-jmeter-5.6.3\bin\jmeter.bat
```

GUI에서 `File → Open` → JMX 파일 선택하여 확인/편집 가능.
**실제 부하 테스트는 CLI(Non-GUI) 모드로 실행해야 한다** (GUI는 리소스 소모가 크기 때문).

### CLI 모드 (Non-GUI) — 실제 테스트 실행

```bash
# 기본 실행
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n -t <JMX파일> -l <결과파일> -e -o <리포트폴더>
```

#### 옵션 설명

| 옵션 | 설명 |
|------|------|
| `-n` | Non-GUI 모드 (CLI 실행) |
| `-t` | 테스트 계획 파일 (.jmx) 경로 |
| `-l` | 테스트 결과 로그 파일 (.jtl) 경로 |
| `-e` | 테스트 종료 후 HTML 리포트 생성 |
| `-o` | HTML 리포트 출력 폴더 (비어있어야 함) |
| `-J<변수>=<값>` | JMX 내 User Defined Variable 오버라이드 |

---

## 4. JMX 테스트 파일 구성

### 파일 목록

```
jmeter/
├── perf-demo-test-plan.jmx           ← S1~S5 기본 병목 테스트
├── perf-demo-advanced-test-plan.jmx  ← S6~S9 운영 장애 시뮬레이션
└── perf-demo-s7-oom-test.jmx         ← S7 OOM 전용 (서버 크래시 가능)
```

### perf-demo-test-plan.jmx (기본 병목 S1~S5)

| # | Thread Group | VUser | 시간 | 기본상태 |
|---|-------------|-------|------|----------|
| 1 | 단건 테스트 | 1 | 1회 | **enabled** |
| 2 | Baseline Before | 10 | 3분 | disabled |
| 3 | Baseline After | 10 | 3분 | disabled |
| 4 | Load Before | 30 | 5분 | disabled |
| 5 | Load After | 30 | 5분 | disabled |
| 6-1~6-4 | Stress (10→20→40→80) | 단계별 | 2분씩 | disabled |
| 7 | Endurance | 20 | 30분 | disabled |

### perf-demo-advanced-test-plan.jmx (운영 장애 S6~S9)

| # | Thread Group | VUser | 시간 | 기본상태 | 핵심 관찰 |
|---|-------------|-------|------|----------|-----------|
| 1 | 단건 테스트 | 1 | 1회 | **enabled** | 스크립트 검증 |
| 2 | Baseline Before | 10 | 3분 | disabled | S6/S8/S9 병목 기준선 |
| 3 | Baseline After | 10 | 3분 | disabled | 최적화 기준선 |
| 4 | Load Before | 30 | 5분 | disabled | Active Service 폭증 |
| 5 | Load After | 30 | 5분 | disabled | 최적화 효과 |
| 6-1~6-4 | Stress (10→80) | 단계별 | 2분씩 | disabled | CPU Saturation |
| 7 | Endurance | 20 | 30분 | disabled | **메모리 누수 (S6)** |

### perf-demo-s7-oom-test.jmx (OOM 전용)

| # | Thread Group | VUser | 시간 | 기본상태 | 주의 |
|---|-------------|-------|------|----------|------|
| 1 | S7 단건 확인 | 1 | 1회 | **enabled** | Before+After 검증 |
| 2 | S7 Stress Before | 5 | 2분 | disabled | **OOM 유도! 서버 크래시 가능** |
| 3 | S7 Stress After | 20 | 5분 | disabled | 안정성 확인 |

### 공통 변수 (JMX 내 User Defined Variables)

| 변수 | 기본값 | 설명 | CLI 오버라이드 |
|------|--------|------|---------------|
| `HOST` | localhost | 서버 주소 | `-JHOST=192.168.1.10` |
| `PORT` | 18081 | 서버 포트 | `-JPORT=18082` |
| `THINK_TIME` | 500 | 요청 간 대기(ms) | `-JTHINK_TIME=1000` |
| `CUST_MAX` | 5000 | 랜덤 고객번호 범위 | `-JCUST_MAX=3000` |

---

## 5. 실전 테스트 수행 절차

### Step 1: 서버 기동 확인

```bash
java -jar target/perf-demo-1.0.0.jar
# "Test data initialization completed" 로그 확인 후 진행
```

### Step 2: 단건 테스트 (스크립트 검증)

모든 JMX 파일은 단건 테스트가 enabled 상태이므로 바로 실행 가능.

```bash
# S1~S5 기본 병목 단건 테스트
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-test-plan.jmx ^
  -l jmeter/result-basic-single.jtl

# S6~S9 운영 장애 단건 테스트
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-advanced-test-plan.jmx ^
  -l jmeter/result-advanced-single.jtl

# S7 OOM 단건 테스트
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-s7-oom-test.jmx ^
  -l jmeter/result-s7-single.jtl
```

결과 확인: 모든 샘플러가 `OK`이고 Assertion 실패가 0건이면 정상.

### Step 3: Baseline 테스트 (기준선 측정)

JMeter GUI에서 원하는 Thread Group만 enable하거나, CLI에서 실행.

> **주의**: CLI 모드에서는 JMX 파일 내 enabled/disabled 상태가 그대로 적용된다.
> 특정 Thread Group만 실행하려면 GUI에서 미리 enable/disable 설정 후 저장해야 한다.

```bash
# S1~S5 Baseline Before (병목 버전 기준선)
# → GUI에서 "2. Baseline Before" 만 enable 후 저장
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-test-plan.jmx ^
  -l jmeter/result-baseline-before.jtl ^
  -e -o jmeter/report-baseline-before

# S1~S5 Baseline After (최적화 버전 기준선)
# → GUI에서 "3. Baseline After" 만 enable 후 저장
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-test-plan.jmx ^
  -l jmeter/result-baseline-after.jtl ^
  -e -o jmeter/report-baseline-after
```

### Step 4: Load 테스트 (부하 테스트)

```bash
# S6~S9 Load Before (30 VUser, 5분)
# → GUI에서 "4. Load Before" 만 enable 후 저장
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-advanced-test-plan.jmx ^
  -l jmeter/result-adv-load-before.jtl ^
  -e -o jmeter/report-adv-load-before

# S6~S9 Load After (30 VUser, 5분)
# → GUI에서 "5. Load After" 만 enable 후 저장
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-advanced-test-plan.jmx ^
  -l jmeter/result-adv-load-after.jtl ^
  -e -o jmeter/report-adv-load-after
```

### Step 5: Stress 테스트 (한계점 탐색)

단계별로 순차 실행. 각 단계 사이에 서버 상태 안정화를 위해 1~2분 대기.

```bash
# GUI에서 "6-1. Stress 10 VUser" 만 enable 후 저장 → 실행
# 이후 6-2 → 6-3 → 6-4 순서로 반복
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-advanced-test-plan.jmx ^
  -l jmeter/result-stress-10.jtl ^
  -e -o jmeter/report-stress-10
```

### Step 6: Endurance 테스트 (장시간 안정성)

```bash
# GUI에서 "7. Endurance" 만 enable 후 저장
# 30분 동안 실행 — Scouter HeapUsed 그래프 관찰 필수
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-advanced-test-plan.jmx ^
  -l jmeter/result-endurance.jtl ^
  -e -o jmeter/report-endurance
```

### Step 7: S7 OOM 테스트 (서버 크래시 유도)

```bash
# ⚠ 서버 크래시 가능! 실행 전 다른 작업 저장
# GUI에서 "2. S7 Stress Before" enable 후 저장
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-s7-oom-test.jmx ^
  -l jmeter/result-s7-oom.jtl ^
  -e -o jmeter/report-s7-oom
```

### 변수 오버라이드 예시

다른 서버나 포트를 대상으로 테스트할 때:

```bash
# 포트 18082 서버 대상, Think Time 1초
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-advanced-test-plan.jmx ^
  -l jmeter/result-port2.jtl ^
  -JHOST=localhost -JPORT=18082 -JTHINK_TIME=1000
```

### HTML 리포트 보기

`-e -o` 옵션으로 생성된 리포트 폴더 내 `index.html`을 브라우저에서 열기:

```bash
start jmeter\report-baseline-before\index.html
```

리포트에 포함되는 차트:
- Response Times Over Time
- Transactions Per Second (TPS)
- Response Time Percentiles
- Error Rate

### 결과 파일(.jtl) 초기화

이전 결과가 남아있으면 JMeter가 오류를 발생시킨다. 재실행 전 삭제 필요:

```bash
del jmeter\result-*.jtl
rmdir /s /q jmeter\report-*
```

---

## 빠른 참조: 전체 테스트 흐름

```
1. 서버 기동
   └→ java -jar target/perf-demo-1.0.0.jar
   └→ "Test data initialization completed" 대기 (23초)

2. Swagger UI 확인
   └→ http://localhost:18081/swagger-ui.html
   └→ 각 API Try it out → 정상 응답 확인

3. JMeter 단건 테스트 (스크립트 검증)
   └→ jmeter.bat -n -t jmeter/perf-demo-test-plan.jmx -l result.jtl
   └→ 모든 샘플러 OK 확인

4. Baseline Before → After (기준선 비교)
5. Load Before → After (부하 비교)
6. Stress 단계별 (한계점 탐색)
7. Endurance 30분 (메모리 누수, 안정성)
8. S7 OOM 전용 (서버 크래시 관찰)

매 단계마다 Scouter Paper 동시 관찰 권장!
```

---

## 6. Swagger ↔ JMeter 연동 워크플로우

### 두 도구의 역할

```
┌──────────────────────────────────────────────────────────────────────┐
│                        성능 테스트 워크플로우                           │
│                                                                      │
│   ① Swagger UI (탐색/검증)          ② JMeter (부하 테스트)            │
│   ┌─────────────────────┐          ┌─────────────────────────┐      │
│   │ • API 스펙 확인       │          │ • N명 동시 호출 시뮬레이션 │      │
│   │ • 파라미터/응답 확인   │   ──→   │ • Before/After 응답시간 비교│      │
│   │ • 단건 호출 테스트     │          │ • TPS, 에러율 측정        │      │
│   │ • 정상 응답 형태 파악  │          │ • HTML 리포트 자동 생성   │      │
│   └─────────────────────┘          └─────────────────────────┘      │
│          ↑                                    │                      │
│          │              ③ Scouter APM         │                      │
│          │         ┌──────────────────┐       │                      │
│          └──────── │ XLog, HeapUsed,  │ ←─────┘                      │
│           디버깅    │ ProcCpu 실시간   │  모니터링                     │
│                    └──────────────────┘                               │
└──────────────────────────────────────────────────────────────────────┘
```

| 단계 | 도구 | 목적 |
|------|------|------|
| **1단계** | Swagger UI | API 스펙 파악, 파라미터 확인, 단건 호출로 정상 동작 검증 |
| **2단계** | JMeter CLI | 같은 API를 N명이 동시 호출, 성능 지표 수집 |
| **3단계** | Scouter APM | JMeter 부하 중 서버 내부 상태 실시간 관찰 |

### Swagger → JMeter 파라미터 대응표

Swagger에서 확인한 파라미터가 JMeter JMX에서 어떻게 매핑되는지:

#### S1~S5 기본 병목

| # | Swagger 화면 | JMeter 샘플러 (JMX) | 비고 |
|---|-------------|-------------------|------|
| S1 | `GET /api/orders/{orderId}` | |
| | orderId = `ORD00000001` | path: `/api/orders/ORD00000001` | JMX에서는 랜덤 `ORD${orderNum}` |
| | optimized = `false` | param 없음 (기본값) | |
| | optimized = `true` | param: `optimized=true` | |
| S2 | `GET /api/orders/search` | |
| | keyword = `Customer-100` | param: `keyword=Customer-100` | JMX에서는 `Customer-${custNum}` |
| | optimized = `true` | param: `optimized=true` | |
| S3 | `GET /api/orders/detail-list` | |
| | limit = `50` | param: `limit=50` | |
| | optimized = `true` | param: `optimized=true` | |
| S4 | `POST /api/orders/process` | |
| | count = `1000` | param: `count=1000` | |
| | optimized = `true` | param: `optimized=true` | |
| S5 | `GET /api/orders/report` | |
| | startDate = `2025-06-01` | param: `startDate=2025-06-01` | |
| | endDate = `2025-06-30` | param: `endDate=2025-06-30` | |
| | optimized = `true` | param: `optimized=true` | |

#### S6~S9 운영 장애 시뮬레이션

| # | Swagger 화면 | JMeter 샘플러 (JMX) | 비고 |
|---|-------------|-------------------|------|
| S6 | `GET /api/orders/cached-balance` | |
| | accountNo = `Customer-100` | param: `accountNo=Customer-100` | JMX에서는 `Customer-${custNum}` |
| S7 | `GET /api/orders/export-transactions` | |
| | (파라미터 없음) | path만 호출 | Before는 별도 JMX |
| S8 | `POST /api/orders/validate-account` | |
| | count = `10000` | param: `count=10000` | |
| S9 | `POST /api/orders/transfer-external` | |
| | accountNo = `Customer-100` | param: `accountNo=Customer-100` | |

### 실전 연동 시나리오: S1 단건조회 예시

#### Phase 1: Swagger에서 API 파악 & 검증

1. http://localhost:18081/swagger-ui.html 접속
2. `GET /api/orders/{orderId}` 클릭 → **Try it out**
3. 병목 버전 테스트:
   ```
   orderId:   ORD00000001
   optimized: false
   ```
4. **Execute** → 응답 확인:
   ```json
   {
     "success": true,
     "data": {
       "orderId": "ORD00000001",
       "customerId": "CUST00042",
       "customerName": "Customer-42",
       "orderStatus": "CONFIRMED",
       "totalAmount": 285000,
       "orderDate": "2025-08-15"
     }
   }
   ```
   - 응답시간: **~3초** (Response headers의 시간 또는 브라우저 네트워크 탭에서 확인)
   - `"success": true` → 정상 동작 확인

5. 최적화 버전 테스트: `optimized: true`로 변경 → **Execute**
   - 응답시간: **~50ms**
   - 같은 데이터, 훨씬 빠름

**이 단계에서 파악한 것:**
- 엔드포인트: `/api/orders/{orderId}`
- 필수 파라미터: `orderId` (경로), `optimized` (쿼리)
- 정상 응답: `"success": true`
- Before ~3초, After ~50ms

#### Phase 2: JMeter로 부하 테스트

Swagger에서 확인한 것과 동일한 API를 JMeter가 30명이 동시에 반복 호출:

```bash
# Baseline Before (10명, 3분) — 병목 버전
C:\apache-jmeter-5.6.3\bin\jmeter.bat -n ^
  -t jmeter/perf-demo-test-plan.jmx ^
  -l jmeter/result-s1-before.jtl ^
  -e -o jmeter/report-s1-before
```

JMX 내부에서 일어나는 일:
```
Swagger에서 수동으로 한 것           JMeter가 자동으로 하는 것
─────────────────────────           ──────────────────────────
orderId: ORD00000001         →      ORD${__Random(1,100000)} (매번 랜덤)
optimized: false             →      파라미터 없음 (Before 그룹)
Execute 1회 클릭              →      10명 × 3분간 반복 호출
브라우저에서 응답시간 확인      →      Summary Report 자동 집계
```

#### Phase 3: Scouter에서 실시간 관찰

JMeter 부하 실행 중 Scouter Paper(http://localhost:6180) 에서:

| Scouter 차트 | Before (병목) | After (최적화) |
|-------------|-------------|---------------|
| XLog | 3초 대에 점 밀집 | 50ms 대에 점 밀집 |
| TPS | ~3 TPS (3초 대기 때문) | ~200 TPS |
| Active Service | 10 (모든 스레드 대기) | 0~1 |
| Elapsed Time | 평균 3,000ms | 평균 50ms |

#### Phase 4: JMeter 리포트 비교

```bash
# Before 리포트
start jmeter\report-s1-before\index.html

# After 리포트
start jmeter\report-s1-after\index.html
```

비교 포인트:
| 지표 | Before | After |
|------|--------|-------|
| Average | ~3,000ms | ~50ms |
| 90th pct | ~3,100ms | ~80ms |
| Throughput | ~3 req/s | ~200 req/s |
| Error % | 0% | 0% |

### 시나리오별 연동 팁

#### S6 메모리 누수 (Endurance)

```
Swagger 확인 사항:
  → accountNo에 다양한 값 넣어보기 (Customer-1, Customer-2, ...)
  → 각 호출마다 응답은 정상이지만, 서버 내부에서 캐시가 쌓이는 중

JMeter 연동:
  → Endurance 30분 실행하면서 Scouter HeapUsed 그래프 관찰
  → Before: 우상향 곡선 (메모리 누수)
  → After: 수평 유지 (LRU 캐시)
```

#### S8 CPU 과부하 (Stress)

```
Swagger 확인 사항:
  → count=10000, optimized=false → 브라우저가 한참 대기 (CPU 점유)
  → count=10000, optimized=true → 즉시 응답

JMeter 연동:
  → Stress 단계별(10→20→40→80 VUser) 실행
  → Scouter ProcCpu 차트에서 포화점 확인
  → Before: 10 VUser에서 이미 CPU 100%
  → After: 80 VUser에서도 CPU 20% 미만
```

#### S9 외부 API 타임아웃 (Load)

```
Swagger 확인 사항:
  → optimized=false → 30초 후 응답 (브라우저 로딩 오래 걸림)
  → optimized=true → 3초 후 폴백 응답 (orderStatus: "PENDING_RETRY")

JMeter 연동:
  → Load 30 VUser 실행
  → Before: 모든 스레드가 30초 블로킹 → Active Service 30 유지
  → After: 3초 후 빠르게 해제 → Active Service 1~3 유지
```

### JMeter 실패 시 Swagger로 디버깅

JMeter에서 Assertion 실패가 발생하면:

1. **JMeter GUI**에서 `View Results Tree` 활성화 → 실패한 요청의 Response Body 확인
2. **같은 파라미터로 Swagger에서 직접 호출** → 서버 응답 직접 확인
3. 일반적인 실패 원인:

| 증상 | Swagger 확인 | 원인 |
|------|-------------|------|
| `"success":false` | 같은 파라미터로 호출 → 에러 메시지 확인 | 파라미터 오류 또는 데이터 미존재 |
| Connection refused | Swagger 접속 불가 | 서버 미기동 또는 크래시 |
| 500 Internal Server Error | Swagger에서도 500 | S7 OOM 등 서버 내부 오류 |
| Timeout | Swagger에서도 로딩 오래 걸림 | S9 Before (30초 블로킹) 정상 동작 |
| 200이지만 Assertion 실패 | 응답에 `"success":true` 없음 | API 스펙 변경 확인 |

### 전체 연동 흐름 요약

```
┌─────────────────────────────────────────────────────────────────┐
│                    시나리오 1건 분석 흐름                          │
│                                                                 │
│  ① Swagger: API 스펙 파악                                       │
│     └→ 엔드포인트, 파라미터, 응답 형태 확인                       │
│     └→ Before/After 각 1회 수동 호출 → 정상 동작 검증             │
│                                                                 │
│  ② JMeter 단건: 스크립트 검증                                    │
│     └→ jmeter.bat -n -t <jmx> -l result.jtl                    │
│     └→ Swagger와 같은 결과가 나오는지 확인                        │
│                                                                 │
│  ③ JMeter Baseline: Before/After 기준선 (10 VUser, 3분)         │
│     └→ 평균 응답시간, TPS 기록                                   │
│     └→ Scouter XLog으로 분포 확인                                │
│                                                                 │
│  ④ JMeter Load: 부하 테스트 (30 VUser, 5분)                     │
│     └→ Before: 병목 심화 → After: 최적화 효과 확인                │
│     └→ Scouter에서 HeapUsed, ProcCpu, Active Service 관찰       │
│                                                                 │
│  ⑤ HTML 리포트: Before vs After 비교                             │
│     └→ 응답시간, TPS, 에러율 비교표 작성                          │
│                                                                 │
│  ⑥ 실패 시: Swagger로 돌아가서 단건 디버깅                        │
└─────────────────────────────────────────────────────────────────┘
```
