import { CatalogCategory } from '../types/catalog';

const WATCH_IMAGE =
  'https://images.unsplash.com/photo-1523170335258-f5ed11844cae?w=400&q=80';
const BALL_IMAGE =
  'https://images.unsplash.com/photo-1614632537421-662e3a9c2e42?w=400&q=80';

const watchItem = {
  id: 'item-watch',
  title: 'Reloj vintage Longines Vintage 388808',
  price: '$350.800',
  timeRemaining: '20h 12m',
  imageUrl: WATCH_IMAGE,
};

const ballItem = {
  id: 'item-ball',
  title: 'Pelota de fútbol autografiada',
  price: '$120.500',
  timeRemaining: '8h 45m',
  imageUrl: BALL_IMAGE,
};

export const MOCK_HOME_CATEGORIES: CatalogCategory[] = [
  {
    id: 'cat-1',
    name: 'Lote #1',
    description:
      'Colección de relojes vintage y memorabilia deportiva. Piezas en excelente estado, con certificados de autenticidad y embalaje original cuando corresponde.',
    items: [
      { ...watchItem, id: 'c1-i1' },
      { ...ballItem, id: 'c1-i2' },
      { ...watchItem, id: 'c1-i3', title: 'Reloj clásico Omega 1962' },
    ],
  },
  {
    id: 'cat-2',
    name: 'Lote #2',
    description:
      'Artículos firmados y ediciones limitadas de fútbol. Ideal para coleccionistas que buscan piezas únicas de partidos históricos.',
    items: [
      { ...ballItem, id: 'c2-i1' },
      { ...watchItem, id: 'c2-i2' },
      { ...ballItem, id: 'c2-i3', price: '$89.000' },
    ],
  },
  {
    id: 'cat-3',
    name: 'Lote #3',
    description:
      'Mix de accesorios premium y objetos de subasta express. Tiempo limitado para pujar en todos los ítems del lote.',
    items: [
      { ...watchItem, id: 'c3-i1' },
      { ...ballItem, id: 'c3-i2' },
      { ...watchItem, id: 'c3-i3', timeRemaining: '2h 30m' },
    ],
  },
];

export function getLotById(lotId: string): CatalogCategory | undefined {
  return MOCK_HOME_CATEGORIES.find((lot) => lot.id === lotId);
}
