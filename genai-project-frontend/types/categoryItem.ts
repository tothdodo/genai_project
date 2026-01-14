export type CategoryItemDetails = {
    id: number;
    name: string;
    description: string;
    createdAt: string;
    category: CategoryHeader;
    filenames: string[];
};

export type CategoryHeader = {
    id: number;
    name: string;
}