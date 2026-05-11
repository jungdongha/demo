package com.obigo.demodong.domain.portfolio.domain.repository;

import com.obigo.demodong.domain.portfolio.domain.entity.PortfolioDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioDetailRepository extends JpaRepository<PortfolioDetail, Long> {
    List<PortfolioDetail> findAllByDeletedFalse();

    Optional<PortfolioDetail> findByIdAndDeletedFalse(Long id);

    Optional<PortfolioDetail> findByStock_TickerAndDeletedFalse(String ticker);

}
