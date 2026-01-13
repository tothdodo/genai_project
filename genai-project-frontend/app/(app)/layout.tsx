"use client";

import * as React from "react";
import { Card } from "@/components/ui/card";
import { getAllCategories } from "@/services/category.service";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { ChevronDown } from "lucide-react";
import { Category, CategoryListItem } from "@/types/Category";
import { CreateDialog, CreationType } from "@/components/categories/CreateDialog";
import Link from "next/link";
import LoadingCategories from "@/components/categories/LoadingCategories";

export default function AppLayout({
    children,
}: {
    children: React.ReactNode
}) {
    const [categories, setCategories] = React.useState<Category[]>([]);
    const [loading, setLoading] = React.useState(true);
    const [openCategories, setOpenCategories] = React.useState<Record<number, boolean>>({})
    const [dialogOpen, setDialogOpen] = React.useState(false);
    const [creationType, setCreationType] = React.useState<CreationType>(CreationType.CATEGORY);
    const [categoryIdToAddItem, setCategoryIdToAddItem] = React.useState<number | undefined>(undefined);

    const objectName = creationType === CreationType.CATEGORY ? "Category" : "Category Item";

    React.useEffect(() => {
        getAllCategories()
            .then(setCategories)
            .finally(() => setLoading(false));
    }, []);


    return (
        <div className="flex min-h-screen w-full">
            <aside className="w-80 border-r bg-zinc-300 py-6">
                <div className="px-3 py-3">
                    <button
                        onClick={() => {
                            setCreationType(CreationType.CATEGORY);
                            setDialogOpen(true);
                        }}
                        disabled={loading}
                        className="disabled:cursor-not-allowed disabled:bg-gray-400 cursor-pointer w-full rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                    >
                        + Add category
                    </button>
                </div>
                <CreateDialog
                    open={dialogOpen}
                    onOpenChange={setDialogOpen}
                    creationType={creationType}
                    objectName={objectName}
                    categoryId={categoryIdToAddItem}
                    onCreated={(created) => {
                        if (creationType === CreationType.CATEGORY) {
                            setCategories((prev) => [created as Category, ...prev]);
                        } else {
                            setCategories((prev) => {
                                return prev.map((category) => {
                                    if (category.id === categoryIdToAddItem) {
                                        return {
                                            ...category,
                                            categoryItems: [...(category.categoryItems || []), created as CategoryListItem],
                                        };
                                    }
                                    return category;
                                })
                            });
                        }

                    }}
                />
                <ScrollArea className="h-full px-3 py-4">
                    <div className="space-y-4">
                        {loading && (
                            <LoadingCategories />
                        )}

                        {categories.map((category) => (
                            <Card key={category.id} className="p-3 shadow-none">
                                <Collapsible
                                    open={openCategories[category.id]}
                                    onOpenChange={(open) =>
                                        setOpenCategories((prev) => ({
                                            ...prev,
                                            [category.id]: open,
                                        }))
                                    }
                                >
                                    <CollapsibleTrigger asChild>
                                        <div className="flex justify-between items-center cursor-pointer">
                                            <Link
                                                href={`/category/${category.id}`}
                                                className="flex items-center justify-between text-sm font-medium">
                                                <span>{category.name}</span>
                                            </Link>
                                            <ChevronDown
                                                className={`h-4 w-4 transition-transform duration-200 ${openCategories[category.id] ? "rotate-180" : ""
                                                    }`}
                                            />
                                        </div>
                                    </CollapsibleTrigger>

                                    <CollapsibleContent>
                                        <div className="space-y-3 mt-3">
                                            {category.categoryItems.map((item) => (
                                                <div key={item.id} className="max-w-64 truncate">
                                                    <Link
                                                        href={`/category/${category.id}/item/${item.id}`}
                                                        className="ml-4 rounded-md px-2 py-2 text-sm text-muted-foreground hover:bg-accent hover:text-accent-foreground cursor-pointer"
                                                    >
                                                        {item.name}
                                                    </Link>
                                                </div>
                                            ))}
                                            <div className="flex justify-center">
                                                <button
                                                    onClick={() => {
                                                        setCreationType(CreationType.CATEGORY_ITEM);
                                                        setCategoryIdToAddItem(category.id);
                                                        setDialogOpen(true);
                                                    }}
                                                    className="cursor-pointer w-3/4 rounded-md bg-primary px-3 py-1 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                                                >
                                                    + Add Category Item
                                                </button>
                                            </div>


                                            {category.categoryItems.length === 0 && (
                                                <div className="ml-4 text-xs text-muted-foreground italic">
                                                    No category items.
                                                </div>
                                            )}
                                        </div>
                                    </CollapsibleContent>
                                </Collapsible>
                            </Card>
                        ))}
                    </div>
                </ScrollArea>
            </aside>

            <main className="flex-1 p-6">
                {children}
            </main>
        </div>
    );
}