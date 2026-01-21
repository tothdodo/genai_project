export const STATUS_CONFIG = {
    PENDING: { label: "Pending", color: "bg-amber-100 text-amber-700 border-amber-200" },
    PROCESSING: { label: "Processing...", color: "bg-slate-100 text-slate-600 border-slate-200" },
    COMPLETED: { label: "Generation Completed", color: "bg-emerald-100 text-emerald-700 border-emerald-200" },
    FAILED: { label: "Generation Failed", color: "bg-rose-100 text-rose-700 border-rose-200" },
} as const;

export type StatusType = keyof typeof STATUS_CONFIG;