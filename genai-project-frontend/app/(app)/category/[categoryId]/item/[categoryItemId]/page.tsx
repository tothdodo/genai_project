import { CategoryItemDetails } from "@/types/categoryItem";
import { getCategoryItemById } from "@/services/categoryItem.service";
import { notFound } from "next/navigation";
import CategoryItemClient from "@/components/categoryItems/CategoryItemClient";

type Params = Promise<{
    categoryItemId: string;
}>;

export default async function CategoryItemPage(props: { params: Params }) {
    const { categoryItemId } = await props.params;

    const categoryItem: CategoryItemDetails = await getCategoryItemById(Number(categoryItemId));

    if (!categoryItem) return notFound();

    return (
        <CategoryItemClient initialItem={categoryItem} />
    );
}