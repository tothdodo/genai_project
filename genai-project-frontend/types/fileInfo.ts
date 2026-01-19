import { HTTP_METHOD } from "next/dist/server/web/http"

export interface FileInfo {
    fileName: string
    originalFileName: string
    categoryItemId: number
    presignedURL?: string
    method?: HTTP_METHOD
}