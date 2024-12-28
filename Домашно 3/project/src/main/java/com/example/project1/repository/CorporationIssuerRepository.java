package com.example.project1.repository;

import com.example.project1.model.CorporationIssuer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorporationIssuerRepository extends JpaRepository<CorporationIssuer, Long> {
    Optional<CorporationIssuer> findByCorporationCode(String companyCode);
    List<CorporationIssuer> findAllByOrderByCorporationCodeAsc();
}
