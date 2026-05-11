package com.obigo.demodong.domain.portfolio.domain.service;

import com.obigo.demodong.domain.portfolio.domain.entity.PortfolioDetail;
import com.obigo.demodong.domain.portfolio.domain.repository.PortfolioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortFolioWriter {

    private final PortfolioDetailRepository portfolioDetailRepository;

    public PortfolioDetail save(PortfolioDetail detail) {
        return portfolioDetailRepository.save(detail);
    }
}
