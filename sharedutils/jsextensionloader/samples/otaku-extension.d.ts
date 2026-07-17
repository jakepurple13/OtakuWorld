// Type declarations for OtakuWorld JS/TS extensions.
// Reference these in your editor for type-checking and autocomplete.
// Not consumed on-device — extension functions must be synchronous.
//
// Each operation is a pure "request" function (describes what to fetch —
// no networking) paired with a pure "parse" function (turns the host-fetched
// response body into the result). The host performs the actual HTTP fetch
// between the two calls; your code never touches the network directly.

interface ExtensionRequest {
    url: string;
    headers?: Record<string, string>;
}

interface ExtensionItem {
    title: string;
    url: string;
    imageUrl: string | null;
}

interface ExtensionChapter {
    name: string;
    url: string;
    uploaded: string | null;
}

interface ExtensionDetail {
    title: string;
    url: string;
    imageUrl: string | null;
    description: string | null;
    genres: string[];
    chapters: ExtensionChapter[];
}

interface ExtensionContent {
    urls: string[];
    headers?: Record<string, string>;
}

declare function getPopularRequest(page: number): ExtensionRequest;
declare function getPopularParse(page: number, responseBody: string): ExtensionItem[];

declare function getLatestRequest(page: number): ExtensionRequest;
declare function getLatestParse(page: number, responseBody: string): ExtensionItem[];

declare function searchRequest(query: string, page: number): ExtensionRequest;
declare function searchParse(query: string, page: number, responseBody: string): ExtensionItem[];

declare function getDetailRequest(url: string): ExtensionRequest;
declare function getDetailParse(url: string, responseBody: string): ExtensionDetail;

declare function getContentRequest(url: string): ExtensionRequest;
declare function getContentParse(url: string, responseBody: string): ExtensionContent;
