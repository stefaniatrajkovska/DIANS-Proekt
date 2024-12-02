package com.example.project1.repository;

import com.example.project1.model.CompanyIssuer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyIssuerRepository extends JpaRepository<CompanyIssuer, Long> {
    Optional<CompanyIssuer> findByCompanyCode(String companyCode);
}
