"use client";

import { useEffect } from "react";
import { StatusTag } from "./StatusTag";
import { getCategoryItemStatusById } from "@/services/categoryItem.service";
import { useCategoryItem } from "@/contexts/CategoryItemContext";
import { useRouter } from "next/navigation";

export function PollingStatusTag() {
    const { categoryItem, status, updateStatus } = useCategoryItem();
    const router = useRouter();

    useEffect(() => {
        if (status !== "PROCESSING") return;
        const interval = setInterval(async () => {
            try {
                const newStatus = await getCategoryItemStatusById(categoryItem.id);

                if (newStatus !== "PROCESSING") {
                    updateStatus(newStatus);
                    clearInterval(interval);
                    router.refresh();
                }
            } catch (error) {
                console.error("Polling failed:", error);
            }
        }, 3000);

        return () => clearInterval(interval);
    }, [status, categoryItem.id, updateStatus, router]);
    return <StatusTag status={status} />;
}