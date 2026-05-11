package com.obigo.demodong.domain.price.domain.repository;

import com.obigo.demodong.domain.price.domain.entity.PriceSnapshot;
import com.obigo.demodong.domain.stock.domain.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findByStockAndRecordedDate(Stock stock, LocalDate date);

    List<PriceSnapshot> findByStockAndRecordedDateBetweenOrderByRecordedDateAsc(
            Stock stock, LocalDate from, LocalDate to);
}
