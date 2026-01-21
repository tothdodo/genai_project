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
    failedJobType?: string;
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

export type StatusInfo = {
    status: string;
    failedJobType?: string;
}

export type Generation = {
    summary: string;
    flashcards: Flashcard[];
}