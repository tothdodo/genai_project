"use client";

import { useCategoryItem } from "@/contexts/CategoryItemContext";
import React from "react";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "../ui/collapsible";
import { ChevronDown, Eye, EyeOff, LayoutGrid, Shuffle } from "lucide-react";
import { cn } from "@/lib/utils";
import { Flashcard } from "@/types/categoryItem";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Separator } from "@radix-ui/react-separator";

export default function Flashcards() {
    const [flashcardsOpen, setFlashcardsOpen] = React.useState(true);
    const { categoryItem, status } = useCategoryItem();

    return (
        <Collapsible
            open={flashcardsOpen}
            onOpenChange={(open) => setFlashcardsOpen(open)}
        >
            <CollapsibleTrigger asChild>
                <div className="flex justify-between items-center cursor-pointer">
                    <h1 className="text-2xl font-semibold tracking-tight">
                        Flashcards
                    </h1>
                    <ChevronDown
                        className={`h-4 w-4 transition-transform duration-200 ${flashcardsOpen ? "rotate-180" : ""
                            }`}
                    />
                </div>
            </CollapsibleTrigger>
            <CollapsibleContent className={cn("text-sm text-muted-foreground mt-2 space-y-2")}>
                {
                    status === "PENDING" ?
                        <p>Upload content to create summary and flashcards.</p> :
                        status === "FAILED" ?
                            <p>Failed to generate flashcards.</p> :
                            status === "COMPLETED" && categoryItem.flashcards.length > 0 ?
                                <FlashcardGrid flashcards={categoryItem.flashcards} /> :
                                <p>No flashcards available.</p>
                }
            </CollapsibleContent>
        </Collapsible>
    )
}

function FlashcardGrid(
    { flashcards }: { flashcards: Flashcard[] }
) {
    const [cards, setCards] = React.useState<Flashcard[]>([...flashcards]);
    const [revealedIds, setRevealedIds] = React.useState<number[]>([]);
    const [version, setVersion] = React.useState(0);

    const showAll = () => setRevealedIds(cards.map(c => c.id));

    const hideAll = () => setRevealedIds([]);

    const toggleCard = (id: number) => {
        setRevealedIds(prev =>
            prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
        );
    };

    const randomize = () => {
        const shuffled = [...cards];
        for (let i = shuffled.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
        }

        setCards(shuffled);
        setVersion(v => v + 1);
    };

    return (
        <div className="container mx-auto p-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8 p-4 bg-card border rounded-xl shadow-sm">
                <div className="flex items-center gap-2">
                    <div className="bg-primary/10 p-2 rounded-lg">
                        <LayoutGrid className="text-primary" size={20} />
                    </div>
                    <div>
                        <h2 className="text-sm font-semibold">Flashcard Controls</h2>
                        <p className="text-xs text-muted-foreground">{cards.length} cards loaded</p>
                    </div>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                    <Button variant="outline" size="sm" onClick={showAll} className="gap-2 cursor-pointer">
                        <Eye size={14} /> Show All
                    </Button>
                    <Button variant="outline" size="sm" onClick={hideAll} className="gap-2 cursor-pointer">
                        <EyeOff size={14} /> Hide All
                    </Button>
                    <Separator orientation="vertical" className="hidden md:block h-8 mx-2" />
                    <Button variant="default" size="sm" onClick={randomize} className="gap-2 cursor-pointer">
                        <Shuffle size={14} /> Randomize
                    </Button>
                </div>
            </div>
            <div key={version} className="grid grid-cols-1 xl:grid-cols-2 gap-6">
                {cards
                    .map((card) => (
                        <FlashcardItem
                            key={card.id}
                            flashcard={card}
                            isRevealed={revealedIds.includes(card.id)}
                            onToggle={() => toggleCard(card.id)}
                        />
                    ))}
            </div>
        </div>
    );
}

const FlashcardItem = ({
    flashcard,
    isRevealed,
    onToggle
}: {
    flashcard: Flashcard,
    isRevealed: boolean,
    onToggle: () => void
}) => {
    return (
        <Card className="flex flex-col h-full transition-all duration-300 hover:shadow-lg border-muted-foreground/20">
            <CardHeader className="pb-2">
                <CardTitle className="text-lg font-semibold leading-tight min-h-[3rem]">
                    {flashcard.question}
                </CardTitle>
            </CardHeader>

            <CardContent className="flex-grow">
                <div
                    className={`rounded-md p-4 min-h-[100px] flex items-center justify-center text-center transition-colors duration-300
            ${isRevealed ? 'bg-secondary/50 text-secondary-foreground font-semibold' : 'bg-muted/30 text-muted-foreground italic text-sm'}
          `}
                >
                    {isRevealed ? (
                        flashcard.answer
                    ) : (
                        <span className="opacity-50">Answer hidden</span>
                    )}
                </div>
            </CardContent>

            <CardFooter>
                <Button
                    variant={isRevealed ? "outline" : "default"}
                    className="w-full gap-2 cursor-pointer"
                    onClick={onToggle}
                >
                    {isRevealed ? (
                        <>
                            <EyeOff size={16} /> Hide Answer
                        </>
                    ) : (
                        <>
                            <Eye size={16} /> Reveal Answer
                        </>
                    )}
                </Button>
            </CardFooter>
        </Card>
    );
};