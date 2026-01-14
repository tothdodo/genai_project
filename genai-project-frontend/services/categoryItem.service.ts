import { CLIENT_BACKEND_URI, SERVER_BACKEND_URI, DEFAULT_CATEGORY_ITEM_PATH, HEADERS } from "@/globals";
import { CategoryListItem } from "@/types/category";
import { CategoryItemDetails } from "@/types/categoryItem";

export async function addCategoryItem(name: string, description: string | null = null, categoryId: number): Promise<CategoryListItem> {
    const params = {
        method: "POST",
        headers: HEADERS,
        body: JSON.stringify({ name, description, categoryId }),
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

    const data: CategoryListItem = await res.json();

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