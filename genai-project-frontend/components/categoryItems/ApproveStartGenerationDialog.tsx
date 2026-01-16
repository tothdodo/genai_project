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

type Props = {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onApproved: () => void;
};

export function ApproveStartGenerationDialog({
    open,
    onOpenChange,
    onApproved,
}: Props) {

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Approve Generation</DialogTitle>
                </DialogHeader>
                <p>
                    Are you sure you want to start the generation process?
                    <br /><br />
                    You will not be able to change materials anymore.
                </p>
                <DialogFooter>
                    <Button
                        className="cursor-pointer"
                        variant="secondary"
                        onClick={() => onOpenChange(false)}
                    >
                        Cancel
                    </Button>
                    <Button
                        className="cursor-pointer"
                        onClick={onApproved}>
                        Start Generation
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
