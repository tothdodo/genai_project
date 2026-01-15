"use client";

import { useCategoryItem } from "@/contexts/CategoryItemContext";
import LargeButton from "../buttons/LargeButton"

export default function Flashcards() {
    const { categoryItem } = useCategoryItem();
    return (
        <div>
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-semibold tracking-tight">
                    Flashcards
                </h1>
                {
                    categoryItem.status === "COMPLETED" &&
                    <LargeButton
                        onClick={() => { alert("Flashcard game coming soon!") }}
                    >
                        Start Flashcard Game
                    </LargeButton>
                }
            </div>
            <p className="text-sm text-muted-foreground mt-2">
                Review flashcards to reinforce your learning.
            </p>
            <div className="text-sm text-muted-foreground mt-6 space-y-2">
                <p>No flashcards available.</p>
                <p>Upload content to create summary and flashcards.</p>
            </div>
        </div>
    )
}