// name: Example Extension
// version: 1.0.0
// author: OtakuWorld
// description: Bundled example JS extension with synthetic in-memory data, mirroring ExampleService.

function getPopularRequest(page) {
    return { url: "https://example.com/", headers: {} };
}

function getPopularParse(page, responseBody) {
    return [
        { title: "Example Item", url: "https://example.com/item", imageUrl: null }
    ];
}

function getLatestRequest(page) {
    return { url: "https://example.com/", headers: {} };
}

function getLatestParse(page, responseBody) {
    return [
        { title: "Example Item", url: "https://example.com/item", imageUrl: null }
    ];
}

function searchRequest(query, page) {
    return { url: "https://example.com/", headers: {} };
}

function searchParse(query, page, responseBody) {
    return [
        { title: "Example Item", url: "https://example.com/item", imageUrl: null }
    ];
}

function getDetailRequest(url) {
    return { url: url, headers: {} };
}

function getDetailParse(url, responseBody) {
    return {
        title: "Example Item",
        url: url,
        imageUrl: null,
        description: "An example item.",
        genres: [],
        chapters: [
            { name: "Chapter 1", url: "https://picsum.photos/200/300", uploaded: null }
        ]
    };
}

function getContentRequest(url) {
    return { url: url, headers: {} };
}

function getContentParse(url, responseBody) {
    return { urls: [ "https://picsum.photos/200/300" ], headers: {} };
}
