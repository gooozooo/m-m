package com.mm.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "2025-11" 같은 월 정보
    private String month;

    // 그 달의 예산 (원)
    private Integer amount;
}
