// name: Sample Extension
// version: 1.0.0
// author: OtakuWorld
// description: Reference/fixture extension with stubbed implementations of all required functions.
// iconUrl: https://example.com/sample-extension-icon.png
// updateUrl: https://example.com/sample-extension/update.json

function getPopularRequest(page) {
    return { url: "https://example.com/popular?page=" + page, headers: {} };
}

function getPopularParse(page, responseBody) {
    return [
        { title: "Sample Item", url: "https://example.com/item/1", imageUrl: null }
    ];
}

function getLatestRequest(page) {
    return { url: "https://example.com/latest?page=" + page, headers: {} };
}

function getLatestParse(page, responseBody) {
    return [
        { title: "Latest Sample Item", url: "https://example.com/item/2", imageUrl: null }
    ];
}

function searchRequest(query, page) {
    return { url: "https://example.com/search?q=" + query + "&page=" + page, headers: {} };
}

function searchParse(query, page, responseBody) {
    return [
        { title: "Search Result for " + query, url: "https://example.com/item/3", imageUrl: null }
    ];
}

function getDetailRequest(url) {
    return { url: url, headers: {} };
}

function getDetailParse(url, responseBody) {
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

function getContentRequest(url) {
    return { url: url, headers: {} };
}

function getContentParse(url, responseBody) {
    return {
        urls: ["https://example.com/content/1.png"],
        headers: {}
    };
}
