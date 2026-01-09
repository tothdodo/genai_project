import { BACKEND_URI, DEFAULT_BUCKET_PATH, HEADERS } from "@/globals"
import { FileInfo } from "@/types/FileInfo"
import { UploadFile } from "@/types/UploadFile"

/* ================================
   Presign request
================================ */
export async function getPresignedUploadUrl(
    file: File,
    hash: string
): Promise<FileInfo> {
    const payload: FileInfo = {
        fileName: `${hash}_${file.name}`,
        originalFileName: file.name,
    }

    const params = {
        method: "POST",
        headers: HEADERS,
        body: JSON.stringify(payload),
    };
    console.log(payload)

    const res = await fetch(
        `${BACKEND_URI}${DEFAULT_BUCKET_PATH}/upload`,
        params
    )

    if (!res.ok) {
        throw new Error("Failed to get presigned upload URL")
    }

    return res.json()
}

/* ================================
   Upload to presigned URL (Updated for Progress)
================================ */
export async function uploadToPresignedUrl(
    file: File,
    url: string,
    method: string = 'PUT',
    onProgress?: (percent: number) => void // Add this callback
): Promise<void> {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();

        xhr.open(method, url);

        // Set the content type
        xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');

        // Monitor progress
        if (onProgress && xhr.upload) {
            xhr.upload.onprogress = (event) => {
                if (event.lengthComputable) {
                    const percentComplete = Math.round((event.loaded / event.total) * 100);
                    onProgress(percentComplete);
                }
            };
        }

        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                resolve();
            } else {
                reject(new Error(`Upload failed with status ${xhr.status}: ${xhr.responseText}`));
            }
        };

        xhr.onerror = () => reject(new Error('Network error during upload'));

        xhr.send(file);
    });
}

/* ================================
   Upload completion callback
================================ */
export async function notifyUploadComplete(
    file: File,
    hash: string
): Promise<FileInfo> {
    const payload: FileInfo = {
        fileName: `${hash}_${file.name}`,
        originalFileName: file.name,
    }

    const res = await fetch(
        `${BACKEND_URI}${DEFAULT_BUCKET_PATH}/upload`,
        {
            method: "PUT",
            headers: HEADERS,
            body: JSON.stringify(payload),
        }
    )

    if (!res.ok) {
        throw new Error("Failed to notify backend of completion")
    }

    return res.json()
}

/* ================================
   SHA-256 hashing
================================ */
export async function hashFile(file: File): Promise<string> {
    try {
        const buffer = await file.arrayBuffer()
        const hashBuffer = await crypto.subtle.digest("SHA-256", buffer)
        const hashArray = Array.from(new Uint8Array(hashBuffer))

        return hashArray
            .map((b) => b.toString(16).padStart(2, "0"))
            .join("")
    } catch (err) {
        console.error("hashFile failed", err)
        throw err
    }
}

/* ================================
   Full presigned upload flow (Updated)
================================ */
export async function uploadFileUsingPresign(
    file: File,
    uploadFile: UploadFile,
    onProgress?: (percent: number) => void
): Promise<void> {
    const hash = await hashFile(file);
    uploadFile.hash = hash;

    const info = await getPresignedUploadUrl(file, hash);

    if (!info.presignedURL) {
        throw new Error("Presign request failed: missing presignedURL");
    }

    // Pass the progress callback through
    await uploadToPresignedUrl(file, info.presignedURL, "PUT", onProgress);

    // Optional: Notify backend completion after successful upload
    await notifyUploadComplete(file, hash);
}
