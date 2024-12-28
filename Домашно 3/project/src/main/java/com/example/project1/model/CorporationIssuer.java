package com.example.project1.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "corporation_issuer")
@Data
@NoArgsConstructor
public class CorporationIssuer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "corporation_code")
    private String corporationCode;

    @Column(name = "date_last_updated")
    private LocalDate dateLastUpdated;

    @OneToMany(mappedBy = "corporationIssuer", fetch = FetchType.EAGER)
    private List<CorporationData> historicalRecords;

    public CorporationIssuer(String companyCode) {
        this.corporationCode = companyCode;
    }

}
