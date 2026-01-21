"use client";

import { useCategoryItem } from "@/contexts/CategoryItemContext";
import { Separator } from "../ui/separator";
import UploadSection from "../upload/UploadSection";
import StartGeneration from "./StartGeneration";
import React, { useState } from "react";
import { MaterialFile } from "@/types/materialFile";

export type UploadFile = {
    fileName: string;
    type: string;
    status: "uploading" | "completed" | "error";
    progress: number;
};

export default function UploadGenerationSection() {
    const { status, categoryItem } = useCategoryItem();
    const [fileIsUploading, setFileIsUploading] = React.useState(false);

    const [files, setFiles] = useState<MaterialFile[]>(() => {
        return categoryItem.filenames.map((fileName) => ({
            fileName: fileName,
            type: fileName.split(".").pop() || "unknown",
            status: "completed",
            progress: 100,
        }));
    });

    return (
        <>
            <UploadSection
                setFileIsUploading={setFileIsUploading}
                files={files}
                setFiles={setFiles}
            />
            {
                status === "PENDING" &&
                <>
                    <Separator />
                    <StartGeneration
                        disabled={fileIsUploading || files.length === 0}
                    />
                </>
            }
        </>

    )
}