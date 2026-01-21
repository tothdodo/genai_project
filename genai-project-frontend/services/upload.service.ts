import { CLIENT_BACKEND_URI, DEFAULT_BUCKET_PATH, HEADERS } from "@/globals"
import { FileInfo } from "@/types/fileInfo"
import { UploadFile } from "@/types/uploadFile"

/* ================================
   Presign request
================================ */
export async function getPresignedUploadUrl(
    file: File,
    hash: string,
    timeStampString: string,
    categoryItemId: number
): Promise<FileInfo> {
    const payload: FileInfo = {
        fileName: `${timeStampString}_${hash}_${file.name}`,
        originalFileName: file.name,
        categoryItemId: categoryItemId
    }

    const params = {
        method: "POST",
        headers: HEADERS,
        body: JSON.stringify(payload),
    };

    const url = `${CLIENT_BACKEND_URI}${DEFAULT_BUCKET_PATH}/upload`;
    console.log("Calling: ", url);

    const res = await fetch(
        url,
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
    onProgress?: (percent: number) => void
): Promise<void> {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();

        const parsed = new URL(url);
        const proxiedUrl = `${process.env.NEXT_PUBLIC_FRONTEND_URI}/minio${parsed.pathname}${parsed.search}`;
        console.log("Calling: ", proxiedUrl);
        xhr.open(method, proxiedUrl);

        xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');

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
    hash: string,
    timeStampString: string,
    categoryItemId: number
): Promise<FileInfo> {
    const payload: FileInfo = {
        fileName: `${timeStampString}_${hash}_${file.name}`,
        originalFileName: file.name,
        categoryItemId: categoryItemId
    }
    const url = `${CLIENT_BACKEND_URI}${DEFAULT_BUCKET_PATH}/upload`;
    console.log("Calling: ", url);

    const res = await fetch(
        url,
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
    categoryItemId: number,
    onProgress?: (percent: number) => void
): Promise<void> {
    const hash = await hashFile(file);
    uploadFile.hash = hash;

    const timeStampString = Date.now().toString();

    const info = await getPresignedUploadUrl(file, hash, timeStampString, categoryItemId);

    if (!info.presignedURL) {
        throw new Error("Presign request failed: missing presignedURL");
    }

    await uploadToPresignedUrl(file, info.presignedURL, "PUT", onProgress);

    await notifyUploadComplete(file, hash, timeStampString, categoryItemId);
}