"use client";

import * as React from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Category, CategoryListItem } from "@/types/Category";
import { addCategory } from "@/services/category.service";
import { Textarea } from "../ui/textarea";
import { addCategoryItem } from "@/services/categoryItem.service";

export enum CreationType {
    CATEGORY = "CATEGORY",
    CATEGORY_ITEM = "CATEGORY_ITEM",
}

type Props = {
    creationType: CreationType;
    open: boolean;
    objectName: string;
    categoryId?: number;
    onOpenChange: (open: boolean) => void;
    onCreated: (category: Category | CategoryListItem) => void;
};

export function CreateDialog({
    creationType,
    open,
    objectName,
    categoryId,
    onOpenChange,
    onCreated,
}: Props) {
    const [name, setName] = React.useState("");
    const [description, setDescription] = React.useState("");
    const [creating, setCreating] = React.useState(false);

    async function handleCreate() {
        try {
            setCreating(true);
            let created: Category | CategoryListItem;
            if (creationType === CreationType.CATEGORY) {
                created = await addCategory(name, description);
            } else {
                if (!categoryId) {
                    throw new Error("Category ID is required for creating a category item.");
                }
                created = await addCategoryItem(name, description, categoryId);
            }
            onCreated(created);
            setName("");
            setDescription("");
            onOpenChange(false);
        } finally {
            setCreating(false);
        }
    }

    React.useEffect(() => {
        if (!open) {
            setName("");
            setDescription("");
        }
    }, [open]);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Create Your New {objectName}!</DialogTitle>
                </DialogHeader>

                <Input
                    placeholder={`${objectName} Name`}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    autoFocus
                    maxLength={32}
                />

                <Textarea
                    placeholder="Description (optional)"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    rows={5}
                    className="h-32 max-h-40"
                />

                <DialogFooter>
                    <Button
                        className="cursor-pointer"
                        variant="secondary"
                        onClick={() => onOpenChange(false)}
                        disabled={creating}
                    >
                        Cancel
                    </Button>
                    <Button
                        className="cursor-pointer"
                        disabled={!name || creating}
                        onClick={handleCreate}>
                        Create
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
