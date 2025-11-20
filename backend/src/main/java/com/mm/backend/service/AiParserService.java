package com.mm.backend.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mm.backend.service.TransactionService.TransactionRequest;

import jakarta.annotation.PostConstruct;

@Service
public class AiParserService {

    @Value("${openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void checkKey() {
        if (apiKey == null) {
            System.out.println("🔥 OPENAI KEY NOT FOUND (null)");
        } else {
            System.out.println("🔥 OPENAI KEY LOADED: " +
                    apiKey.substring(0, Math.min(20, apiKey.length())) + "...");
        }
    }

    public TransactionRequest parseWithAI(String rawText) {
        try {
            // ===================== 1) GPT 프롬프트 =====================
            String requestBody = """
            {
              "model": "gpt-4o-mini",
              "messages": [
                {
                  "role": "system",
                  "content": "You are an expert receipt parsing assistant. Output ONLY valid JSON."
                },
                {
                  "role": "user",
                  "content": "Extract payment info from this text and return ONLY this JSON format:\\n{\\n  \\"amount\\": number,\\n  \\"merchant\\": string,\\n  \\"paymentMethod\\": string,\\n  \\"category\\": string,\\n  \\"month\\": \\"YYYY-MM\\",\\n  \\"datetime\\": \\"YYYY-MM-DDTHH:mm:ss\\"\\n}\\nTEXT: %s"
                }
              ],
              "temperature": 0.0
            }
            """.formatted(rawText);

            // ===================== 2) HTTP 요청 설정 =====================
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("=== OpenAI STATUS === " + response.getStatusCode());
            System.out.println("=== OpenAI BODY === " + response.getBody());

            // ===================== 3) JSON 파싱 ==========================
            JsonNode root = mapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            System.out.println("=== AI Parsed Content === " + content);

            // ===================== 4) 결과 채우기 ========================
            return convertJsonToTransaction(rawText, content);
        }
        // ======= GPT 에러(429, 401 등) → 규칙 기반 fallback ============
        catch (HttpClientErrorException e) {
            System.out.println("=== OpenAI ERROR STATUS === " + e.getStatusCode());
            System.out.println("=== OpenAI ERROR BODY === " + e.getResponseBodyAsString());

            TransactionRequest backup = ruleBasedParse(rawText);
            backup.setMerchant("GPT_FAILED | " + backup.getMerchant());
            return backup;
        }
        catch (Exception e) {
            e.printStackTrace();
            // 여기서도 완전 죽지 말고 규칙 기반 fallback 사용
            TransactionRequest backup = ruleBasedParse(rawText);
            backup.setMerchant("GPT_ERROR | " + backup.getMerchant());
            return backup;
        }
    }

    // ===================== JSON → TransactionRequest ======================
    private TransactionRequest convertJsonToTransaction(String rawText, String jsonText) {
        TransactionRequest tr = new TransactionRequest();
        tr.setRawText(rawText);

        try {
            JsonNode json = mapper.readTree(jsonText);

            if (json.has("amount")) tr.setAmount(json.path("amount").asInt());
            if (json.has("merchant")) tr.setMerchant(json.path("merchant").asText());
            if (json.has("paymentMethod")) tr.setPaymentMethod(json.path("paymentMethod").asText());
            if (json.has("category")) tr.setCategory(json.path("category").asText());
            if (json.has("month")) tr.setMonth(json.path("month").asText());

            if (json.has("datetime")) {
                try {
                    tr.setDatetime(LocalDateTime.parse(json.path("datetime").asText()));
                } catch (Exception e) {
                    tr.setDatetime(LocalDateTime.now());
                }
            } else {
                tr.setDatetime(LocalDateTime.now());
            }

            if (tr.getMonth() == null) {
                tr.setMonth(YearMonth.now().toString());
            }
            if (tr.getMerchant() == null) tr.setMerchant("Unknown");
            if (tr.getPaymentMethod() == null) tr.setPaymentMethod("Unknown");
            if (tr.getCategory() == null) tr.setCategory("기타");

        } catch (Exception e) {
            System.out.println("⚠️ JSON 형식 아님 → 전체 응답을 merchant에 저장");
            tr.setMerchant(jsonText);
            tr.setDatetime(LocalDateTime.now());
            tr.setMonth(YearMonth.now().toString());
        }

        return tr;
    }

    // ===================== 규칙 기반 fallback 파서 ========================
    private TransactionRequest ruleBasedParse(String rawText) {
        TransactionRequest tr = new TransactionRequest();
        tr.setRawText(rawText);
        tr.setDatetime(LocalDateTime.now());
        tr.setMonth(YearMonth.now().toString());

        // 1) 금액 추출: "4,500원", "5400원"
        Pattern p = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*원");
        Matcher m = p.matcher(rawText);
        if (m.find()) {
            try {
                tr.setAmount(Integer.parseInt(m.group(1).replace(",", "")));
            } catch (NumberFormatException ignore) {}
        }

        // 2) 가맹점 추정 (아주 단순 버전)
        String merchant = "Unknown";
        if (rawText.contains("스타벅스")) merchant = "스타벅스";
        else if (rawText.contains("배달의민족")) merchant = "배달의민족";
        else if (rawText.contains("요기요")) merchant = "요기요";
        else if (rawText.contains("CU") || rawText.contains("GS25")) merchant = "편의점";
        tr.setMerchant(merchant);

        // 3) 결제수단 추정
        String pm = "Unknown";
        if (rawText.contains("카카오페이")) pm = "KakaoPay";
        else if (rawText.contains("네이버페이")) pm = "NaverPay";
        else if (rawText.contains("삼성페이")) pm = "SamsungPay";
        else if (rawText.contains("토스")) pm = "TossPay";
        else if (rawText.contains("카드")) pm = "Card";
        tr.setPaymentMethod(pm);

        // 4) 카테고리 추정
        String base = rawText;
        String category = "기타";
        if (base.contains("스타벅스") || base.contains("커피") || base.contains("카페")) {
            category = "카페/간식";
        } else if (base.contains("편의점") || base.contains("마트") || base.contains("식품")) {
            category = "식비/장보기";
        } else if (base.contains("택시") || base.contains("버스") || base.contains("지하철")) {
            category = "교통";
        } else if (base.contains("배달") || base.contains("배달의민족") || base.contains("요기요")) {
            category = "배달/외식";
        }
        tr.setCategory(category);

        return tr;
    }
}
