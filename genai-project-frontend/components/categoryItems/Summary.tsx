import { useCategoryItem } from "@/contexts/CategoryItemContext";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "../ui/collapsible";
import React from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import ContentParser from "./ContentParser";

export default function Summary() {
    const [summaryOpen, setSummaryOpen] = React.useState(true);
    const { categoryItem, status } = useCategoryItem();

    return (
        <Collapsible
            open={summaryOpen}
            onOpenChange={(open) => setSummaryOpen(open)}
        >
            <CollapsibleTrigger asChild>
                <div className="flex justify-between items-center cursor-pointer">
                    <h1 className="text-2xl font-semibold tracking-tight">
                        Summary
                    </h1>
                    <ChevronDown
                        className={`h-4 w-4 transition-transform duration-200 ${summaryOpen ? "rotate-180" : ""
                            }`}
                    />
                </div>
            </CollapsibleTrigger>
            <CollapsibleContent className={cn("text-sm text-muted-foreground mt-2 space-y-2")}>
                {
                    status === "PENDING" ?
                        <p>Upload content to create summary and flashcards.</p> :
                        status === "FAILED" ?
                            <p>Failed to generate summary.</p> :
                            status === "COMPLETED" && categoryItem.summary ?
                                <>
                                    {/* <div
                                        className="prose prose-slate max-w-none"
                                        dangerouslySetInnerHTML={{ __html: htmlSummary }}
                                    /> */}
                                    <ContentParser content={categoryItem.summary} />
                                </> :
                                <p>No summary available.</p>
                }
            </CollapsibleContent>
        </Collapsible>
    )
}