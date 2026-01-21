import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { STATUS_CONFIG, StatusType } from "@/types/statusConfig";

interface StatusTagProps {
    status: string; // The value coming from your backend
    failedJobType?: string;
}

function formatFailedJobType(failedJobType: string) {
    return failedJobType
        .replaceAll("_", " ")
        .toLowerCase()
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

export function StatusTag({ status, failedJobType }: StatusTagProps) {
    // Normalize the backend string to match our keys (e.g., "active" -> "ACTIVE")
    const key = status.toUpperCase() as StatusType;
    const config = STATUS_CONFIG[key] || STATUS_CONFIG.PENDING;

    return (
        <Badge
            variant="outline"
            className={cn("px-4 py-1 text-md shadow-none", config.color)}
        >
            {config.label} {failedJobType ? ` (${formatFailedJobType(failedJobType)})` : ""}
        </Badge>
    );
}