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

interface Request {
    url: string;
    headers: Record<string, string>;
}

function getPopularRequest(page: number): Request {
    return { url: "https://example.com/popular?page=" + page, headers: {} };
}

function getPopularParse(page: number, responseBody: string): Item[] {
    return [
        { title: "Sample Item", url: "https://example.com/item/1", imageUrl: null }
    ];
}

function getLatestRequest(page: number): Request {
    return { url: "https://example.com/latest?page=" + page, headers: {} };
}

function getLatestParse(page: number, responseBody: string): Item[] {
    return [
        { title: "Latest Sample Item", url: "https://example.com/item/2", imageUrl: null }
    ];
}

function searchRequest(query: string, page: number): Request {
    return { url: "https://example.com/search?q=" + query + "&page=" + page, headers: {} };
}

function searchParse(query: string, page: number, responseBody: string): Item[] {
    return [
        { title: "Search Result for " + query, url: "https://example.com/item/3", imageUrl: null }
    ];
}

function getDetailRequest(url: string): Request {
    return { url: url, headers: {} };
}

function getDetailParse(url: string, responseBody: string) {
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

function getContentRequest(url: string): Request {
    return { url: url, headers: {} };
}

function getContentParse(url: string, responseBody: string) {
    return {
        urls: ["https://example.com/content/1.png"],
        headers: {}
    };
}
