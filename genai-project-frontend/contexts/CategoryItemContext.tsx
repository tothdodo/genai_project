"use client";

import { CategoryItemDetails } from "@/types/categoryItem";
import React, { createContext, useContext, useState, ReactNode } from "react";

interface CategoryItemContextType {
    categoryItem: CategoryItemDetails;
    status: string;
    updateCategoryItem: <K extends keyof CategoryItemDetails>(key: K, value: CategoryItemDetails[K]) => void;
}

const CategoryItemContext = createContext<CategoryItemContextType | undefined>(undefined);

export function CategoryItemProvider({
    children,
    initialItem
}: {
    children: ReactNode;
    initialItem: CategoryItemDetails;
}) {
    const [categoryItem, setCategoryItem] = useState(initialItem);

    const updateCategoryItem = <K extends keyof CategoryItemDetails>(
        key: K,
        value: CategoryItemDetails[K]
    ) => {
        setCategoryItem((prev) => {
            if (!prev) return prev;
            return { ...prev, [key]: value };
        });
    };

    return (
        <CategoryItemContext.Provider value={{ categoryItem, updateCategoryItem, status: categoryItem.status }}>
            {children}
        </CategoryItemContext.Provider>
    );
}

// Custom hook for easy access in children
export function useCategoryItem() {
    const context = useContext(CategoryItemContext);
    if (!context) {
        throw new Error("useCategoryItem must be used within a CategoryItemProvider");
    }
    return context;
}