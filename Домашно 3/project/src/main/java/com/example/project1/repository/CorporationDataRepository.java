package com.example.project1.repository;

import com.example.project1.model.CorporationIssuer;
import com.example.project1.model.CorporationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorporationDataRepository extends JpaRepository<CorporationData, Long> {
    Optional<CorporationData> findByDateAndCorporationIssuer(LocalDate date, CorporationIssuer company);
    List<CorporationData> findByCorporationIssuerIdAndDateBetween(Long companyId, LocalDate from, LocalDate to);
    List<CorporationData> findByCorporationIssuerId(Long companyId);
}
