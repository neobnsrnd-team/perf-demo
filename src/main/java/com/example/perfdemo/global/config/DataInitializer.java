package com.example.perfdemo.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random(42);

    private static final String[] PRODUCT_TYPES = {
            "Laptop", "Keyboard", "Mouse", "Monitor", "Headset",
            "Webcam", "USB Hub", "Charger", "Cable", "Speaker"
    };
    private static final String[] CATEGORIES = {
            "Electronics", "Accessories", "Peripherals", "Audio", "Network"
    };
    private static final String[] STATUSES = {
            "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
    };

    private static final int PRODUCT_COUNT = 1_000;
    private static final int ORDER_COUNT = 100_000;
    private static final int DETAIL_COUNT = 500_000;
    private static final int BATCH_SIZE = 5_000;

    @Override
    public void run(String... args) {
        long start = System.currentTimeMillis();
        log.info("=== Test data initialization started ===");

        insertProducts();
        insertOrders();
        insertOrderDetails();

        long elapsed = System.currentTimeMillis() - start;
        log.info("=== Test data initialization completed in {}ms ===", elapsed);
        log.info("  PERF_PRODUCT: {} rows", PRODUCT_COUNT);
        log.info("  PERF_ORDER: {} rows", ORDER_COUNT);
        log.info("  PERF_ORDER_DETAIL: {} rows", DETAIL_COUNT);
    }

    private void insertProducts() {
        String sql = "INSERT INTO PERF_PRODUCT (PRODUCT_ID, PRODUCT_NAME, CATEGORY, PRICE) VALUES (?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            String productId = String.format("PRD%05d", i);
            String productName = PRODUCT_TYPES[i % PRODUCT_TYPES.length] + " Model-" + String.format("%04d", i);
            String category = CATEGORIES[i % CATEGORIES.length];
            long price = 5000 + random.nextInt(495000);

            batch.add(new Object[]{productId, productName, category, price});
            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
        log.info("  PERF_PRODUCT: {} rows inserted", PRODUCT_COUNT);
    }

    private void insertOrders() {
        String sql = "INSERT INTO PERF_ORDER (ORDER_ID, CUSTOMER_ID, CUSTOMER_NAME, ORDER_STATUS, TOTAL_AMOUNT, ORDER_DATE, CREATED_AT) VALUES (?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        LocalDate baseDate = LocalDate.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (int i = 1; i <= ORDER_COUNT; i++) {
            String orderId = String.format("ORD%08d", i);
            int custNum = random.nextInt(5000) + 1;
            String customerId = String.format("CUST%05d", custNum);
            String customerName = "Customer-" + custNum;
            String status = STATUSES[random.nextInt(STATUSES.length)];
            long totalAmount = 10000 + random.nextInt(4990000);
            LocalDate orderDate = baseDate.minusDays(random.nextInt(365) + 1);
            String orderDateStr = orderDate.format(dateFmt);
            String createdAt = orderDate.atTime(random.nextInt(24), random.nextInt(60), random.nextInt(60))
                    .format(dtFmt);

            batch.add(new Object[]{orderId, customerId, customerName, status, totalAmount, orderDateStr, createdAt});
            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
        log.info("  PERF_ORDER: {} rows inserted", ORDER_COUNT);
    }

    private void insertOrderDetails() {
        String sql = "INSERT INTO PERF_ORDER_DETAIL (DETAIL_ID, ORDER_ID, PRODUCT_ID, PRODUCT_NAME, QUANTITY, UNIT_PRICE, LINE_AMOUNT) VALUES (?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

        // 미리 상품 정보 캐시
        List<Object[]> products = jdbcTemplate.query(
                "SELECT PRODUCT_ID, PRODUCT_NAME, PRICE FROM PERF_PRODUCT",
                (rs, rowNum) -> new Object[]{
                        rs.getString("PRODUCT_ID"),
                        rs.getString("PRODUCT_NAME"),
                        rs.getLong("PRICE")
                }
        );

        for (int i = 1; i <= DETAIL_COUNT; i++) {
            String detailId = String.format("DTL%08d", i);
            String orderId = String.format("ORD%08d", random.nextInt(ORDER_COUNT) + 1);

            Object[] product = products.get(random.nextInt(products.size()));
            String productId = (String) product[0];
            String productName = (String) product[1];
            long unitPrice = (long) product[2];

            int quantity = random.nextInt(10) + 1;
            long lineAmount = quantity * unitPrice;

            batch.add(new Object[]{detailId, orderId, productId, productName, quantity, unitPrice, lineAmount});
            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
        log.info("  PERF_ORDER_DETAIL: {} rows inserted", DETAIL_COUNT);
    }
}
