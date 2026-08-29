package com.bominvestidor.spring.integration.asset;

import java.util.List;
import java.util.Optional;

import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;

public interface AssetSearchStrategy {

	AssetMarket market();
	List<AssetCandidate> findCandidates(AssetType assetType, String query);
	Optional<AssetQuote> findQuote(String ticker);
}
