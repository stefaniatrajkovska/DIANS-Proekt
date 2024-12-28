package com.example.project1.service;

import com.example.project1.model.CorporationIssuer;
import com.example.project1.repository.CorporationIssuerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CorporationIssuerService {

    private final CorporationIssuerRepository companyIssuerRepository;

    public List<CorporationIssuer> findAll() {
        return companyIssuerRepository.findAllByOrderByCorporationCodeAsc();
    }

    public CorporationIssuer findById(Long id) throws Exception {
        return companyIssuerRepository.findById(id).orElseThrow(Exception::new);
    }

}
