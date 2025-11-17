package com.mm.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 소비가 속하는 달 (예: "2025-11")
    private String month;

    // 실제 결제 시각
    private LocalDateTime datetime;

    // 결제 금액
    private Integer amount;

    // 가맹점 이름 (스타벅스, 편의점 등)
    private String merchant;

    // 결제 수단 (KakaoPay, NaverPay, Card 등)
    private String paymentMethod;

    // 카테고리 (카페/간식, 식비, 교통, 기타 등)
    private String category;

    // OCR/문자에서 뽑은 원본 텍스트 전체
    @Column(columnDefinition = "TEXT")
    private String rawText;
}
