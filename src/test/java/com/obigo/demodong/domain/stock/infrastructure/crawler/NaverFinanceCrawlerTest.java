package com.obigo.demodong.domain.stock.infrastructure.crawler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@SpringBootTest
class NaverFinanceCrawlerTest {
    @Autowired
    private NaverFinanceCrawler naverFinanceCrawler;

    @Test
    void crawl() {

        //given
        String ticekr = "삼성전자";
        //when
        String result = naverFinanceCrawler.crawl(ticekr);
        //then
        System.out.println(result);

        assertThat(result).isNotEmpty();
    }
}