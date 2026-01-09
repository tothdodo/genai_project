import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export default function HomePage() {
    return (
        <Card>
            <CardHeader>
                <CardTitle>Home</CardTitle>
            </CardHeader>
            <CardContent>
                <p className="text-muted-foreground">
                    Welcome to the Home page.
                </p>
            </CardContent>
        </Card>
    )
}
