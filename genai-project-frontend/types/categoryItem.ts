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
    summary: string;
    flashcards: Flashcard[];
};

export type Summary = {
    title: string;
    summary_sections: {
        heading: string;
        content: string;
        subContent: string | null;
    }[];
}

export type Flashcard = {
    id: number;
    question: string;
    answer: string;
}

export type CategoryHeader = {
    id: number;
    name: string;
}