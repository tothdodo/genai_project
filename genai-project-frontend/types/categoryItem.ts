export type CategoryItemDetails = {
    id: number;
    name: string;
    description: string;
    createdAt: string;
    category: CategoryHeader;
};

export type CategoryHeader = {
    id: number;
    name: string;
}