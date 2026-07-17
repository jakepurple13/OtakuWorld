// name: Sample Extension
// version: 1.0.0
// author: OtakuWorld
// description: Reference/fixture extension with stubbed implementations of all required functions.
// iconUrl: https://example.com/sample-extension-icon.png
// updateUrl: https://example.com/sample-extension/update.json

function getPopular(page) {
    return [
        { title: "Sample Item", url: "https://example.com/item/1", imageUrl: null }
    ];
}

function getLatest(page) {
    return [
        { title: "Latest Sample Item", url: "https://example.com/item/2", imageUrl: null }
    ];
}

function search(query, page) {
    return [
        { title: "Search Result for " + query, url: "https://example.com/item/3", imageUrl: null }
    ];
}

function getDetail(url) {
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

function getContent(url) {
    return {
        urls: ["https://example.com/content/1.png"],
        headers: {}
    };
}
