package com.mm.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mm.backend.entity.Transaction;
import com.mm.backend.service.TransactionService;
import com.mm.backend.service.TransactionService.TransactionRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    // 소비 내역 1건 추가
    @PostMapping("/add")
    public Transaction addTransaction(@RequestBody TransactionRequest req) {
        return transactionService.addTransaction(req);
    }

    // 특정 month의 소비 내역 목록 조회
    @GetMapping("/list")
    public List<Transaction> getList(@RequestParam String month) {
        return transactionService.getTransactionsByMonth(month);
    }
}

