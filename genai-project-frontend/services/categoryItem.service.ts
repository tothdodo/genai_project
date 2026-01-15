import { CLIENT_BACKEND_URI, SERVER_BACKEND_URI, DEFAULT_CATEGORY_ITEM_PATH, HEADERS } from "@/globals";
import { CategoryItem, CategoryItemDetails } from "@/types/categoryItem";

export async function addCategoryItem(name: string, description: string | null = null, categoryId: number): Promise<CategoryItem> {
    const params = {
        method: "POST",
        headers: HEADERS,
        body: JSON.stringify({ name, description: description === "" ? null : description, categoryId }),
    };
    const url = `${CLIENT_BACKEND_URI}${DEFAULT_CATEGORY_ITEM_PATH}`;
    console.log("Calling: ", url);

    const res = await fetch(
        url,
        params
    );
    if (!res.ok) {
        throw new Error("Failed to create category item");
    }

    const data: CategoryItem = await res.json();

    return data;
}

export async function getCategoryItemById(categoryItemId: number): Promise<CategoryItemDetails> {
    const params = {
        method: "GET",
        headers: HEADERS,
    };
    const url = `${SERVER_BACKEND_URI}${DEFAULT_CATEGORY_ITEM_PATH}/${categoryItemId}`;
    console.log("Calling: ", url);

    const res = await fetch(
        url,
        params
    );

    if (!res.ok) {
        throw new Error("Failed to get category item");
    }

    const data: CategoryItemDetails = await res.json();
    return data;
}

export async function getCategoryItemStatusById(categoryItemId: number): Promise<string> {
    const params = {
        method: "GET",
        headers: HEADERS,
    };
    const url = `${CLIENT_BACKEND_URI}${DEFAULT_CATEGORY_ITEM_PATH}/${categoryItemId}/status`;
    console.log("Calling: ", url);

    const res = await fetch(url, params);
    if (!res.ok) throw new Error("Failed to get category item status");

    const data = await res.json();
    return data.status;
}

export async function startGeneration(categoryId: number): Promise<void> {
    const params = {
        method: "POST",
        headers: HEADERS,
    };
    const url = `${CLIENT_BACKEND_URI}${DEFAULT_CATEGORY_ITEM_PATH}/${categoryId}/start-generation`;
    console.log("Calling: ", url);

    const res = await fetch(
        url,
        params
    );
    if (!res.ok) {
        throw new Error("Failed to start generation");
    }
}