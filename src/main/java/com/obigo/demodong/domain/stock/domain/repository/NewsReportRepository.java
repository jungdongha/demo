package com.obigo.demodong.domain.stock.domain.repository;

import com.obigo.demodong.domain.stock.domain.entity.NewsReport;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsReportRepository extends JpaRepository<NewsReport, Long> {

    List<NewsReport> findByStockOrderByCreatedAtDesc(Stock stock);
}

