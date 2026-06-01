/**
 * BidUp — Tipos globales de la aplicación
 */

// ── Auth ─────────────────────────────────────────────────
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  dni: string;
  phone?: string;
  avatarUrl?: string;
  status: 'pending' | 'approved' | 'rejected';
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isGuest: boolean;
  isAuthenticated: boolean;
}

// ── Auction ──────────────────────────────────────────────
export interface Auction {
  id: string;
  title: string;
  description: string;
  imageUrl: string;
  startDate: string;
  endDate: string;
  startingPrice: number;
  currentPrice: number;
  status: 'upcoming' | 'active' | 'finished';
}

export interface AuctionDetail extends Auction {
  images: string[];
  category: string;
  seller: {
    id: string;
    name: string;
  };
  totalBids: number;
}

// ── Bid ──────────────────────────────────────────────────
export interface Bid {
  id: string;
  auctionId: string;
  userId: string;
  amount: number;
  createdAt: string;
}

// ── Catalog ──────────────────────────────────────────────
export interface CatalogItem {
  id: string;
  title: string;
  description: string;
  images: string[];
  category: string;
  estimatedValue: number;
}

// ── Payment Methods ──────────────────────────────────────
export interface PaymentMethod {
  id: string;
  type: 'card' | 'bank_account' | 'check';
  label: string;
  lastFourDigits?: string;
}

export interface CardPayment {
  cardNumber: string;
  cardHolder: string;
  expirationDate: string;
  cvv: string;
}

export interface CheckPayment {
  bankName: string;
  checkNumber: string;
  amount: number;
}

// ── Address ──────────────────────────────────────────────
export interface Address {
  id: string;
  street: string;
  number: string;
  city: string;
  state: string;
  zipCode: string;
  isDefault: boolean;
}

// ── Purchase ─────────────────────────────────────────────
export interface Purchase {
  id: string;
  auctionId: string;
  itemTitle: string;
  finalPrice: number;
  purchaseDate: string;
  status: 'pending_payment' | 'paid' | 'shipped' | 'delivered';
}

// ── Consignment ──────────────────────────────────────────
export interface ConsignmentItem {
  id: string;
  title: string;
  description: string;
  images: string[];
  status: 'submitted' | 'under_review' | 'accepted' | 'rejected';
}

// ── Metrics ──────────────────────────────────────────────
export interface Stats {
  totalAuctions: number;
  totalBids: number;
  totalWins: number;
  totalSpent: number;
}

export interface Fine {
  id: string;
  reason: string;
  amount: number;
  status: 'pending' | 'paid';
  createdAt: string;
}

// ── Chat ─────────────────────────────────────────────────
export interface ChatConversation {
  id: string;
  participantName: string;
  lastMessage: string;
  lastMessageDate: string;
  unreadCount: number;
}

export interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  createdAt: string;
}

// ── Navigation ───────────────────────────────────────────
export type RootStackParamList = {
  Auth: undefined;
  Main: undefined;
  AuctionDetail: { auctionId: string };
  ConfirmBid: { auctionId: string; amount: number };
};

export type HomeStackParamList = {
  HomeMain: undefined;
  UploadItem: { returnTo?: 'home' | 'myAuctions' } | undefined;
  ItemUploaded: { returnTo?: 'home' | 'myAuctions' } | undefined;
  ChatList: undefined;
  ChatDetail: { conversationId: string };
  LoginWall: undefined;
  LotDetail: { lotId: string };
};

export type ProfileStackParamList = {
  ProfileMain: undefined;
  EditUsername: undefined;
  EditPassword: undefined;
  AddressList: undefined;
  AddAddress: { addressId?: string } | undefined;
  PaymentMethods: undefined;
  AddCard: undefined;
  AddCheck: undefined;
};

export type AuthStackParamList = {
  Splash: undefined;
  Access: undefined;
  Login: undefined;
  ForgotPassword: undefined;
  ForgotPasswordSent: undefined;
  RegisterStep1: undefined;
  RegisterStep2: undefined;
  RegisterStep3: undefined;
  PendingApproval: undefined;
};

export type MainTabParamList = {
  Home: undefined;
  MyBids: undefined;
  MyAuctions: undefined;
  Profile: undefined;
};

export type MyAuctionsStackParamList = {
  MyAuctionsMain: undefined;
  UploadItem: { returnTo?: 'home' | 'myAuctions' } | undefined;
  ItemUploaded: { returnTo?: 'home' | 'myAuctions' } | undefined;
};
