"use client"

import { useRef, useState } from "react"
import { Input } from "@/components/ui/input"
import { uploadFileUsingPresign } from "@/services/upload.service"
import SmallButton from "../buttons/SmallButton"
import { CircularProgress } from "./CircularProgress"
import {
    CheckIcon, CircleX, FileText, FileImage, FileBox, File as FileGeneric
} from "lucide-react"

const getFileIcon = (fileName: string) => {
    const extension = fileName.split('.').pop()?.toLowerCase();

    switch (extension) {
        case 'pdf':
            return <FileText className="h-5 w-5 text-red-500" />;
        case 'png':
        case 'jpg':
        case 'jpeg':
            return <FileImage className="h-5 w-5 text-blue-500" />;
        case 'ppt':
        case 'pptx':
            return <FileBox className="h-5 w-5 text-orange-500" />;
        default:
            return <FileGeneric className="h-5 w-5 text-gray-500" />;
    }
};

type Props = {
    categoryItemId: number;
    initFiles: string[];
};

type UploadFile = {
    fileName: string;
    type: string;
    status: "uploading" | "completed" | "error";
    progress: number;
}

export default function UploadSection({
    categoryItemId,
    initFiles
}: Props) {
    const [error, setError] = useState<string | null>(null)

    const [files, setFiles] = useState<UploadFile[]>(() => {
        // 1. Transform the props as before
        const initialFromProps: UploadFile[] = initFiles.map((name) => ({
            fileName: name,
            type: name.split(".").pop() || "unknown",
            progress: 100,
            status: "completed",
        }));

        // 2. Add manual test records for different states
        const testRecords: UploadFile[] = [
            // {
            //     fileName: "vacation_photosdasdasdasdasdasdasdasdas.jpg",
            //     type: "jpg",
            //     status: "uploading",
            //     progress: 45,
            // },
            // {
            //     fileName: "large_dataset.csv",
            //     type: "csv",
            //     status: "error",
            //     progress: 12,
            // },
            // {
            //     fileName: "presentation_draft.pptx",
            //     type: "pptx",
            //     status: "uploading",
            //     progress: 100,
            // }
        ];

        return [...initialFromProps, ...testRecords];
    });

    const inputRef = useRef<HTMLInputElement>(null)

    async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
        const selectedFile = e.target.files?.[0] ?? null
        if (!selectedFile) {
            setError("");
            return;
        }
        const newFile: UploadFile = {
            fileName: selectedFile.name,
            type: selectedFile.name.split(".").pop() || "unknown",
            status: "uploading",
            progress: 0,
        }
        if (files.some(f => f.fileName === newFile.fileName)) {
            setError("A file with this name has already been uploaded.")
            return
        }
        setFiles((prevFiles) => [...prevFiles, newFile])
        setError(null)

        try {
            if (!selectedFile) throw new Error("No file selected.")
            await uploadFileUsingPresign(
                selectedFile,
                {},
                categoryItemId,
                (p) => {
                    newFile.progress = p
                    setFiles((prevFiles) =>
                        prevFiles.map((file) =>
                            file.fileName === newFile.fileName ? newFile : file
                        )
                    )
                }
            )
        } catch (err) {
            console.error(err)
            setError("Upload failed. Please try again.")
        } finally {
            newFile.status = newFile.progress === 100 ? "completed" : "error"
            setFiles((prevFiles) =>
                prevFiles.map((file) =>
                    file.fileName === newFile.fileName ? newFile : file
                )
            )
        }
    }

    return (
        <div>
            <h1 className="text-2xl font-semibold tracking-tight">
                Material Upload
            </h1>
            {
                files.length === 0 ?
                    <p className="text-sm text-muted-foreground mt-2">
                        No files uploaded yet.
                    </p> :
                    <ul className="mt-4 space-y-2">
                        {files.map((file, index) => (
                            <li key={index} className="flex items-center justify-between">
                                <div className="flex items-center max-w-4/5">
                                    <div className="flex items-center justify-start w-8 h-8 pr-3">
                                        {getFileIcon(file.fileName)}
                                    </div>
                                    <span className="truncate font-medium">
                                        {file.fileName}
                                    </span>
                                </div>
                                <span className="text-sm text-muted-foreground">
                                    {file.status === "uploading" &&
                                        <div className="w-[22px] h-[22px]"><CircularProgress value={file.progress} /></div>}
                                    {file.status === "completed" && <CheckIcon className="text-green-500" />}
                                    {file.status === "error" && <CircleX className="text-red-500" />}
                                </span>
                            </li>
                        ))}
                    </ul>
            }
            <Input
                ref={inputRef}
                type="file"
                accept=".jpg,.png,.pdf"
                className="hidden"
                onChange={handleFileChange}
            />

            {error && (
                <p className="mt-4 text-center text-sm text-destructive font-medium">
                    {error}
                </p>
            )}

            {/* <CircularProgress value={progress} /> */}
            <div className="flex justify-end mt-4">
                <SmallButton
                    onClick={() => inputRef.current?.click()}
                    additionalClasses="w-fit"
                >
                    + Upload New File
                </SmallButton>
            </div>
        </div>
    )
}