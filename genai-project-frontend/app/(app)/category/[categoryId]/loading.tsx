import { Skeleton } from "@/components/ui/skeleton"

export default function Loading() {
    return (
        <div className="space-y-4">
            <Skeleton className="h-80 w-3/4 rounded-md" />
        </div>
    )
}