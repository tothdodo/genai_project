import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";

export default function HomePage() {
    return (
        <div className="max-w-5xl mx-auto px-6 py-10 space-y-10">
            <section className="space-y-3">
                <h1 className="text-3xl font-semibold tracking-tight">
                    Knowledge Organizer & Learning Assistant
                </h1>
                <p className="text-muted-foreground text-base max-w-3xl">
                    Organize your knowledge into structured categories, enrich them with
                    files, and automatically generate summaries and flashcards to support
                    efficient learning.
                </p>
            </section>

            <Separator />

            <section className="space-y-6">
                <h2 className="text-xl font-semibold">How the application works</h2>

                <div className="grid gap-6 sm:grid-cols-3">
                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <Badge variant="secondary">Step 1</Badge>
                                Categories
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="text-sm text-muted-foreground">
                            Create one or more categories to represent high-level topics
                            (e.g. Subjects, Projects, Courses).
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <Badge variant="secondary">Step 2</Badge>
                                Category Items
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="text-sm text-muted-foreground">
                            Inside each category, add category items to represent concrete
                            units such as chapters, modules, or tasks.
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <Badge variant="secondary">Step 3</Badge>
                                Learning Assets
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="text-sm text-muted-foreground">
                            Attach files to a category item and generate summaries and
                            flashcards to support understanding and long-term retention.
                        </CardContent>
                    </Card>
                </div>
            </section>

            <Separator />

            <section className="space-y-6">
                <h2 className="text-xl font-semibold">Core features</h2>

                <div className="grid gap-4 sm:grid-cols-2">
                    <Card>
                        <CardContent className="space-y-2">
                            <h3 className="font-medium">Structured Knowledge Management</h3>
                            <p className="text-sm text-muted-foreground">
                                A clear hierarchy of categories and items keeps content
                                organized and easy to navigate.
                            </p>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardContent className="space-y-2">
                            <h3 className="font-medium">File-Based Learning</h3>
                            <p className="text-sm text-muted-foreground">
                                Upload documents and materials directly to category items as
                                the source of learning content.
                            </p>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardContent className="space-y-2">
                            <h3 className="font-medium">Automatic Summaries</h3>
                            <p className="text-sm text-muted-foreground">
                                Generate concise summaries that highlight the most important
                                concepts from your files.
                            </p>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardContent className="space-y-2">
                            <h3 className="font-medium">Flashcard Generation</h3>
                            <p className="text-sm text-muted-foreground">
                                Learn actively with flashcards created from your content to
                                reinforce memory and understanding.
                            </p>
                        </CardContent>
                    </Card>
                </div>
            </section>

            <Separator />

            <section className="rounded-lg border bg-muted/30 p-6">
                <p className="text-sm text-muted-foreground">
                    Start by creating a category using the left panel, then add category
                    items and attach files to begin generating learning materials.
                </p>
            </section>
        </div>
    );
}
