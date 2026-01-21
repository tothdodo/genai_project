"use client";

import { useEffect } from "react";
import { StatusTag } from "./StatusTag";
import { getCategoryItemGenerationById, getCategoryItemStatusById } from "@/services/categoryItem.service";
import { useCategoryItem } from "@/contexts/CategoryItemContext";
import { StatusInfo } from "@/types/categoryItem";

export function PollingStatusTag() {
    const { categoryItem, status, updateCategoryItem } = useCategoryItem();

    useEffect(() => {
        if (status !== "PROCESSING") return;
        const interval = setInterval(async () => {
            try {
                const newStatusInfo: StatusInfo = await getCategoryItemStatusById(categoryItem.id);

                if (newStatusInfo.status !== "PROCESSING") {
                    updateCategoryItem("status", newStatusInfo.status);
                    if (newStatusInfo.status === "FAILED" && newStatusInfo.failedJobType) {
                        updateCategoryItem("failedJobType", newStatusInfo.failedJobType);
                    } else if (newStatusInfo.status === "COMPLETED") {
                        await getCategoryItemGenerationById(categoryItem.id).then((generation) => {
                            updateCategoryItem("summary", generation.summary);
                            updateCategoryItem("flashcards", generation.flashcards);
                        });
                    }
                    clearInterval(interval);
                }
            } catch (error) {
                console.error("Polling failed:", error);
            }
        }, 3000);

        return () => clearInterval(interval);
    }, [status, categoryItem.id, updateCategoryItem]);
    return <StatusTag status={status} failedJobType={categoryItem.failedJobType} />;
}