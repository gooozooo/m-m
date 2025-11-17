package com.mm.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mm.backend.entity.Transaction;
import com.mm.backend.repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    // 생성자 직접 작성 (Lombok 없이 확실하게)
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // 소비 저장
    public Transaction addTransaction(TransactionRequest req) {

        Transaction t = new Transaction();
        t.setMonth(req.getMonth());
        t.setDatetime(req.getDatetime() != null ? req.getDatetime() : LocalDateTime.now());
        t.setAmount(req.getAmount());
        t.setMerchant(req.getMerchant());
        t.setPaymentMethod(req.getPaymentMethod());
        t.setCategory(req.getCategory());
        t.setRawText(req.getRawText());

        return transactionRepository.save(t);
    }

    public List<Transaction> getTransactionsByMonth(String month) {
        return transactionRepository.findByMonth(month);
    }

    // -----------------------------
    //  ⚠ Lombok 안 쓰고 직접 getter/setter 작성!
    // -----------------------------
    public static class TransactionRequest {

        private String month;
        private LocalDateTime datetime;
        private Integer amount;
        private String merchant;
        private String paymentMethod;
        private String category;
        private String rawText;

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public LocalDateTime getDatetime() {
            return datetime;
        }

        public void setDatetime(LocalDateTime datetime) {
            this.datetime = datetime;
        }

        public Integer getAmount() {
            return amount;
        }

        public void setAmount(Integer amount) {
            this.amount = amount;
        }

        public String getMerchant() {
            return merchant;
        }

        public void setMerchant(String merchant) {
            this.merchant = merchant;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getRawText() {
            return rawText;
        }

        public void setRawText(String rawText) {
            this.rawText = rawText;
        }
    }
}
