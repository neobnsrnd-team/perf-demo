# 성능테스트 실습 가이드

> JMeter + Scouter를 활용한 성능테스트 실습

---

## 1. 성능테스트란?

- **기능 테스트:** 1명이 써도 동작하는가?
- **성능 테스트:** N명이 동시에 써도 감당되는가?
- **목적:** 운영 투입 전에 시스템 적합성 검증 및 문제 사전 발견

| 유형 | 목적 | 방법 | 판정 기준 |
|------|------|------|----------|
| Peak | 목표 부하에서 정상 동작 | 목표 TPS로 부하 | CPU 70% 이하, 에러율 0.1% 미만 |
| Stress | 시스템 한계점 확인 | 부하를 단계적 증가 | CPU 90% 도달 시점 TPS |
| Endurance | 장시간 안정성 | Peak의 70~80%로 4~8시간 | 메모리 누수 없음, 응답 일정 |

**핵심 지표:**

| 지표 | 의미 | 기준 |
|------|------|------|
| TPS | 초당 처리 건수 | 목표 TPS 달성 |
| 응답시간 | 요청~응답 (ms) | 3초 이내 |
| 에러율 | 에러 비율 (%) | 0.1% 미만 |
| CPU | 서버 CPU (%) | Peak 70%, Stress 90% |

**TPS 계산:** `TPS = 스레드 수 / (Think Time + 평균 응답시간)`
- 예: 50스레드, Think 1초, 응답 0.5초 → TPS = 50 / 1.5 = **33**
- 이론값 ≒ 실측값 → 서버 여유 / 실측값이 낮으면 → 병목

---

## 2. 도구 소개

**JMeter** — 부하 발생 + 성능 측정 (Apache, 오픈소스)
- N명 동시 접속을 시뮬레이션하여 TPS, 응답시간, 에러율 측정
- 테스트 결과를 JTL(CSV) / HTML 리포트로 출력

**Scouter** — 실시간 모니터링 (LG CNS, 오픈소스 Java APM)
- JMeter가 부하를 거는 동안 서버 내부를 실시간 관찰
- XLog(트랜잭션별 응답시간), CPU, Heap, GC
- 느린 트랜잭션 클릭 → 메서드/SQL 단위 병목 추적 (프로파일)

> JMeter = **"성능이 얼마나 나오는지"** / Scouter = **"왜 느린지"**

---

## 3. 실습 준비

### 환경 (내 PC 1대, localhost)

```
Scouter Server :6100        데이터 수집/저장
Paper          :6180        웹 대시보드 (브라우저 접속)
Host Agent                  CPU/Memory 수집
perf-demo-1    :18081 ┐
perf-demo-2    :18082 ├ Spring Boot + Scouter Agent
perf-demo-3    :18083 ┘
JMeter                      부하 발생
```

### 설치

1. **Java 8** (Scouter용) + **Java 17** (perf-demo용)
2. **Scouter** 다운로드 후 압축 해제
   - https://github.com/scouter-project/scouter/releases/tag/v2.21.2
3. **JMeter** 설치
4. **perf-demo** clone 후 빌드
   ```
   git clone https://github.com/neobnsrnd-team/perf-demo.git
   cd perf-demo
   mvn clean package
   ```
5. `scouter-extweb\` → Scouter webapp의 `extweb\`에 복사 (커스텀 대시보드)

### 경로 수정 (본인 PC에 맞게)

```
# start-scouter-server.bat / host.bat / webapp.bat (3개 파일)
set JAVA_HOME=본인의 Java 8 경로
set SCOUTER_HOME=본인의 Scouter 설치 경로

# start-all.bat
set JAVA_HOME=본인의 Java 17 경로
set SCOUTER_AGENT=본인의 scouter.agent.jar 경로
```

### Scouter Agent 설정 (conf)

실습 conf는 `conf\scouter-demo1.conf`에 이미 포함. 실제 프로젝트 적용 시 아래 3개만 수정:

```properties
net_collector_ip=127.0.0.1                     # Scouter Server IP
obj_name=perf-demo-1                           # 인스턴스 식별명
hook_method_patterns=com.example.perfdemo.*.*   # 프로파일 대상 패키지
```

---

## 4. 실습 실행

### ① 기동

`start-all.bat` 더블클릭 → Scouter(Server/Paper/Host) + perf-demo 3개 자동 기동

### ② 동작 확인

1. Paper 접속: http://localhost:6180
2. Swagger 접속: http://localhost:18081/swagger-ui.html
3. Swagger에서 API 1건 호출 → Paper XLog에 점이 찍히면 연동 성공

점이 안 찍히면: Scouter Server 기동 여부, conf의 `net_collector_ip`, Agent 연결 확인

### ③ 부하 발생

```
jmeter\run-test.bat 1           S1~S5 기본 시나리오
jmeter\run-test.bat 1 18082     포트 지정 (perf-demo-2)
jmeter\run-scenario.bat 2       시나리오 선택 (Baseline Before)
```

---

## 5. 결과 분석

### JMeter 리포트 (성능 측정 결과)

테스트 완료 후 `jmeter\results\` 에 HTML 리포트가 자동 생성됨.

| 지표 | 확인 항목 |
|------|----------|
| TPS (Throughput) | 목표 TPS 달성 여부 |
| 평균/P95/P99 응답시간 | 3초 이내인지 |
| 에러율 | 0.1% 미만인지 |
| 응답시간 추이 | 시간이 지나도 일정한지 (Endurance) |

JTL 파일(CSV) → JMeter Gui에서 리스너 추가 후 분석 또는 `scouter-report-jtl2.html`로 상세 분석 가능

### Scouter 실시간 모니터링 (병목 추적)

Paper(http://localhost:6180) 에서 확인:

| 지표 | 용도 |
|------|------|
| XLog | 트랜잭션별 응답시간 분포 (점 그래프) |
| Active Service | 현재 처리 중인 요청 수 |
| TPS 카운터 | 초당 처리량 추이 |
| CPU / Heap | 서버 리소스 사용률 |


**프로파일** — 느린 점 클릭 → 트랜잭션 내부 추적:

```
0ms   UserController.search              2340ms
 2ms  UserService.searchUsers             2328ms
  40ms  SQL: SELECT * FROM FWK_USER ...    890ms
  975ms UserService.enrichUserData         1315ms
   980ms SQL: SELECT * FROM FWK_MENU ...   1310ms ← 병목!
```

→ 어떤 메서드/SQL이 오래 걸리는지 한눈에 파악. 로그만으로는 못 찾는 병목을 찾아준다.

---

## 참고

- **테스트 흐름:** 시나리오 작성 → 단건 확인 → 부하 → 모니터링 → 분석 → 병목 개선 → 재테스트
- **JMeter CLI 옵션:** `-n`(Non-GUI) `-t`(JMX) `-l`(결과) `-e -o`(HTML 리포트)
- 운영 환경 부하 테스트는 JMeter CLI 모드 필수 (GUI는 JMeter 자체가 병목)
