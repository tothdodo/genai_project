"use client";

import Flashcards from "@/components/categoryItems/Flashcards";
import { PollingStatusTag } from "@/components/categoryItems/PollingStatusTag";
import UploadGenerationSection from "@/components/categoryItems/UploadGenerationSection";
import { Card } from "@/components/ui/card";
import Link from "next/link";
import { CategoryItemDetails } from "@/types/categoryItem";
import { CategoryItemProvider, useCategoryItem } from "@/contexts/CategoryItemContext";
import Summary from "./Summary";

export default function CategoryItemClient({ initialItem }: { initialItem: CategoryItemDetails }) {
    return (
        <CategoryItemProvider initialItem={initialItem}>
            <div className="container mx-auto flex flex-col">
                <CategoryItemContent />
            </div>
        </CategoryItemProvider>
    );
}

function CategoryItemContent() {
    const { categoryItem } = useCategoryItem();

    return (
        <div className="container mx-auto flex flex-col">
            <Link href={`/category/${categoryItem.category.id}`} className="text-lg text-primary underline inline-block mb-6">
                {`← ${categoryItem.category.name}`}
            </Link>
            <div className="flex justify-between">
                <div className="flex-1 flex flex-col gap-6 mr-6">
                    <Card className="p-3">
                        <div className="flex justify-between items-center">
                            <h1 className="text-2xl font-semibold tracking-tight">
                                {categoryItem.name}
                            </h1>
                            <PollingStatusTag />
                        </div>
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-muted-foreground">
                                Description
                            </p>

                            <p className="text-sm text-muted-foreground mt-2 whitespace-pre-wrap">
                                {categoryItem.description ?? "No description provided."}
                            </p>
                        </div>
                    </Card>
                    <Card className="p-3">
                        <Summary />
                    </Card>
                    <Card className="p-3">
                        <Flashcards />
                    </Card>
                </div>
                <Card className="p-3 w-1/3 h-fit">
                    <UploadGenerationSection />
                </Card>
            </div>
        </div>
    )
}