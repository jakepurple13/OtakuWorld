// name: Sample TypeScript Extension
// version: 1.0.0
// author: OtakuWorld
// description: TypeScript reference/fixture extension with stubbed implementations of all required functions.
// iconUrl: https://example.com/sample-extension-icon.png
// updateUrl: https://example.com/sample-extension/update.json

interface Item {
    title: string;
    url: string;
    imageUrl: string;
}

function getPopular(page: number): Item[] {
    return [
        { title: "Sample Item", url: "https://example.com/item/1", imageUrl: null }
    ];
}

function getLatest(page: number): Item[] {
    return [
        { title: "Latest Sample Item", url: "https://example.com/item/2", imageUrl: null }
    ];
}

function search(query: string, page: number): Item[] {
    return [
        { title: "Search Result for " + query, url: "https://example.com/item/3", imageUrl: null }
    ];
}

function getDetail(url: string) {
    return {
        title: "Sample Item",
        url: url,
        imageUrl: null,
        description: "A sample item detail.",
        genres: ["Action"],
        chapters: [
            { name: "Chapter 1", url: "https://example.com/chapter/1", uploaded: null }
        ]
    };
}

function getContent(url: string) {
    return {
        urls: ["https://example.com/content/1.png"],
        headers: {}
    };
}
