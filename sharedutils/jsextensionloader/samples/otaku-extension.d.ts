// Type declarations for OtakuWorld JS/TS extensions.
// Reference these in your editor for type-checking and autocomplete.
// Not consumed on-device — extension functions must be synchronous.

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

declare function getPopular(page: number): ExtensionItem[];
declare function getLatest(page: number): ExtensionItem[];
declare function search(query: string, page: number): ExtensionItem[];
declare function getDetail(url: string): ExtensionDetail;
declare function getContent(url: string): ExtensionContent;
