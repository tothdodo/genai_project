"use client";

import { CategoryItemDetails } from "@/types/categoryItem";
import React, { createContext, useContext, useState, ReactNode } from "react";

interface CategoryItemContextType {
    categoryItem: CategoryItemDetails;
    status: string;
    updateStatus: (newStatus: string) => void;
    // You can add more update functions here later (e.g., updateSummary)
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

    const updateStatus = (newStatus: string) => {
        setCategoryItem((prev) => ({ ...prev, status: newStatus }));
    };

    return (
        <CategoryItemContext.Provider value={{ categoryItem, status: categoryItem.status, updateStatus }}>
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