"use client";

import * as React from "react";
import { Card } from "@/components/ui/card";
import { getAllCategories } from "@/services/category.service";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { ChevronDown } from "lucide-react";
import { Category } from "@/types/category";
import { CreateDialog, CreationType } from "@/components/categories/CreateDialog";
import Link from "next/link";
import LoadingCategories from "@/components/categories/LoadingCategories";
import { cn } from "@/lib/utils"
import LargeButton from "@/components/buttons/LargeButton";
import SmallButton from "@/components/buttons/SmallButton";
import { CategoryItem } from "@/types/categoryItem";
import { useRouter } from "next/navigation";

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
        console.log(process.env.NEXT_PUBLIC_BACKEND_URI);
        getAllCategories()
            .then(setCategories)
            .finally(() => setLoading(false));
    }, []);


    // Inside your main component:
    const router = useRouter();


    return (
        <div className="flex min-h-screen w-full">
            <aside className="w-80 border-r bg-zinc-300 py-6">
                <div className="px-6 mb-3">
                    <Link
                        href="/home"
                        className="block text-lg font-semibold tracking-tight hover:opacity-80 transition"
                    >
                        Knowledge Hub
                    </Link>

                    <p className="text-xs text-muted-foreground mt-1">
                        Organize, summarize, and learn!
                    </p>
                </div>
                <div className="px-6 pt-3">
                    <LargeButton
                        onClick={() => {
                            setCreationType(CreationType.CATEGORY);
                            setDialogOpen(true);
                        }}
                        additionalClassName="w-full"
                        disabled={loading}
                    >
                        + Add category
                    </LargeButton>
                </div>
                <CreateDialog
                    open={dialogOpen}
                    onOpenChange={setDialogOpen}
                    creationType={creationType}
                    objectName={objectName}
                    categoryId={categoryIdToAddItem}
                    onCreated={(created) => {
                        if (creationType === CreationType.CATEGORY) {
                            const newCategory = created as Category;
                            setCategories((prev) => [newCategory, ...prev]);
                            router.push(`/category/${newCategory.id}`);
                        } else {
                            const newCategoryItem = created as CategoryItem;
                            setCategories((prev) => {
                                return prev.map((category) => {
                                    if (category.id === categoryIdToAddItem) {
                                        return {
                                            ...category,
                                            categoryItems: [newCategoryItem, ...(category.categoryItems || [])],
                                        };
                                    }
                                    return category;
                                })
                            });
                            router.push(`/category/${newCategoryItem.categoryId}/item/${newCategoryItem.id}`);
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

                                    <CollapsibleContent
                                        className={cn("text-popover-foreground outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2")}
                                    >
                                        <div className="space-y-3 mt-3">
                                            {category.categoryItems.map((item) => (
                                                <div key={item.id} className="max-w-64 truncate">
                                                    <Link
                                                        href={`/category/${category.id}/item/${item.id}`}
                                                        className="ml-4 rounded-xl px-2 py-2 text-sm text-muted-foreground hover:bg-accent hover:text-accent-foreground cursor-pointer"
                                                    >
                                                        {item.name}
                                                    </Link>
                                                </div>
                                            ))}
                                            {category.categoryItems.length === 0 && (
                                                <div className="ml-4 text-xs text-muted-foreground italic">
                                                    No category items.
                                                </div>
                                            )}
                                            <div className="flex justify-center">
                                                <SmallButton
                                                    onClick={() => {
                                                        setCreationType(CreationType.CATEGORY_ITEM);
                                                        setCategoryIdToAddItem(category.id);
                                                        setDialogOpen(true);
                                                    }}
                                                >
                                                    + Add Category Item
                                                </SmallButton>
                                            </div>
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