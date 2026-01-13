import { Skeleton } from "@/components/ui/skeleton"

export default function Loading() {
    return (
        <div className="space-y-4">
            <Skeleton className="h-8 w-[250px] rounded-md" />

            <div className="space-y-2">
                <Skeleton className="h-4 w-[400px]" />
                <Skeleton className="h-4 w-[350px]" />
            </div>
        </div>
    )
}