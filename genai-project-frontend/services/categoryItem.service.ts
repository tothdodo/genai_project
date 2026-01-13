import { BACKEND_URI, DEFAULT_CATEGORY_ITEM_PATH, HEADERS } from "@/globals";
import { CategoryListItem } from "@/types/Category";
import { CategoryItemDetails } from "@/types/categoryItem";

export async function addCategoryItem(name: string, description: string = "", categoryId: number): Promise<CategoryListItem> {
    const params = {
        method: "POST",
        headers: HEADERS,
        body: JSON.stringify({ name, description, categoryId }),
    };
    const res = await fetch(
        `${BACKEND_URI}${DEFAULT_CATEGORY_ITEM_PATH}`,
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

    const res = await fetch(
        `${BACKEND_URI}${DEFAULT_CATEGORY_ITEM_PATH}/${categoryItemId}`,
        params
    );

    if (!res.ok) {
        throw new Error("Failed to get category item");
    }

    const data: CategoryItemDetails = await res.json();
    return data;
}