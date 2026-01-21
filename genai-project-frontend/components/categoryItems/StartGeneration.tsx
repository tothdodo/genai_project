"use client";

import React from "react";
import LargeButton from "../buttons/LargeButton"
import { ApproveStartGenerationDialog } from "./ApproveStartGenerationDialog";
import { startGeneration } from "@/services/categoryItem.service";
import { useCategoryItem } from "@/contexts/CategoryItemContext";

export default function StartGeneration({ disabled }: { disabled: boolean }) {
    const { categoryItem, updateCategoryItem } = useCategoryItem();
    const [dialogOpen, setDialogOpen] = React.useState(false);
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState<string | null>(null);

    async function handleCreate() {
        setDialogOpen(false);
        try {
            setLoading(true);
            await startGeneration(categoryItem.id);
            updateCategoryItem("status", "PROCESSING");
        } catch (error) {
            console.error("Error starting generation:", error);
            setError("Failed to start generation. Please try again.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div>
            {error && <p className="text-red-500 mt-2">{error}</p>}
            <LargeButton
                onClick={() => setDialogOpen(true)}
                additionalClassName="w-full"
                disabled={disabled || loading}
            >
                Generate Summary & Flashcards
            </LargeButton>
            <ApproveStartGenerationDialog
                open={dialogOpen}
                onOpenChange={setDialogOpen}
                onApproved={handleCreate}
            />
        </div>
    )
}