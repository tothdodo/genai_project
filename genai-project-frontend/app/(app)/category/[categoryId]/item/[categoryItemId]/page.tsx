import { getCategoryItemById } from "@/services/categoryItem.service";
import { CategoryItemDetails } from "@/types/categoryItem";
import Link from "next/link";
import { notFound } from "next/navigation";

type Params = Promise<{
    categoryItemId: string;
}>;

export default async function CategoryItemPage(props: { params: Params }) {
    const { categoryItemId } = await props.params;

    const categoryItem: CategoryItemDetails = await getCategoryItemById(Number(categoryItemId));

    if (!categoryItem) return notFound();

    return (
        <div>
            <Link href={`/category/${categoryItem.category.id}`} className="text-sm text-primary underline mb-4 inline-block">
                &larr; Back to Category
            </Link>
            <h1 className="text-2xl font-bold mb-2">{categoryItem.name}</h1>
            <p className="text-muted-foreground mb-4">
                {categoryItem.description
                    ? `This is the details page for category "${categoryItem.description}"`
                    : "No description provided."}
            </p>
        </div>
    );
}