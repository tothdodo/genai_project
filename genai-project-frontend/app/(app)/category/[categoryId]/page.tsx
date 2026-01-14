import { getCategoryById } from "@/services/category.service";
import { Category } from "@/types/category";
import { notFound } from "next/navigation";

type Params = Promise<{
    categoryId: string;
}>;

export default async function CategoryPage(props: { params: Params }) {
    const { categoryId } = await props.params;

    const category: Category = await getCategoryById(Number(categoryId));

    if (!category) return notFound();

    return (
        <div className="space-y-4">
            <div>
                <h1 className="text-2xl font-semibold tracking-tight">
                    {category.name}
                </h1>
            </div>
            <div className="space-y-1">
                <p className="text-sm font-medium text-muted-foreground">
                    Description
                </p>

                <p className="text-sm leading-relaxed text-foreground whitespace-pre-line">
                    {category.description ?? "No description provided."}
                </p>
            </div>
        </div>
    );
}