package com.mm.backend.repository;

import com.mm.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 특정 month에 해당하는 모든 소비 내역 조회
    List<Transaction> findByMonth(String month);
}
