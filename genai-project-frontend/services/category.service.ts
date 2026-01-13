import { BACKEND_URI, DEFAULT_CATEGORY_PATH, HEADERS } from "@/globals"
import { Category } from "@/types/Category";

export async function getCategoryById(categoryId: number): Promise<Category> {
    const params = {
        method: "GET",
        headers: HEADERS,
    };

    const res = await fetch(
        `${BACKEND_URI}${DEFAULT_CATEGORY_PATH}/${categoryId}`,
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

    const res = await fetch(
        `${BACKEND_URI}${DEFAULT_CATEGORY_PATH}/all`,
        params
    )

    if (!res.ok) {
        throw new Error("Failed to get categories")
    }

    const data: Category[] = await res.json();

    return data;
}

export async function addCategory(name: string, description: string = ""): Promise<Category> {
    const params = {
        method: "POST",
        headers: HEADERS,
        body: JSON.stringify({ name, description }),
    };
    const res = await fetch(
        `${BACKEND_URI}${DEFAULT_CATEGORY_PATH}`,
        params
    );
    if (!res.ok) {
        throw new Error("Failed to create category");
    }

    const data: Category = await res.json();

    return data;
}