"use client"

import { Dispatch, SetStateAction, useEffect, useRef, useState } from "react"
import { Input } from "@/components/ui/input"
import { uploadFileUsingPresign } from "@/services/upload.service"
import SmallButton from "../buttons/SmallButton"
import { CircularProgress } from "./CircularProgress"
import {
    CheckIcon, CircleX, FileText, FileImage, FileBox, File as FileGeneric
} from "lucide-react"
import { useCategoryItem } from "@/contexts/CategoryItemContext"
import { MaterialFile } from "@/types/materialFile"

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
    files: MaterialFile[];
    setFiles: Dispatch<SetStateAction<MaterialFile[]>>;
    setFileIsUploading: (uploading: boolean) => void;
};

export default function UploadSection({
    files, setFiles,
    setFileIsUploading
}: Props) {
    const { categoryItem, status } = useCategoryItem();
    const [error, setError] = useState<string | null>(null);

    const inputRef = useRef<HTMLInputElement>(null);

    async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
        const selectedFile = e.target.files?.[0] ?? null
        if (!selectedFile) {
            setError("");
            return;
        }
        const newFile: MaterialFile = {
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
                categoryItem.id,
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

    useEffect(() => {
        const uploading = files.some(file => file.status === "uploading");
        setFileIsUploading(uploading);
    }, [files, setFileIsUploading]);

    return (
        <div>
            <h1 className="text-2xl font-semibold tracking-tight">
                Materials
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
            {
                status === "PENDING" &&
                <div className="flex justify-end mt-4">
                    <SmallButton
                        onClick={() => inputRef.current?.click()}
                        additionalClasses="w-fit"
                    >
                        + Upload New Material
                    </SmallButton>
                </div>
            }
        </div>
    )
}