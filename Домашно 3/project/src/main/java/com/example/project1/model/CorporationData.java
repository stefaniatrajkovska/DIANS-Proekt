package com.example.project1.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "corporation_data")
@Data
@NoArgsConstructor
public class CorporationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "transaction_price")
    private Double transaction_price;

    @Column(name = "maxPrice")
    private Double max_price;

    @Column(name = "minPrice")
    private Double min_price;

    @Column(name = "avgPrice")
    private Double avg_price;

    @Column(name = "percentageChangePrice")
    private Double price_percentage_change;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "turnoverBest")
    private Integer turnover_best;

    @Column(name = "turnoverTotal")
    private Integer turnover_total;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private CorporationIssuer corporationIssuer;

    // Конструктор за пополнување на податоците за објектите
    public CorporationData(LocalDate recordDate, Double transactionPrice, Double highestPrice, Double lowestPrice, Double avgPrice, Double priceChangePercentage, Integer tradedQuantity, Integer topTurnover, Integer overallTurnover) {
        this.date = recordDate;
        this.transaction_price = transactionPrice;
        this.max_price = highestPrice;
        this.min_price = lowestPrice;
        this.avg_price = avgPrice;
        this.price_percentage_change = priceChangePercentage;
        this.quantity = tradedQuantity;
        this.turnover_best = topTurnover;
        this.turnover_total = overallTurnover;
    }
}
