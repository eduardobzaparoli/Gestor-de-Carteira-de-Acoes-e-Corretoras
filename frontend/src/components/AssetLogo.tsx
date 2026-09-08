import { useEffect, useState } from "react";

export type AssetLogoMarket = "BR" | "US" | string;

export const assetLogoUrl = (ticker: string, market: AssetLogoMarket) => {
  const symbol = ticker.trim().toUpperCase();
  if (!symbol) return null;
  const encoded = encodeURIComponent(symbol);
  if (market === "BR") return `https://icons.brapi.dev/icons/${encoded}.svg`;
  if (market === "US")
    return `https://assets.parqet.com/logos/symbol/${encoded}?format=webp&size=96`;
  return null;
};

export function AssetLogo({
  ticker,
  market,
}: {
  ticker: string;
  market: AssetLogoMarket;
}) {
  const source = assetLogoUrl(ticker, market);
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setFailed(false);
    setLoaded(false);
  }, [source]);

  return (
    <span className="asset-logo" data-loaded={loaded && !failed}>
      <span className="asset-logo__fallback" aria-hidden="true">
        {market || ticker.slice(0, 2).toUpperCase()}
      </span>
      {source && !failed && (
        <img
          src={source}
          alt={`Logotipo de ${ticker.toUpperCase()}`}
          loading="lazy"
          decoding="async"
          referrerPolicy="no-referrer"
          onLoad={() => setLoaded(true)}
          onError={() => setFailed(true)}
        />
      )}
    </span>
  );
}

export function AssetLogoAttribution() {
  return (
    <p className="asset-logo-attribution">
      Logos americanos fornecidos por{" "}
      <a href="https://parqet.com/api" target="_blank" rel="noreferrer">
        Parqet
      </a>
      .
    </p>
  );
}
