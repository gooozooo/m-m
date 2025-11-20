package com.mm.backend.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mm.backend.entity.Transaction;
import com.mm.backend.service.TransactionService.TransactionRequest;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OcrService {

    private final TransactionService transactionService;
    private final AiParserService aiParserService;

    public OcrService(TransactionService transactionService,
                      AiParserService aiParserService) {
        this.transactionService = transactionService;
        this.aiParserService = aiParserService;
    }

    /** 1) 이미지 -> OCR -> 텍스트 -> 규칙 기반 */
    public List<TransactionRequest> analyzeCapture(MultipartFile file) {
        try {
            String text = ocrImage(file);
            return analyzeText(text);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** 2) 텍스트 -> TransactionRequest (규칙 기반) */
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

    /** 3) OCR + DB 저장 */
    public List<Transaction> analyzeAndSave(MultipartFile file) {
        List<TransactionRequest> parsedList = analyzeCapture(file);
        List<Transaction> saved = new ArrayList<>();
        for (TransactionRequest req : parsedList) {
            saved.add(transactionService.addTransaction(req));
        }
        return saved;
    }

    /** 4) 이미지 → OCR → AI 파싱 */
    public TransactionRequest analyzeCaptureWithAI(MultipartFile file) throws Exception {
        String text = ocrImage(file);
        return aiParserService.parseWithAI(text);
    }

    // OCR 처리
    private String ocrImage(MultipartFile file) throws IOException, TesseractException {
        String originalName = file.getOriginalFilename();
        String suffix = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            suffix = originalName.substring(originalName.lastIndexOf('.'));
        }

        File temp = File.createTempFile("ocr-", suffix);
        file.transferTo(temp);

        try {
            BufferedImage img = ImageIO.read(temp);
            if (img == null) throw new IOException("이미지를 읽을 수 없음");

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
            tesseract.setLanguage("kor+eng");
            return tesseract.doOCR(img);

        } finally {
            temp.delete();
        }
    }

    private Integer extractAmount(String text) {
        Matcher m = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*원").matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1).replace(",", ""));
        return null;
    }

    private String extractMerchant(String text) {
        String cleaned = text.replaceAll("\\[[^]]+\\]", "");
        for (String token : cleaned.split("\\s+")) {
            if (token.matches(".*\\d.*") || token.contains("결제")) continue;
            return token;
        }
        return "Unknown";
    }

    private String detectPaymentMethod(String text) {
        if (text.contains("카카오")) return "KakaoPay";
        if (text.contains("네이버")) return "NaverPay";
        if (text.contains("카드"))  return "Card";
        return "Unknown";
    }

    private String guessCategory(String merchant, String text) {
        String base = merchant + " " + text;
        if (base.contains("커피")) return "카페";
        return "기타";
    }
}
