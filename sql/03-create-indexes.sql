-- ============================================================
-- perf-demo: 최적화용 인덱스 생성 스크립트
-- Scenario 2 수정 시 실행 (Before → After 전환)
-- ============================================================

-- Scenario 2: LIKE 'keyword%' 검색 최적화
-- CUSTOMER_NAME prefix 검색용 인덱스
CREATE INDEX IDX_PERF_ORDER_CUST_NAME ON PERF_ORDER(CUSTOMER_NAME);

-- ORDER_STATUS 필터링 + ORDER_DATE 정렬용
CREATE INDEX IDX_PERF_ORDER_STATUS_DATE ON PERF_ORDER(ORDER_STATUS, ORDER_DATE);

-- Scenario 3: N+1 → JOIN 최적화
-- ORDER_DETAIL에서 ORDER_ID로 조회 시 성능 향상
CREATE INDEX IDX_PERF_DETAIL_ORDER_ID ON PERF_ORDER_DETAIL(ORDER_ID);

-- Scenario 5: 집계 쿼리 최적화
-- ORDER_DATE 범위 검색 + ORDER_STATUS GROUP BY
CREATE INDEX IDX_PERF_ORDER_DATE ON PERF_ORDER(ORDER_DATE);

-- ============================================================
-- 인덱스 생성 확인
-- ============================================================
SELECT INDEX_NAME, TABLE_NAME, STATUS
FROM USER_INDEXES
WHERE TABLE_NAME LIKE 'PERF_%'
ORDER BY TABLE_NAME, INDEX_NAME;
