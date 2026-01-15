export type Category = {
    id: number;
    name: string;
    description: string;
    createdAt: string;
    categoryItems: CategoryListItem[];
};

export type CategoryListItem = {
    id: number;
    name: string;
    description: string;
    createdAt: string;
}