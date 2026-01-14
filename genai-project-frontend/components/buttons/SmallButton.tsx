"use client";

type SmallButtonProps = {
    onClick: () => void;
    disabled?: boolean;
    additionalClasses?: string;
    children: React.ReactNode;
};

export default function SmallButton(
    {
        onClick,
        disabled,
        additionalClasses,
        children,
    }: SmallButtonProps
) {
    return (
        <button
            onClick={onClick}
            disabled={disabled}
            className={`cursor-pointer w-3/4 rounded-md bg-primary px-3 py-1 text-xs font-medium text-primary-foreground hover:bg-primary/90 ${additionalClasses ?? ""}`}
        >
            {children}
        </button>
    )
}