package com.bominvestidor.spring.domain.asset;

import java.math.BigDecimal;

public record AssetQuote(String ticker, String currency, BigDecimal price) {
}
