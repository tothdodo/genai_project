export type CategoryItem = {
    id: number;
    name: string;
    description: string;
    createdAt: string;
    categoryId: number;
};

export type CategoryItemDetails = {
    id: number;
    name: string;
    description: string;
    createdAt: string;
    category: CategoryHeader;
    status: string;
    filenames: string[];
};

export type CategoryHeader = {
    id: number;
    name: string;
}