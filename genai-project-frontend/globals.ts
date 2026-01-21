export const SERVER_BACKEND_URI = process.env.NEXT_PUBLIC_SERVER_BACKEND_URI + "/api/v1"
export const CLIENT_BACKEND_URI = process.env.NEXT_PUBLIC_CLIENT_BACKEND_URI + "/api/v1"
export const DEFAULT_BUCKET_PATH = '/bucket'
export const DEFAULT_CATEGORY_PATH = '/category'
export const DEFAULT_CATEGORY_ITEM_PATH = '/category-item'

export const HEADERS: HeadersInit = {
    "Content-Type": "application/json",
    Accept: "application/json",
}