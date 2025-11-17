package com.mm.backend.service;

import com.mm.backend.entity.Transaction;
import com.mm.backend.service.TransactionService.TransactionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final TransactionService transactionService;

    /**
     * ① 아직은 이미지 → 텍스트 OCR이 없으니까,
     *    임시로 더미 TransactionRequest를 하나 만들어서 반환.
     *    나중에 file을 OCR 돌려서 나온 텍스트를 analyzeText()에 넘기면 된다.
     */
    public List<TransactionRequest> analyzeCapture(MultipartFile file) {
        // TODO: file → OCR → 텍스트 추출 후 analyzeText(rawText) 호출하도록 변경하기

        // 지금은 테스트용으로 고정 텍스트를 사용
        String fakeText = "[카카오페이] 11/13 14:23 스타벅스 아메리카노 4,500원 결제 완료";
        return analyzeText(fakeText);
    }

    /**
     * ② 카카오페이 / 네이버페이 / 문자 같은 "텍스트"를 받아서
     *    TransactionRequest 리스트로 변환하는 핵심 로직.
     *    지금은 간단한 정규식 + if 문으로 만들고,
     *    나중에 이 안을 LLM(API) 호출로 교체하면 된다.
     */
    public List<TransactionRequest> analyzeText(String rawText) {
        List<TransactionRequest> list = new ArrayList<>();

        TransactionRequest req = new TransactionRequest();

        // 원본 텍스트는 그대로 저장
        req.setRawText(rawText);

        // month: YearMonth.now() 또는 텍스트에서 연/월 추출
        YearMonth ym = YearMonth.now();
        req.setMonth(ym.toString()); // 예: "2025-11"

        // datetime: 지금은 대충 now()로; 나중에 텍스트에서 날짜/시간 파싱
        req.setDatetime(LocalDateTime.now());

        // amount(금액) 추출: "4,500원", "12000원" 등 숫자 + "원" 패턴 찾기
        Integer amount = extractAmount(rawText);
        req.setAmount(amount);

        // merchant(가맹점) 추출: "스타벅스", "네이버페이", "배달의민족" 등
        String merchant = extractMerchant(rawText);
        req.setMerchant(merchant);

        // paymentMethod: 텍스트 안의 키워드를 보고 추정
        String paymentMethod = detectPaymentMethod(rawText);
        req.setPaymentMethod(paymentMethod);

        // category: 아주 단순하게 카페/배달/기타 정도만 구분 (임시)
        String category = guessCategory(merchant, rawText);
        req.setCategory(category);

        list.add(req);
        return list;
    }

    /**
     * ③ analyzeText() 결과를 실제 Transaction 테이블에 저장까지 하는 메서드
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

    // ------------ 아래는 간단한 파싱 helper 메서드들 ------------

    // 금액 숫자 뽑기 (가장 먼저 나오는 "숫자+원" 패턴)
    private Integer extractAmount(String text) {
        Pattern p = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*원");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String num = m.group(1).replace(",", "");
            try {
                return Integer.parseInt(num);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // 아주 단순한 방식으로 가맹점 이름 추출 (카카오페이/네이버페이 등 키워드 뒤의 한 단어)
    private String extractMerchant(String text) {
        // 예: "[카카오페이] 11/13 14:23 스타벅스 아메리카노 4,500원 결제 완료"
        // → "스타벅스" 정도 뽑기
        // 여기서는 그냥 "카카오페이/네이버페이" 같은 단어를 빼고
        // "원" 앞쪽에서 단어 하나만 가져오는 매우 단순한 로직으로 처리
        // (나중에 LLM이 담당하게 될 부분)

        // 1) 대괄호 안 "카카오페이" 이런 건 제거
        String cleaned = text.replaceAll("\\[[^]]+\\]", " ").trim();

        // 2) "원" 앞의 문장만 사용
        int idxWon = cleaned.indexOf("원");
        if (idxWon > 0) {
            cleaned = cleaned.substring(0, idxWon);
        }

        // 3) 공백 기준으로 쪼개서 3개 정도 단어만 보고 "카카오페이/네이버페이/결제" 같은 키워드 제외
        String[] tokens = cleaned.split("\\s+");
        for (String token : tokens) {
            if (token.contains("카카오페이") || token.contains("네이버페이")
                    || token.contains("결제") || token.matches(".*\\d.*")) {
                continue;
            }
            // 숫자나 "결제" 이런 거 아닌 첫 단어를 가맹점으로 가정
            return token;
        }
        return null;
    }

    private String detectPaymentMethod(String text) {
        if (text.contains("카카오페이")) return "KakaoPay";
        if (text.contains("네이버페이")) return "NaverPay";
        if (text.contains("삼성페이")) return "SamsungPay";
        if (text.contains("토스페이")) return "TossPay";
        if (text.contains("신한카드") || text.contains("현대카드") || text.contains("카드")) {
            return "Card";
        }
        return "Unknown";
    }

    private String guessCategory(String merchant, String text) {
        String base = (merchant != null ? merchant : "") + " " + text;
        if (base.contains("카페") || base.contains("스타벅스") || base.contains("커피")) {
            return "카페/간식";
        }
        if (base.contains("편의점") || base.contains("마트") || base.contains("식품")) {
            return "식비/장보기";
        }
        if (base.contains("택시") || base.contains("버스") || base.contains("지하철")) {
            return "교통";
        }
        if (base.contains("배달") || base.contains("배달의민족") || base.contains("요기요")) {
            return "배달/외식";
        }
        return "기타";
    }
}
