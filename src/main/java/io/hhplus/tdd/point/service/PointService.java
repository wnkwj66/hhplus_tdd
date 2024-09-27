package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    private final Lock lock = new ReentrantLock();

    /**
     * 유저 정보 조회
     */
    public UserPoint getUserPoint(long id) {
        validateUser(id);
        return userPointTable.selectById(id);
    }

    /**
     * 포인트 히스토리 조회
     */
    public List<PointHistory> getUserPointHistory(long id) {
        validateUser(id);
        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * 포인트 충전
     */
    public UserPoint pointCharge(long id, long amount) {
        lock.lock();
        try{
            validateUser(id);
            validateChargePoint(amount);
            UserPoint userInfo = userPointTable.selectById(id);
            long updateAmount = userInfo.point() + amount;

            userInfo = userPointTable.insertOrUpdate(id, updateAmount);
            pointHistoryTable.insert(id,updateAmount,TransactionType.CHARGE,System.currentTimeMillis());

            return userInfo;
        } finally {
            lock.unlock();
        }

    }

    /**
     * 포인트 사용
     */
    public UserPoint pointUse(long id, long amount) {
        lock.lock();
        try{
            validateUser(id);
            UserPoint userInfo = userPointTable.selectById(id);
            validateUsePoint(userInfo.point(), amount);
            long updateAmount = userInfo.point() - amount;

            userInfo = userPointTable.insertOrUpdate(id, updateAmount);
            pointHistoryTable.insert(id,updateAmount,TransactionType.USE,System.currentTimeMillis());

            return userInfo;
        } finally {
            lock.unlock();
        }
    }

    public void validateUser(Long id){
        if(id == null) {
            throw new IllegalArgumentException("사용자가 존재하지 않습니다.");
        }
    }


    private void validateChargePoint(long amount) {
        if(amount < 0) {
            throw new IllegalArgumentException("충전 금액이 0보다 적습니다.");
        }
    }

    private void validateUsePoint(long point, long amount) {
        if(point < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

    }
}
