package com.mm.backend.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mm.backend.entity.Transaction;
import com.mm.backend.service.TransactionService.TransactionRequest;

@Service
public class OcrService {

    private final TransactionService transactionService;

    public OcrService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 1) 이미지 → 분석 (지금은 더미 텍스트)
     */
    public List<TransactionRequest> analyzeCapture(MultipartFile file) {
        String fakeText = "[카카오페이] 11/13 14:23 스타벅스 아메리카노 4,500원 결제 완료";
        return analyzeText(fakeText);
    }

    /**
     * 2) 진짜 핵심: 텍스트 → TransactionRequest 파싱
     */
    public List<TransactionRequest> analyzeText(String rawText) {
        List<TransactionRequest> list = new ArrayList<>();

        TransactionRequest req = new TransactionRequest();
        req.setRawText(rawText);

        YearMonth ym = YearMonth.now();
        req.setMonth(ym.toString());
        req.setDatetime(LocalDateTime.now());

        req.setAmount(extractAmount(rawText));
        req.setMerchant(extractMerchant(rawText));
        req.setPaymentMethod(detectPaymentMethod(rawText));
        req.setCategory(guessCategory(req.getMerchant(), rawText));

        list.add(req);
        return list;
    }

    /**
     * 3) 텍스트 분석 결과 → DB 저장
     */
    public List<Transaction> analyzeAndSave(MultipartFile file) {
        List<TransactionRequest> parsedList = analyzeCapture(file);

        List<Transaction> saved = new ArrayList<>();
        for (TransactionRequest req : parsedList) {
            Transaction t = transactionService.addTransaction(req);
            saved.add(t);
        }
        return saved;
    }

    // ----------------- Helper Methods -----------------

    private Integer extractAmount(String text) {
        Pattern p = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*원");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1).replace(",", ""));
        }
        return null;
    }

    private String extractMerchant(String text) {
        String cleaned = text.replaceAll("\\[[^]]+\\]", "").trim();
        int idx = cleaned.indexOf("원");
        if (idx > 0) cleaned = cleaned.substring(0, idx);

        String[] tokens = cleaned.split("\\s+");
        for (String token : tokens) {
            if (token.contains("카카오페이") || token.contains("네이버페이") ||
                token.contains("결제") || token.matches(".*\\d.*")) continue;
            return token;
        }
        return "Unknown";
    }

    private String detectPaymentMethod(String text) {
        if (text.contains("카카오페이")) return "KakaoPay";
        if (text.contains("네이버페이")) return "NaverPay";
        if (text.contains("삼성페이")) return "SamsungPay";
        if (text.contains("카드")) return "Card";
        return "Unknown";
    }

    private String guessCategory(String merchant, String text) {
        String base = (merchant + " " + text);

        if (base.contains("스타벅스") || base.contains("커피") || base.contains("카페"))
            return "카페/간식";
        if (base.contains("편의점") || base.contains("마트") || base.contains("식품"))
            return "식비/장보기";
        if (base.contains("택시") || base.contains("버스") || base.contains("지하철"))
            return "교통";
        if (base.contains("배달") || base.contains("배달의민족") || base.contains("요기요"))
            return "배달/외식";

        return "기타";
    }
}
