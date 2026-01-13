"use client"

import { useRef, useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import { uploadFileUsingPresign } from "@/services/upload.service"

function CircularProgress({ value }: { value: number }) {
    const radius = 36;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference - (value / 100) * circumference;

    return (
        <div className="relative flex items-center justify-center">
            <svg className="w-20 h-20 transform -rotate-90">
                <circle
                    className="text-muted-foreground/20"
                    strokeWidth="8"
                    stroke="currentColor"
                    fill="transparent"
                    r={radius}
                    cx="40"
                    cy="40"
                />
                <circle
                    className="text-primary transition-all duration-300 ease-in-out"
                    strokeWidth="8"
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                    strokeLinecap="round"
                    stroke="currentColor"
                    fill="transparent"
                    r={radius}
                    cx="40"
                    cy="40"
                />
            </svg>
            <span className="absolute text-xs font-bold">{value}%</span>
        </div>
    );
}

export default function UploadPage() {
    const [file, setFile] = useState<File | null>(null)
    const [isUploading, setIsUploading] = useState(false)
    const [progress, setProgress] = useState<number>(0)
    const [error, setError] = useState<string | null>(null)

    const inputRef = useRef<HTMLInputElement>(null)

    function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
        const selectedFile = e.target.files?.[0] ?? null
        setFile(selectedFile)
        setError(null)
        setProgress(0)
    }

    async function handleUpload() {
        if (!file || isUploading) return

        setIsUploading(true)
        setProgress(0)
        setError(null)

        try {
            await uploadFileUsingPresign(
                file,
                {},
                (p) => setProgress(p)
            )
        } catch (err) {
            console.error(err)
            setError("Upload failed")
        } finally {
            setIsUploading(false)
        }
    }

    return (
        <div className="flex justify-center p-10">
            <Card className="w-full max-w-md text-center">
                <CardHeader>
                    <CardTitle>Upload File</CardTitle>
                </CardHeader>

                <CardContent className="space-y-6 flex flex-col items-center">
                    <Input
                        ref={inputRef}
                        type="file"
                        accept=".jpg,.png,.pdf"
                        className="hidden"
                        onChange={handleFileChange}
                    />

                    {isUploading ? (
                        <CircularProgress value={progress} />
                    ) : (
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => inputRef.current?.click()}
                            disabled={isUploading}
                            className="cursor-pointer"
                        >
                            {file ? "Change file" : "Choose file"}
                        </Button>
                    )}

                    <div className="space-y-1">
                        <p className="text-sm font-medium">
                            {file ? file.name : "No file selected"}
                        </p>
                        {file && (
                            <p className="text-xs text-muted-foreground">
                                {(file.size / (1024 * 1024)).toFixed(2)} MB
                            </p>
                        )}
                    </div>

                    {error && (
                        <p className="text-sm text-destructive font-medium">
                            {error}
                        </p>
                    )}

                    <Button
                        className="w-full"
                        onClick={handleUpload}
                        disabled={!file || isUploading}
                    >
                        {isUploading ? "Uploading…" : "Start Upload"}
                    </Button>
                </CardContent>
            </Card>
        </div>
    )
}