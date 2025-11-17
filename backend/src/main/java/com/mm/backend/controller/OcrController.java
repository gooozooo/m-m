package com.mm.backend.controller;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mm.backend.entity.Transaction;
import com.mm.backend.service.OcrService;
import com.mm.backend.service.TransactionService.TransactionRequest;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    // 1) 이미지 → 분석 결과 미리보기 (DB 저장 X)
    @PostMapping(
            value = "/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<TransactionRequest> preview(@RequestPart("file") MultipartFile file) {
        return ocrService.analyzeCapture(file);
    }

    // 2) 이미지 → 분석 + DB 저장
    @PostMapping(
            value = "/analyze-and-save",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<Transaction> analyzeAndSave(@RequestPart("file") MultipartFile file) {
        return ocrService.analyzeAndSave(file);
    }

    // 3) 텍스트만 보내서 파싱 결과 미리보기 (서비스 메서드 안 부르고, 여기서 직접 파싱)
    @PostMapping("/parse-text")
    public List<TransactionRequest> parseText(@RequestBody TextParseRequest request) {
        String rawText = request.getRawText();
        return analyzeText(rawText);
    }

    // ---------- 요청 DTO ----------
    public static class TextParseRequest {

        private String rawText;

        public String getRawText() {
            return rawText;
        }

        public void setRawText(String rawText) {
            this.rawText = rawText;
        }
    }

    // ---------- 여기부터는 컨트롤러 안에 직접 구현한 파싱 로직 ----------

    private List<TransactionRequest> analyzeText(String rawText) {
        List<TransactionRequest> list = new ArrayList<>();

        TransactionRequest req = new TransactionRequest();
        req.setRawText(rawText);

        YearMonth ym = YearMonth.now();
        req.setMonth(ym.toString());
        req.setDatetime(LocalDateTime.now());

        req.setAmount(extractAmount(rawText));
        String merchant = extractMerchant(rawText);
        req.setMerchant(merchant);
        req.setPaymentMethod(detectPaymentMethod(rawText));
        req.setCategory(guessCategory(merchant, rawText));

        list.add(req);
        return list;
    }

    private Integer extractAmount(String text) {
        Pattern p = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*원");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
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
                token.contains("결제") || token.matches(".*\\d.*")) {
                continue;
            }
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
