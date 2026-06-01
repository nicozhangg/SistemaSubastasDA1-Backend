export type CatalogCardItem = {
  id: string;
  title: string;
  price: string;
  timeRemaining: string;
  imageUrl: string;
};

export type CatalogCategory = {
  id: string;
  name: string;
  description: string;
  items: CatalogCardItem[];
};
