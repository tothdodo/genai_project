export function CircularProgress({ value }: { value: number }) {
    const radius = 8.5;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference - (value / 100) * circumference;

    return (
        <div className="flex items-center justify-center w-[20px] h-[20px]">
            <svg
                className="w-full h-full transform -rotate-90"
                viewBox="0 0 20 20"
                xmlns="http://www.w3.org/2000/svg"
            >
                {/* Background Track */}
                <circle
                    className="text-muted-foreground/20"
                    strokeWidth="3"
                    stroke="currentColor"
                    fill="transparent"
                    r={radius}
                    cx="10"
                    cy="10"
                />
                {/* Progress Bar */}
                <circle
                    className="text-primary transition-all duration-300 ease-in-out"
                    strokeWidth="3"
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                    strokeLinecap="round"
                    stroke="currentColor"
                    fill="transparent"
                    r={radius}
                    cx="10"
                    cy="10"
                />
            </svg>
        </div>
    );
}