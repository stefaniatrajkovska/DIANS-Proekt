package com.example.project1.repository;

import com.example.project1.model.CompanyIssuer;
import com.example.project1.model.DayPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DayPriceRepository extends JpaRepository<DayPrice, Long> {
    Optional<DayPrice> findByDateAndCompany(LocalDate date, CompanyIssuer companyIssuer);
    List<DayPrice> findByCompanyIdAndDateBetween(Long companyId, LocalDate from, LocalDate to);
}
