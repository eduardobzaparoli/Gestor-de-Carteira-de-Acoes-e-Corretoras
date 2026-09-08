import { fireEvent, render, screen } from "@testing-library/react";
import { AssetLogo, assetLogoUrl } from "./AssetLogo";

test("resolve a fonte de logo de acordo com o mercado", () => {
  expect(assetLogoUrl("bbdc4", "BR")).toBe(
    "https://icons.brapi.dev/icons/BBDC4.svg",
  );
  expect(assetLogoUrl("aapl", "US")).toBe(
    "https://assets.parqet.com/logos/symbol/AAPL?format=webp&size=96",
  );
});

test("remove a imagem quebrada e mantém o marcador local", () => {
  const { container } = render(<AssetLogo ticker="BBDC4" market="BR" />);
  const image = screen.getByRole("img", { name: "Logotipo de BBDC4" });

  fireEvent.error(image);

  expect(screen.queryByRole("img")).not.toBeInTheDocument();
  expect(container.querySelector(".asset-logo__fallback")).toHaveTextContent(
    "BR",
  );
});
