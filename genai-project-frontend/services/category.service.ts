import { CLIENT_BACKEND_URI, SERVER_BACKEND_URI, DEFAULT_CATEGORY_PATH, HEADERS } from "@/globals"
import { Category } from "@/types/category";

export async function getCategoryById(categoryId: number): Promise<Category> {
    const params = {
        method: "GET",
        headers: HEADERS,
    };
    const url = `${SERVER_BACKEND_URI}${DEFAULT_CATEGORY_PATH}/${categoryId}`;
    console.log("Calling: ", url);

    const res = await fetch(
        url,
        params
    )

    if (!res.ok) {
        throw new Error("Failed to get category")
    }

    const data: Category = await res.json();
    return data;
}

export async function getAllCategories(): Promise<Category[]> {
    const params = {
        method: "GET",
        headers: HEADERS,
    };
    const url = `${CLIENT_BACKEND_URI}${DEFAULT_CATEGORY_PATH}/all`;
    console.log("Calling: ", url);

    try {
        const res = await fetch(
            url,
            params
        )

        if (!res.ok) {
            throw new Error("Failed to get categories")
        }

        const data: Category[] = await res.json();

        return data;
    } catch (error) {
        throw error;
    }
}

export async function addCategory(name: string, description: string | null = null): Promise<Category> {
    const params = {
        method: "POST",
        headers: HEADERS,
        body: JSON.stringify({ name, description: description === "" ? null : description }),
    };
    const url = `${CLIENT_BACKEND_URI}${DEFAULT_CATEGORY_PATH}`;
    console.log("Calling: ", url);
    const res = await fetch(
        url,
        params
    );
    if (!res.ok) {
        throw new Error("Failed to create category");
    }

    const data: Category = await res.json();

    return data;
}