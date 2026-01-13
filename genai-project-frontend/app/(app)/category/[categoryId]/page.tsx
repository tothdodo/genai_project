import { getCategoryById } from "@/services/category.service";
import { Category } from "@/types/Category";
import { notFound } from "next/navigation";

type Params = Promise<{
    categoryId: string;
}>;

export default async function CategoryPage(props: { params: Params }) {
    const { categoryId } = await props.params;

    const category: Category = await getCategoryById(Number(categoryId));

    if (!category) return notFound();

    return (
        <div>
            <h1 className="text-2xl font-bold mb-2">{category.name}</h1>
            <p className="text-muted-foreground mb-4">
                {category.description
                    ? `This is the details page for category "${category.description}"`
                    : "No description provided."}
            </p>
        </div>
    );
}