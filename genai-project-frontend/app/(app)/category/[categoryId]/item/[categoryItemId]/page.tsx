import Flashcards from "@/components/categoryItems/Flashcards";
import { Card } from "@/components/ui/card";
import UploadSection from "@/components/upload/UploadSection";
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

    console.log("Category Item Details:", categoryItem);

    return (
        <div className="container mx-auto flex flex-col">
            <Link href={`/category/${categoryItem.category.id}`} className="text-lg text-primary underline inline-block mb-6">
                {`← ${categoryItem.category.name}`}
            </Link>
            <div className="flex justify-between">
                <div className="flex-1 flex flex-col gap-6 mr-6">
                    <Card className="p-3">
                        <div>
                            <h1 className="text-2xl font-semibold tracking-tight">
                                {categoryItem.name}
                            </h1>
                        </div>
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-muted-foreground">
                                Description
                            </p>

                            <p className="text-sm leading-relaxed text-foreground whitespace-pre-line">
                                {categoryItem.description ?? "No description provided."}
                            </p>
                        </div>
                    </Card>
                    <Card className="p-3">
                        <div>
                            <h1 className="text-2xl font-semibold tracking-tight">
                                Summary
                            </h1>
                            <p className="text-sm text-muted-foreground mt-2">
                                No summary available. Upload content to create summary and flashcards.
                            </p>
                        </div>
                    </Card>
                    <Card className="p-3">
                        <Flashcards />
                    </Card>
                </div>
                <Card className="p-3 w-1/3 h-fit">
                    <UploadSection
                        categoryItemId={categoryItem.id}
                        initFiles={categoryItem.filenames} />
                </Card>
            </div>
        </div>
    );
}