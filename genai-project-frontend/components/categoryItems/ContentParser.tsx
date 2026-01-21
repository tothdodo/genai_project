import ReactMarkdown from 'react-markdown';
import remarkMath from 'remark-math';
import remarkGfm from 'remark-gfm';
import rehypeKatex from 'rehype-katex';
import rehypeRaw from 'rehype-raw';
import Image from 'next/image';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { Brain, Activity, Info, Link2, CheckCircle2, Circle } from 'lucide-react';
import { Card, CardContent } from "@/components/ui/card";
import 'katex/dist/katex.min.css';

interface LiProps extends React.LiHTMLAttributes<HTMLLIElement> {
    checked?: boolean | null;
    children?: React.ReactNode;
}

const ContentParser = ({ content }: { content: string }) => {
    return (
        <div className="max-w-4xl mx-auto p-6 bg-background">
            <ReactMarkdown
                remarkPlugins={[remarkMath, remarkGfm]}
                rehypePlugins={[rehypeKatex, rehypeRaw]}
                components={{
                    h1: ({ children }) => <h1 className="text-3xl font-bold mb-6 border-b pb-2">{children}</h1>,
                    h2: ({ children }) => <h2 className="text-2xl font-bold mb-4 mt-8 text-foreground/90">{children}</h2>,
                    h3: ({ children }) => (
                        <div className="flex items-center gap-3 mt-12 mb-6 border-b pb-4">
                            <div className="bg-primary text-primary-foreground p-2 rounded-lg">
                                <Brain size={24} />
                            </div>
                            <h2 className="text-3xl font-bold tracking-tight text-foreground">{children}</h2>
                        </div>
                    ),
                    h4: ({ children }) => (
                        <h3 className="text-xl font-semibold mt-8 mb-4 flex items-center gap-2 text-primary/90">
                            <Activity size={18} className="text-primary" />
                            {children}
                        </h3>
                    ),

                    p: ({ children }) => <p className="leading-7 mb-4 text-slate-700 dark:text-slate-300">{children}</p>,
                    a: ({ href, children }) => (
                        <a href={href} className="text-primary underline underline-offset-4 hover:text-primary/80 transition-colors inline-flex items-center gap-1">
                            <Link2 size={14} />{children}
                        </a>
                    ),
                    strong: ({ children }) => <strong className="font-bold text-foreground">{children}</strong>,
                    hr: () => <hr className="my-8 border-muted" />,

                    ul: ({ children }) => <ul className="space-y-2 mb-6 ml-2">{children}</ul>,
                    ol: ({ children }) => <ol className="list-decimal space-y-2 mb-6 ml-6">{children}</ol>,
                    li: ({ children, checked, ...props }: LiProps) => {
                        if (checked !== null && checked !== undefined) {
                            return (
                                <li className="flex items-start gap-2 mb-2 list-none" {...props}>
                                    {checked ?
                                        <CheckCircle2 size={18} className="text-green-500 mt-1 shrink-0" /> :
                                        <Circle size={18} className="text-muted-foreground mt-1 shrink-0" />
                                    }
                                    <span className={checked ? "line-through text-muted-foreground" : ""}>
                                        {children}
                                    </span>
                                </li>
                            );
                        }

                        return (
                            <li className="mb-4 last:mb-0 pl-4 border-l-2 border-muted hover:border-primary transition-colors py-1 list-none" {...props}>
                                <span className="text-slate-700 dark:text-slate-300 leading-relaxed">
                                    {children}
                                </span>
                            </li>
                        );
                    },

                    table: ({ children }) => (
                        <div className="my-6 w-full overflow-y-auto rounded-lg border">
                            <table className="w-full text-sm">{children}</table>
                        </div>
                    ),
                    thead: ({ children }) => <thead className="bg-muted/50 font-bold">{children}</thead>,
                    th: ({ children }) => <th className="px-4 py-2 text-left border-b">{children}</th>,
                    td: ({ children }) => <td className="px-4 py-2 border-b">{children}</td>,
                    tr: ({ children }) => <tr className="even:bg-muted/20 transition-colors hover:bg-muted/30">{children}</tr>,

                    code: ({ className, children, ...props }) => {
                        const match = /language-(\w+)/.exec(className || '');
                        const isCodeBlock = !!match;

                        return isCodeBlock ? (
                            <div className="rounded-lg overflow-hidden my-6 border border-border">
                                <SyntaxHighlighter
                                    language={match[1]}
                                    PreTag="div"
                                    customStyle={{ margin: 0, padding: '1.5rem' }}
                                >
                                    {String(children).replace(/\n$/, '')}
                                </SyntaxHighlighter>
                            </div>
                        ) : (
                            <code className="bg-muted px-1.5 py-0.5 rounded text-sm font-mono font-medium text-pink-500" {...props}>
                                {children}
                            </code>
                        );
                    },

                    blockquote: ({ children }) => (
                        <Card className="my-6 bg-slate-50 dark:bg-slate-900 border-l-4 border-l-blue-500 italic">
                            <CardContent className="pt-6">
                                <div className="flex gap-2">
                                    <Info className="text-blue-500 shrink-0" size={20} />
                                    <div>{children}</div>
                                </div>
                            </CardContent>
                        </Card>
                    ),
                    img: ({ src, alt }) => (
                        <div className="my-8 flex flex-col items-center">
                            <Image src={src as string} alt={alt as string} className="rounded-xl shadow-lg max-w-full h-auto" />
                            {alt && <span className="text-sm text-muted-foreground mt-2 italic">{alt}</span>}
                        </div>
                    ),
                    div: ({ className, children }) => {
                        if (className?.includes('math-display')) {
                            return (
                                <div className="my-8 p-6 bg-slate-100 dark:bg-slate-800 rounded-xl flex justify-center items-center shadow-inner overflow-x-auto">
                                    {children}
                                </div>
                            );
                        }
                        return <div>{children}</div>;
                    }
                }}
            >
                {content}
            </ReactMarkdown>
        </div>
    );
};

export default ContentParser;