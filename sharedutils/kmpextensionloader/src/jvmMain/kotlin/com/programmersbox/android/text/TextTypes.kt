package android.text

// Interface shapes match the SDK so settings-UI code in extensions links and verifies. Nothing on desktop
// implements Editable or calls a TextWatcher; span APIs are left out because no extension calls them.

interface Spanned : CharSequence

interface Spannable : Spanned

interface GetChars : CharSequence

interface Editable : CharSequence, GetChars, Spannable, Appendable

interface NoCopySpan

interface TextWatcher : NoCopySpan {
    fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int)
    fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)
    fun afterTextChanged(s: Editable?)
}

open class TextPaint : android.graphics.Paint {
    constructor() : super()
    constructor(flags: Int) : super(flags)
}

/** Plain-text conversion: tags are dropped (block tags and `<br>` become line breaks) and common entities decoded. */
object Html {
    const val FROM_HTML_MODE_LEGACY = 0
    const val FROM_HTML_MODE_COMPACT = 63

    @JvmStatic fun fromHtml(source: String?, flags: Int): Spanned = PlainSpanned(toPlainText(source.orEmpty()))

    @JvmStatic fun fromHtml(source: String?): Spanned = fromHtml(source, FROM_HTML_MODE_LEGACY)

    private val lineBreak = Regex("(?i)<br\\s*/?>|</(p|div|h[1-6]|li)>")
    private val tag = Regex("<[^>]*>")
    private val numericEntity = Regex("&#(x?)([0-9a-fA-F]+);")

    private fun toPlainText(html: String): String = html
        .replace(lineBreak, "\n")
        .replace(tag, "")
        .replace(numericEntity) { m ->
            val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) 10 else 16)
            code?.let { String(Character.toChars(it)) } ?: m.value
        }
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
        .trimEnd('\n')

    private class PlainSpanned(private val text: String) : Spanned, CharSequence by text {
        override fun toString(): String = text
    }
}
