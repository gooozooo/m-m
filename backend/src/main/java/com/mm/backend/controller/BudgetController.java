package com.mm.backend.controller;

import com.mm.backend.service.BudgetService;
import com.mm.backend.service.BudgetService.BudgetStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetService budgetService;

    // 1) 월 예산 설정
    @PostMapping("/set")
    public BudgetSetResponse setBudget(@RequestBody BudgetSetRequest request) {
        var budget = budgetService.setBudget(request.getMonth(), request.getAmount());
        return new BudgetSetResponse(budget.getMonth(), budget.getAmount());
    }

    // 2) 월 예산 상태 조회
    @GetMapping("/status")
    public BudgetStatus getStatus(@RequestParam String month) {
        return budgetService.getBudgetStatus(month);
    }

    // ==== 요청/응답 DTO ====

    @Getter
    @Setter
    public static class BudgetSetRequest {
        private String month;   // "2025-11"
        private Integer amount; // 예산 금액
    }

    @Getter
    public static class BudgetSetResponse {
        private final String month;
        private final Integer amount;

        public BudgetSetResponse(String month, Integer amount) {
            this.month = month;
            this.amount = amount;
        }
    }
}
