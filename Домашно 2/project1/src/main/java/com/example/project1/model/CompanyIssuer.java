package com.example.project1.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "company_issuer")
@Data
@NoArgsConstructor
public class CompanyIssuer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "last_updated")
    private LocalDate lastUpdated;

    @OneToMany(mappedBy = "company", fetch = FetchType.EAGER)
    private List<DayPrice> historicalData;

    public CompanyIssuer(String companyCode) {
        this.companyCode = companyCode;
    }

}
