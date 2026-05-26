package com.obigo.demodong.domain.analysis.infrastructure;

import org.springframework.stereotype.Component;

import java.time.Month;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 업종별, 월별 계절성 점수를 제공하는 하드코딩 테이블.
 * 주요 8개 섹터와 시장 평균(DEFAULT) 데이터를 내장하고 있다.
 */
@Component
public class SeasonalityTable {

    private static final Map<String, Map<Month, Integer>> TABLE = new HashMap<>();

    static {
        // 1. 반도체 (연말/연초 강세)
        Map<Month, Integer> semiconductor = new EnumMap<>(Month.class);
        semiconductor.put(Month.JANUARY, 85);
        semiconductor.put(Month.FEBRUARY, 80);
        semiconductor.put(Month.MARCH, 70);
        semiconductor.put(Month.APRIL, 60);
        semiconductor.put(Month.MAY, 45);
        semiconductor.put(Month.JUNE, 50);
        semiconductor.put(Month.JULY, 55);
        semiconductor.put(Month.AUGUST, 50);
        semiconductor.put(Month.SEPTEMBER, 65);
        semiconductor.put(Month.OCTOBER, 70);
        semiconductor.put(Month.NOVEMBER, 80);
        semiconductor.put(Month.DECEMBER, 90);
        TABLE.put("반도체", semiconductor);

        // 2. 바이오 (봄/여름 학회 시즌 강세)
        Map<Month, Integer> bio = new EnumMap<>(Month.class);
        bio.put(Month.JANUARY, 60);
        bio.put(Month.FEBRUARY, 65);
        bio.put(Month.MARCH, 80);
        bio.put(Month.APRIL, 85);
        bio.put(Month.MAY, 75);
        bio.put(Month.JUNE, 80);
        bio.put(Month.JULY, 55);
        bio.put(Month.AUGUST, 45);
        bio.put(Month.SEPTEMBER, 60);
        bio.put(Month.OCTOBER, 50);
        bio.put(Month.NOVEMBER, 55);
        bio.put(Month.DECEMBER, 70);
        TABLE.put("바이오", bio);

        // 3. 자동차 (신차 및 연말 소비 강세)
        Map<Month, Integer> auto = new EnumMap<>(Month.class);
        auto.put(Month.JANUARY, 50);
        auto.put(Month.FEBRUARY, 55);
        auto.put(Month.MARCH, 65);
        auto.put(Month.APRIL, 70);
        auto.put(Month.MAY, 60);
        auto.put(Month.JUNE, 55);
        auto.put(Month.JULY, 50);
        auto.put(Month.AUGUST, 55);
        auto.put(Month.SEPTEMBER, 75);
        auto.put(Month.OCTOBER, 80);
        auto.put(Month.NOVEMBER, 85);
        auto.put(Month.DECEMBER, 75);
        TABLE.put("자동차", auto);

        // 4. 은행 (연말 배당 시즌 강세)
        Map<Month, Integer> bank = new EnumMap<>(Month.class);
        bank.put(Month.JANUARY, 55);
        bank.put(Month.FEBRUARY, 50);
        bank.put(Month.MARCH, 45);
        bank.put(Month.APRIL, 40);
        bank.put(Month.MAY, 45);
        bank.put(Month.JUNE, 50);
        bank.put(Month.JULY, 55);
        bank.put(Month.AUGUST, 60);
        bank.put(Month.SEPTEMBER, 70);
        bank.put(Month.OCTOBER, 80);
        bank.put(Month.NOVEMBER, 90);
        bank.put(Month.DECEMBER, 95);
        TABLE.put("은행", bank);

        // 5. IT (신제품 및 연초 랠리 강세)
        Map<Month, Integer> it = new EnumMap<>(Month.class);
        it.put(Month.JANUARY, 80);
        it.put(Month.FEBRUARY, 85);
        it.put(Month.MARCH, 75);
        it.put(Month.APRIL, 65);
        it.put(Month.MAY, 50);
        it.put(Month.JUNE, 55);
        it.put(Month.JULY, 60);
        it.put(Month.AUGUST, 50);
        it.put(Month.SEPTEMBER, 75);
        it.put(Month.OCTOBER, 80);
        it.put(Month.NOVEMBER, 75);
        it.put(Month.DECEMBER, 80);
        TABLE.put("IT", it);

        // 6. 소비재 (여름 휴가 및 연말 쇼핑 랠리)
        Map<Month, Integer> consumer = new EnumMap<>(Month.class);
        consumer.put(Month.JANUARY, 60);
        consumer.put(Month.FEBRUARY, 55);
        consumer.put(Month.MARCH, 50);
        consumer.put(Month.APRIL, 55);
        consumer.put(Month.MAY, 65);
        consumer.put(Month.JUNE, 75);
        consumer.put(Month.JULY, 80);
        consumer.put(Month.AUGUST, 75);
        consumer.put(Month.SEPTEMBER, 60);
        consumer.put(Month.OCTOBER, 65);
        consumer.put(Month.NOVEMBER, 85);
        consumer.put(Month.DECEMBER, 90);
        TABLE.put("소비재", consumer);

        // 7. 화학 (봄철 산업 수요 증가)
        Map<Month, Integer> chemical = new EnumMap<>(Month.class);
        chemical.put(Month.JANUARY, 60);
        chemical.put(Month.FEBRUARY, 70);
        chemical.put(Month.MARCH, 85);
        chemical.put(Month.APRIL, 80);
        chemical.put(Month.MAY, 75);
        chemical.put(Month.JUNE, 60);
        chemical.put(Month.JULY, 50);
        chemical.put(Month.AUGUST, 45);
        chemical.put(Month.SEPTEMBER, 55);
        chemical.put(Month.OCTOBER, 60);
        chemical.put(Month.NOVEMBER, 65);
        chemical.put(Month.DECEMBER, 60);
        TABLE.put("화학", chemical);

        // 8. 에너지 (겨울철 난방 성수기)
        Map<Month, Integer> energy = new EnumMap<>(Month.class);
        energy.put(Month.JANUARY, 85);
        energy.put(Month.FEBRUARY, 75);
        energy.put(Month.MARCH, 60);
        energy.put(Month.APRIL, 50);
        energy.put(Month.MAY, 45);
        energy.put(Month.JUNE, 50);
        energy.put(Month.JULY, 55);
        energy.put(Month.AUGUST, 60);
        energy.put(Month.SEPTEMBER, 70);
        energy.put(Month.OCTOBER, 80);
        energy.put(Month.NOVEMBER, 85);
        energy.put(Month.DECEMBER, 90);
        TABLE.put("에너지", energy);

        // 9. DEFAULT (시장 평균, 산타 랠리 및 셀인메이 반영)
        Map<Month, Integer> defaultTable = new EnumMap<>(Month.class);
        defaultTable.put(Month.JANUARY, 70);
        defaultTable.put(Month.FEBRUARY, 65);
        defaultTable.put(Month.MARCH, 60);
        defaultTable.put(Month.APRIL, 55);
        defaultTable.put(Month.MAY, 40);
        defaultTable.put(Month.JUNE, 45);
        defaultTable.put(Month.JULY, 50);
        defaultTable.put(Month.AUGUST, 45);
        defaultTable.put(Month.SEPTEMBER, 55);
        defaultTable.put(Month.OCTOBER, 60);
        defaultTable.put(Month.NOVEMBER, 75);
        defaultTable.put(Month.DECEMBER, 80);
        TABLE.put("DEFAULT", defaultTable);
    }

    /**
     * 특정 섹터와 월에 대한 계절성 점수를 가져온다.
     *
     * @param sector 섹터명 (주요 8개 섹터 중 하나)
     * @param month 월 (Month)
     * @return 계절성 점수 (0~100). 매칭되는 섹터가 없을 시 DEFAULT 테이블 값 사용.
     */
    public int getScore(String sector, Month month) {
        if (month == null) {
            month = Month.JANUARY; // 방어 코드
        }

        Map<Month, Integer> sectorMap = null;
        if (sector != null) {
            sectorMap = TABLE.get(sector);
        }

        if (sectorMap == null) {
            sectorMap = TABLE.get("DEFAULT");
        }

        return sectorMap.getOrDefault(month, 50);
    }
}
