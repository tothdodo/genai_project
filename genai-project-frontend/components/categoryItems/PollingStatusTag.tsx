"use client";

import { useEffect } from "react";
import { StatusTag } from "./StatusTag";
import { getCategoryItemStatusById } from "@/services/categoryItem.service";
import { useCategoryItem } from "@/contexts/CategoryItemContext";

export function PollingStatusTag() {
    const { categoryItem, status, updateStatus } = useCategoryItem();

    useEffect(() => {
        // If it's not processing, don't start polling
        if (status !== "PROCESSING") return;
        const interval = setInterval(async () => {
            try {
                const newStatus = await getCategoryItemStatusById(categoryItem.id);

                if (newStatus !== "PROCESSING") {
                    updateStatus(newStatus);
                    clearInterval(interval);
                }
            } catch (error) {
                console.error("Polling failed:", error);
            }
        }, 3000);

        return () => clearInterval(interval);
    }, [status, categoryItem.id, updateStatus]);
    return <StatusTag status={status} />;
}