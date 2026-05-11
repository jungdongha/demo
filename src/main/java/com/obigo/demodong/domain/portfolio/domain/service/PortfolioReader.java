package com.obigo.demodong.domain.portfolio.domain.service;


import com.obigo.demodong.domain.portfolio.application.exception.PortfolioErrorCode;
import com.obigo.demodong.domain.portfolio.domain.entity.PortfolioDetail;
import com.obigo.demodong.domain.portfolio.domain.repository.PortfolioDetailRepository;
import com.obigo.demodong.global.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioReader {
    private final PortfolioDetailRepository portfolioDetailRepository;

    public List<PortfolioDetail> findAll() {
        return portfolioDetailRepository.findAllByDeletedFalse();
    }

    public PortfolioDetail findById(Long id) {
        return portfolioDetailRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApplicationException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }
}
