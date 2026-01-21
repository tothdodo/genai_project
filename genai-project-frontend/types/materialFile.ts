export type MaterialFile = {
    fileName: string;
    type: string;
    status: "uploading" | "completed" | "error";
    progress: number;
}