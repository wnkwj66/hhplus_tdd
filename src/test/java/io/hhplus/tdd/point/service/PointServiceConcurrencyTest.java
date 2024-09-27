package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class PointServiceConcurrencyTest {
    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService; // 실제 테스트할 서비스

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Mockito 초기화
    }

    @Test
    void 동시에_충전과_사용_요청시_성공() throws Exception {
        long userId = 1L;

        final int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i -> {
            executorService.execute(() -> {
                if (i % 2 == 0) {
                    pointService.pointCharge(userId, 10);
                } else {
                    pointService.pointUse(userId, 5);
                }
                latch.countDown();
            });
        });

        latch.await();

        long expectedPoint = 1250;
        UserPoint point = pointService.getUserPoint(userId);

        assertEquals(expectedPoint, point.point());
    }


}
