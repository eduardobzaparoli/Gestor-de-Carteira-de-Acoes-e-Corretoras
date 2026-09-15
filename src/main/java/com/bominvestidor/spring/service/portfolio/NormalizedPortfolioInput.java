package com.bominvestidor.spring.service.portfolio;

import java.util.UUID;

record NormalizedPortfolioInput(String name, String nameKey, UUID brokerageId) {
}
