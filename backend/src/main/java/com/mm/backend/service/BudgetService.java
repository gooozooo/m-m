package com.mm.backend.service;

import com.mm.backend.entity.Budget;
import com.mm.backend.entity.Transaction;
import com.mm.backend.repository.BudgetRepository;
import com.mm.backend.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    // 1) 예산 설정
    public Budget setBudget(String month, Integer amount) {
        Budget budget = budgetRepository.findByMonth(month)
                .orElseGet(() -> Budget.builder().month(month).build());

        budget.setAmount(amount);

        return budgetRepository.save(budget);
    }

    // 2) 예산 상태 조회
    public BudgetStatus getBudgetStatus(String month) {
        Budget budget = budgetRepository.findByMonth(month)
                .orElseThrow(() -> new IllegalArgumentException("Budget not set for month: " + month));

        List<Transaction> transactions = transactionRepository.findByMonth(month);

        int spent = transactions.stream()
                .mapToInt(Transaction::getAmount)
                .sum();

        int remaining = budget.getAmount() - spent;
        double progress = budget.getAmount() == 0
                ? 0.0
                : (double) spent / budget.getAmount();

        return new BudgetStatus(
                month,
                budget.getAmount(),
                spent,
                remaining,
                progress
        );
    }

    @Getter
    @AllArgsConstructor
    public static class BudgetStatus {
        private String month;
        private Integer budget;     // 예산
        private Integer spent;      // 사용한 금액
        private Integer remaining;  // 남은 금액
        private Double progress;    // 사용 비율 (0.0 ~ 1.0)
    }
}
