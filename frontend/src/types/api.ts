export type UserRole = "INVESTOR" | "ADMIN";
export type UserStatus = "ACTIVE" | "INACTIVE";
export type AssetMarket = "BR" | "US";
export type AssetType = "STOCK" | "ETF";
export type TransactionType = "BUY" | "SELL";
export type TransactionStatus = "PENDING" | "EFFECTIVE" | "CANCELLED";
export type IncomeEventType =
  "DIVIDEND" | "INTEREST_ON_EQUITY" | "DISTRIBUTION";
export type IncomeEventStatus = "PENDING" | "EFFECTIVE" | "CANCELLED";
export type IncomeEventSource = "BRAPI" | "ALPHA_VANTAGE" | "MANUAL";
export type DecimalValue = number | string;

export interface FieldError {
  field: string;
  message: string;
}
export interface ApiErrorBody {
  timestamp?: string;
  status: number;
  code: string;
  message: string;
  path?: string;
  fieldErrors: FieldError[];
}
export interface PublicUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
}
export interface AuthenticationResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: PublicUser;
}
export interface Address {
  cep: string;
  street: string;
  neighborhood: string;
  number: string;
  complement?: string;
  city: string;
  state: string;
}
export interface CepLookup {
  cep: string;
  street: string;
  neighborhood: string;
  city: string;
  state: string;
}
export interface CnpjLookup {
  cnpj: string;
  legalName: string;
  tradeName?: string;
}
export interface Brokerage {
  id: string;
  nickname: string;
  cnpj: string;
  legalName: string;
  tradeName: string;
  registrationStatus: string;
  cvmParticipantCategory: string;
  address: Address;
  createdAt: string;
  updatedAt: string;
}
export interface BrokerageInput {
  nickname: string;
  cnpj: string;
  cep: string;
  street: string;
  neighborhood: string;
  number: string;
  complement?: string;
}
export interface BrokerageSummary {
  id: string;
  nickname: string;
  cnpj: string;
  legalName: string;
}
export interface Portfolio {
  id: string;
  name: string;
  brokerage: BrokerageSummary;
  createdAt: string;
  updatedAt: string;
}
export interface AssetSearchResult {
  selectionId: string;
  ticker: string;
  name: string;
  market: AssetMarket;
  assetType: AssetType;
  currency: string;
  price: DecimalValue;
}
export interface RegisteredAsset {
  id: string;
  ticker: string;
  name: string;
  market: AssetMarket;
  assetType: AssetType;
  currency: string;
  lastQuote: DecimalValue;
  quotedAt: string;
  createdAt: string;
  updatedAt: string;
}
export interface RegisteredAssetQuote {
  assetId: string;
  ticker: string;
  market: AssetMarket;
  currency: string;
  price: DecimalValue;
  quotedAt: string;
}
export interface Position {
  ticker: string;
  assetName: string;
  market: AssetMarket;
  assetType: AssetType;
  currency: string;
  quantity: DecimalValue;
  averagePrice: DecimalValue;
  custodyCost: DecimalValue;
}
export interface Transaction {
  id: string;
  ticker: string;
  assetName: string;
  market: AssetMarket;
  assetType: AssetType;
  currency: string;
  type: TransactionType;
  status: TransactionStatus;
  transactionDate: string;
  quantity: DecimalValue;
  unitPrice: DecimalValue;
  costs: DecimalValue;
  createdAt: string;
  updatedAt: string;
}
export interface TransactionInput {
  registeredAssetId: string;
  type: TransactionType;
  transactionDate: string;
  quantity: string;
  unitPrice: string;
  costs: string;
}
export type TransactionUpdateInput = Omit<
  TransactionInput,
  "registeredAssetId"
>;
export interface ExchangeRate {
  sourceCurrency: string;
  targetCurrency: string;
  rate: DecimalValue;
  referenceDate: string;
}
export interface ValuationPosition extends Position {
  currentPrice: DecimalValue;
  marketValue: DecimalValue;
  unrealizedGain: DecimalValue;
  returnPercentage: DecimalValue;
  allocationPercentage: DecimalValue;
}
export interface CurrencySummary {
  currency: string;
  investedValue: DecimalValue;
  marketValue: DecimalValue;
  unrealizedGain: DecimalValue;
  returnPercentage: DecimalValue;
}
export interface ConsolidatedSummary {
  baseCurrency: string;
  investedValue: DecimalValue;
  marketValue: DecimalValue;
  totalGain: DecimalValue;
  returnPercentage: DecimalValue;
  exchangeRates: ExchangeRate[];
  historicalExchangeRates: ExchangeRate[];
}
export interface Valuation {
  positions: ValuationPosition[];
  currencySummaries: CurrencySummary[];
  consolidatedSummary?: ConsolidatedSummary;
}
export interface EvolutionPoint {
  date: string;
  investedValue: DecimalValue;
  marketValue: DecimalValue;
}
export interface IncomeCurrencySummary {
  currency: string;
  receivedAmount: DecimalValue;
}
export interface IncomeSummary {
  currencySummaries: IncomeCurrencySummary[];
  baseCurrency: string;
  consolidatedReceivedAmount: DecimalValue;
  exchangeRates: ExchangeRate[];
}
export interface IncomeCandidate {
  candidateId: string;
  ticker: string;
  assetName: string;
  market: AssetMarket;
  assetType: AssetType;
  currency: string;
  type: IncomeEventType;
  source: IncomeEventSource;
  unitAmount: DecimalValue;
  eligibilityDate: string;
  paymentDate: string;
  eligibleQuantity: DecimalValue;
  expectedAmount: DecimalValue;
  confirmable: boolean;
  alreadyRecorded: boolean;
}
export interface IncomeCandidateWarning {
  ticker: string;
  market: AssetMarket;
  code: string;
}
export interface IncomeCandidatesResponse {
  candidates: IncomeCandidate[];
  updatedAt: string;
  stale: boolean;
  warnings: IncomeCandidateWarning[];
}
export interface IncomeEvent {
  id: string;
  ticker: string;
  assetName: string;
  market: AssetMarket;
  assetType: AssetType;
  currency: string;
  type: IncomeEventType;
  source: IncomeEventSource;
  status: IncomeEventStatus;
  eligibilityDate?: string;
  paymentDate: string;
  eligibleQuantity?: DecimalValue;
  unitAmount?: DecimalValue;
  expectedAmount?: DecimalValue;
  receivedAmount: DecimalValue;
  adjustmentReason?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}
export interface AdminUser extends PublicUser {
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
}
