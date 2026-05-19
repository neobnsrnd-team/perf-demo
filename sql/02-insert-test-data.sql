-- ============================================================
-- perf-demo: 테스트 데이터 생성 스크립트
-- 선행: 01-create-tables.sql 실행 완료
-- 소요시간: 약 3~5분 (Oracle 19c 기준)
-- ============================================================

-- ============================================================
-- 1. PERF_PRODUCT: 1,000건
-- ============================================================
INSERT INTO PERF_PRODUCT (PRODUCT_ID, PRODUCT_NAME, CATEGORY, PRICE)
SELECT
    'PRD' || LPAD(LEVEL, 5, '0'),
    CASE MOD(LEVEL, 10)
        WHEN 0 THEN 'Laptop'
        WHEN 1 THEN 'Keyboard'
        WHEN 2 THEN 'Mouse'
        WHEN 3 THEN 'Monitor'
        WHEN 4 THEN 'Headset'
        WHEN 5 THEN 'Webcam'
        WHEN 6 THEN 'USB Hub'
        WHEN 7 THEN 'Charger'
        WHEN 8 THEN 'Cable'
        WHEN 9 THEN 'Speaker'
    END || ' Model-' || LPAD(LEVEL, 4, '0'),
    CASE MOD(LEVEL, 5)
        WHEN 0 THEN 'Electronics'
        WHEN 1 THEN 'Accessories'
        WHEN 2 THEN 'Peripherals'
        WHEN 3 THEN 'Audio'
        WHEN 4 THEN 'Network'
    END,
    TRUNC(DBMS_RANDOM.VALUE(5000, 500000))
FROM DUAL
CONNECT BY LEVEL <= 1000;

COMMIT;

-- ============================================================
-- 2. PERF_ORDER: 100,000건 (10만건, 1만건씩 10회 배치)
-- ============================================================
DECLARE
    v_batch_size  NUMBER := 10000;
    v_total       NUMBER := 100000;
    v_offset      NUMBER;
    v_statuses    SYS.ODCIVARCHAR2LIST := SYS.ODCIVARCHAR2LIST(
        'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'
    );
BEGIN
    FOR batch IN 0 .. (v_total / v_batch_size - 1) LOOP
        v_offset := batch * v_batch_size;

        INSERT INTO PERF_ORDER (ORDER_ID, CUSTOMER_ID, CUSTOMER_NAME, ORDER_STATUS, TOTAL_AMOUNT, ORDER_DATE, CREATED_AT)
        SELECT
            'ORD' || LPAD(v_offset + LEVEL, 8, '0'),
            'CUST' || LPAD(TRUNC(DBMS_RANDOM.VALUE(1, 5001)), 5, '0'),
            'Customer-' || TRUNC(DBMS_RANDOM.VALUE(1, 5001)),
            CASE TRUNC(DBMS_RANDOM.VALUE(1, 6))
                WHEN 1 THEN 'PENDING'
                WHEN 2 THEN 'CONFIRMED'
                WHEN 3 THEN 'SHIPPED'
                WHEN 4 THEN 'DELIVERED'
                WHEN 5 THEN 'CANCELLED'
            END,
            TRUNC(DBMS_RANDOM.VALUE(10000, 5000000)),
            TO_CHAR(SYSDATE - TRUNC(DBMS_RANDOM.VALUE(1, 365)), 'YYYY-MM-DD'),
            TO_CHAR(SYSDATE - TRUNC(DBMS_RANDOM.VALUE(1, 365)), 'YYYY-MM-DD HH24:MI:SS')
        FROM DUAL
        CONNECT BY LEVEL <= v_batch_size;

        COMMIT;
        DBMS_OUTPUT.PUT_LINE('PERF_ORDER batch ' || (batch + 1) || ' done (' || (v_offset + v_batch_size) || ' rows)');
    END LOOP;
END;
/

-- ============================================================
-- 3. PERF_ORDER_DETAIL: 500,000건 (주문당 평균 5건, 1만건씩 50회)
-- ============================================================
DECLARE
    v_batch_size  NUMBER := 10000;
    v_total       NUMBER := 500000;
    v_offset      NUMBER;
    v_prod_num    NUMBER;
    v_qty         NUMBER;
BEGIN
    FOR batch IN 0 .. (v_total / v_batch_size - 1) LOOP
        v_offset := batch * v_batch_size;

        INSERT INTO PERF_ORDER_DETAIL (DETAIL_ID, ORDER_ID, PRODUCT_ID, PRODUCT_NAME, QUANTITY, UNIT_PRICE, LINE_AMOUNT)
        SELECT
            dtl.DETAIL_ID,
            dtl.ORDER_ID,
            dtl.PRODUCT_ID,
            p.PRODUCT_NAME,
            dtl.QUANTITY,
            p.PRICE,
            dtl.QUANTITY * p.PRICE
        FROM (
            SELECT
                'DTL' || LPAD(v_offset + LEVEL, 8, '0') AS DETAIL_ID,
                'ORD' || LPAD(TRUNC(DBMS_RANDOM.VALUE(1, 100001)), 8, '0') AS ORDER_ID,
                'PRD' || LPAD(MOD(v_offset + LEVEL, 1000) + 1, 5, '0') AS PRODUCT_ID,
                TRUNC(DBMS_RANDOM.VALUE(1, 11)) AS QUANTITY
            FROM DUAL
            CONNECT BY LEVEL <= v_batch_size
        ) dtl
        JOIN PERF_PRODUCT p ON p.PRODUCT_ID = dtl.PRODUCT_ID;

        COMMIT;
        DBMS_OUTPUT.PUT_LINE('PERF_ORDER_DETAIL batch ' || (batch + 1) || ' done (' || (v_offset + v_batch_size) || ' rows)');
    END LOOP;
END;
/

-- ============================================================
-- 검증
-- ============================================================
SELECT 'PERF_PRODUCT' AS TABLE_NAME, COUNT(*) AS ROW_COUNT FROM PERF_PRODUCT
UNION ALL
SELECT 'PERF_ORDER', COUNT(*) FROM PERF_ORDER
UNION ALL
SELECT 'PERF_ORDER_DETAIL', COUNT(*) FROM PERF_ORDER_DETAIL;
