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

const assetLogoSources = (ticker: string, market: AssetLogoMarket) => {
  const primary = assetLogoUrl(ticker, market);
  if (!primary) return [];
  if (market === "US") {
    return [
      primary,
      primary.replace("format=webp", "format=png"),
      primary.replace("format=webp", "format=svg"),
    ];
  }
  return [primary, `${primary}?retry=1`, `${primary}?retry=2`];
};

export function AssetLogo({
  ticker,
  market,
}: {
  ticker: string;
  market: AssetLogoMarket;
}) {
  const sources = assetLogoSources(ticker, market);
  const sourceKey = sources.join("|");
  const [attempt, setAttempt] = useState(0);
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const source = sources[attempt];

  useEffect(() => {
    setAttempt(0);
    setFailed(false);
    setLoaded(false);
  }, [sourceKey]);

  return (
    <span className="asset-logo" data-loaded={loaded && !failed}>
      <span className="asset-logo__fallback" aria-hidden="true">
        {market || ticker.slice(0, 2).toUpperCase()}
      </span>
      {source && !failed && (
        <img
          src={source}
          alt={`Logotipo de ${ticker.toUpperCase()}`}
          loading="eager"
          decoding="async"
          referrerPolicy="no-referrer"
          onLoad={() => setLoaded(true)}
          onError={() => {
            setLoaded(false);
            if (attempt < sources.length - 1) setAttempt(attempt + 1);
            else setFailed(true);
          }}
        />
      )}
    </span>
  );
}
