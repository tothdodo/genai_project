"use client";

import LargeButton from "../buttons/LargeButton"

export default function Flashcards() {
    return (
        <div>
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-semibold tracking-tight">
                    Flashcards
                </h1>
                <LargeButton
                    onClick={() => { alert("Flashcard game coming soon!") }}
                >
                    Start Flashcard Game
                </LargeButton>
            </div>
            <p className="text-sm text-muted-foreground mt-2">
                Review flashcards to reinforce your learning.
            </p>
            <p className="text-sm text-muted-foreground mt-2">
                No flashcards available. Upload content to create summary and flashcards.
            </p>
        </div>
    )
}