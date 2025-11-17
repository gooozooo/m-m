package com.mm.backend.controller;

import com.mm.backend.entity.Transaction;
import com.mm.backend.service.OcrService;
import com.mm.backend.service.TransactionService.TransactionRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;

    // 1) 이미지 → 분석 결과 (미리보기, 저장 X)
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

    // 3) "텍스트"만 보내서 파싱 결과 미리보기
    @PostMapping("/parse-text")
    public List<TransactionRequest> parseText(@RequestBody TextParseRequest request) {
        return ocrService.analyzeText(request.getRawText());
    }

    // ---------- 요청 DTO ----------
    @Getter
    @Setter
    public static class TextParseRequest {
        private String rawText;
    }
}
