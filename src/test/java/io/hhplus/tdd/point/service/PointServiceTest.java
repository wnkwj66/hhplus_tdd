package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointServiceTest {

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
    void 유저포인트_조회_성공_테스트() {

        long userId = 1L;

        // given
        UserPoint mockUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis()); // 모킹된 UserPoint
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint); // Mock 설정

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(1000L, result.point()); // UserPoint의 point 필드 확인
        assertEquals(userId, result.id()); // UserPoint의 id 필드 확인
        verify(userPointTable).selectById(userId); // selectById가 호출되었는지 검증
    }

    @Test
    @DisplayName("유저가 존재하지 않는 경우")
    void 유저포인트_조회_실패_테스트() {
        // given
        Long validUserId = null;

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.validateUser(validUserId);
        });

        assertEquals("사용자가 존재하지 않습니다.", exception.getMessage());
    }


    @Test
    void 포인트_충전_성공_테스트() {
        // given
        long userId = 1L;
        UserPoint existingUserPoint = new UserPoint(userId, 500L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(userId, 1500L)).thenReturn(new UserPoint(userId, 1500L, System.currentTimeMillis()));

        // when
        UserPoint updatedUserPoint = pointService.pointCharge(userId, 1000L);

        // then
        assertNotNull(updatedUserPoint);
        assertEquals(1500L, updatedUserPoint.point());
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(userId, 1500L);
        verify(pointHistoryTable).insert(eq(userId), eq(1500L), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    void 포인트_충전_실패_테스트(){
        // given
        long userId = 1L;
        long negativeAmount = -1000L;

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.pointCharge(userId, negativeAmount);
        });
        assertEquals("충전 금액이 0보다 적습니다.", exception.getMessage());
    }

    @Test
    void 포인트_사용_성공_테스트() {
        // given
        long userId = 1L;
        long useAmount = 500L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(userId, 500L)).thenReturn(new UserPoint(userId, 500L, System.currentTimeMillis()));

        // when
        UserPoint updatedUserPoint = pointService.pointUse(userId, useAmount);

        // then
        assertNotNull(updatedUserPoint);
        assertEquals(500L, updatedUserPoint.point());
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(userId, 500L);
        verify(pointHistoryTable).insert(eq(userId), eq(500L), eq(TransactionType.USE), anyLong());
    }

    @Test
    void 포인트_부족_테스트() {
        // given
        long userId = 1L;
        long useAmount = 500L;
        UserPoint existingUserPoint = new UserPoint(userId, 300L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(existingUserPoint);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.pointUse(userId, useAmount);
        });
        assertEquals("포인트가 부족합니다.", exception.getMessage());
    }
}