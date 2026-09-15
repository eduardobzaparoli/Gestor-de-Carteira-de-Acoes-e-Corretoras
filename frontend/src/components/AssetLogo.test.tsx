import { fireEvent, render, screen } from "@testing-library/react";
import { AssetLogo, assetLogoUrl } from "./AssetLogo";

test("resolve a fonte de logo de acordo com o mercado", () => {
  expect(assetLogoUrl("bbdc4", "BR")).toBe(
    "https://icons.brapi.dev/icons/BBDC4.svg",
  );
  expect(assetLogoUrl("aapl", "US")).toBe(
    "https://assets.parqet.com/logos/symbol/AAPL?format=webp&size=96",
  );
  expect(assetLogoUrl("sanb11f", "BR")).toBe(
    "https://icons.brapi.dev/icons/SANB11.svg",
  );
});

test("marca a imagem como carregada sem alterar o ticker acessível", () => {
  const { container } = render(<AssetLogo ticker="SANB11F" market="BR" />);
  const image = screen.getByRole("img", { name: "Logotipo de SANB11F" });

  fireEvent.load(image);

  expect(image).toHaveAttribute(
    "src",
    "https://icons.brapi.dev/icons/SANB11.svg",
  );
  expect(container.querySelector(".asset-logo")).toHaveAttribute(
    "data-loaded",
    "true",
  );
});

test("tenta fontes alternativas antes de manter o marcador local", () => {
  const { container } = render(<AssetLogo ticker="BBDC4" market="BR" />);
  let image = screen.getByRole("img", { name: "Logotipo de BBDC4" });

  fireEvent.error(image);
  image = screen.getByRole("img", { name: "Logotipo de BBDC4" });
  expect(image).toHaveAttribute("src", expect.stringContaining("retry=1"));
  fireEvent.error(image);
  image = screen.getByRole("img", { name: "Logotipo de BBDC4" });
  expect(image).toHaveAttribute("src", expect.stringContaining("retry=2"));
  fireEvent.error(image);

  expect(screen.queryByRole("img")).not.toBeInTheDocument();
  expect(container.querySelector(".asset-logo__fallback")).toHaveTextContent(
    "BR",
  );
});

test("usa outro formato quando a logo americana em webp falha", () => {
  render(<AssetLogo ticker="MSFT" market="US" />);
  const image = screen.getByRole("img", { name: "Logotipo de MSFT" });

  fireEvent.error(image);

  expect(screen.getByRole("img", { name: "Logotipo de MSFT" })).toHaveAttribute(
    "src",
    expect.stringContaining("format=png"),
  );
});
