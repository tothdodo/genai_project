import { Skeleton } from "@/components/ui/skeleton"

export default function Loading() {
    return (
        <div className="flex h-full">
            <div className="flex-1 space-y-4">
                <Skeleton className="h-64 w-full rounded-md" />
                <Skeleton className="h-64 w-full rounded-md" />
                <Skeleton className="h-64 w-full rounded-md" />
            </div>
            <div className="w-1/3 pl-6">
                <Skeleton className="h-full rounded-md" />
            </div>
        </div>
    )
}