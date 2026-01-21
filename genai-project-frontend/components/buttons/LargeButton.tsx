"use client";

type LargeButtonProps = {
    onClick: () => void;
    disabled?: boolean;
    additionalClassName?: string;
    children: React.ReactNode;
};

export default function LargeButton(
    {
        onClick,
        disabled,
        additionalClassName,
        children,
    }: LargeButtonProps
) {
    return (
        <button
            onClick={onClick}
            disabled={disabled}
            className={`disabled:cursor-not-allowed disabled:bg-gray-400 cursor-pointer rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${additionalClassName}`}
        >
            {children}
        </button>
    )
}