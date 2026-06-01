/**
 * BidUp — Datos de prueba para Mis Pujas y Mis Subastas
 */

export interface MockBidItem {
  id: string;
  title: string;
  imageUrl: string;
  timeRemaining: string;
  currentPrice: string;
  myBid: string;
  status: 'winning' | 'losing' | 'won' | 'lost';
}

export interface MockAuctionItem {
  id: string;
  title: string;
  imageUrl: string;
  timeRemaining: string;
  currentPrice: string;
  status: 'soon' | 'finished' | 'canceled';
  moderationStatus: 'pending' | 'approved_pending_lot' | 'published' | 'rejected';
  rejectionReason?: string;
}

const WATCH_IMAGE = 'https://images.unsplash.com/photo-1523170335258-f5ed11844cae?w=400&q=80';

export const MOCK_BIDS: MockBidItem[] = [
  {
    id: 'bid-1',
    title: 'Reloj vintage Longines Vintage 366908',
    imageUrl: WATCH_IMAGE,
    timeRemaining: '20H 53M',
    currentPrice: '$350.800',
    myBid: '$300.000',
    status: 'losing',
  },
  {
    id: 'bid-2',
    title: 'Reloj vintage Longines Vintage 366908',
    imageUrl: WATCH_IMAGE,
    timeRemaining: '20H 53M',
    currentPrice: '$350.800',
    myBid: '$350.800',
    status: 'winning',
  },
  {
    id: 'bid-3',
    title: 'Reloj vintage Longines Vintage 366908',
    imageUrl: WATCH_IMAGE,
    timeRemaining: '00H 00M',
    currentPrice: '$350.800',
    myBid: '$350.800',
    status: 'won',
  },
  {
    id: 'bid-4',
    title: 'Reloj vintage Longines Vintage 366908',
    imageUrl: WATCH_IMAGE,
    timeRemaining: '00H 00M',
    currentPrice: '$350.800',
    myBid: '$300.000',
    status: 'lost',
  },
];

export const MOCK_AUCTIONS: MockAuctionItem[] = [
  {
    id: 'auc-1',
    title: 'Reloj vintage Longines Vintage 366908',
    imageUrl: WATCH_IMAGE,
    timeRemaining: '20H 53M',
    currentPrice: '$350.800',
    status: 'soon',
    moderationStatus: 'published',
  },
  {
    id: 'auc-2',
    title: 'Reloj vintage Longines Vintage 366908',
    imageUrl: WATCH_IMAGE,
    timeRemaining: '00H 00M',
    currentPrice: '$350.800',
    status: 'finished',
    moderationStatus: 'published',
  },
  {
    id: 'auc-3',
    title: 'Reloj vintage Longines Vintage 366908',
    imageUrl: WATCH_IMAGE,
    timeRemaining: '00H 00M',
    currentPrice: '$350.800',
    status: 'canceled',
    moderationStatus: 'rejected',
    rejectionReason: 'Las fotos no muestran el estado real del artículo.',
  },
  {
    id: 'auc-4',
    title: 'Cámara Polaroid SX-70',
    imageUrl: WATCH_IMAGE,
    timeRemaining: 'En revisión',
    currentPrice: '$45.000',
    status: 'soon',
    moderationStatus: 'pending',
  },
  {
    id: 'auc-5',
    title: 'Vinilo The Beatles — Abbey Road',
    imageUrl: WATCH_IMAGE,
    timeRemaining: 'Sin lote asignado',
    currentPrice: '$18.500',
    status: 'soon',
    moderationStatus: 'approved_pending_lot',
  },
];
