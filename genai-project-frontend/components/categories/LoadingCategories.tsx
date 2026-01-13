import { Skeleton } from "../ui/skeleton";

export default function LoadingCategories() {
    return (
        <div className="space-y-4">
            {
                Array.from({ length: 9 }).map((_, index) => (
                    <Skeleton key={index} className="h-11 w-full rounded-xl" />
                ))
            }
        </div>
    )
}