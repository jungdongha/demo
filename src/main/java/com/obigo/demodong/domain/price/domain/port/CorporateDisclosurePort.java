package com.obigo.demodong.domain.price.domain.port;

import com.obigo.demodong.domain.price.domain.model.DisclosureItem;

import java.util.List;

public interface CorporateDisclosurePort {
    List<DisclosureItem> fetchRecentDisclosures(String dartCorpCode, int limit);
    String format(List<DisclosureItem> items);
}
