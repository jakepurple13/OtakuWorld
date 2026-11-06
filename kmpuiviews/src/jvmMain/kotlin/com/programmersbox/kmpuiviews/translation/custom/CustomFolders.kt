package com.programmersbox.kmpuiviews.translation.custom

import com.programmersbox.kmpuiviews.translation.LanguagePair
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotModelFiles
import java.io.File

sealed interface FolderCheck {
    /** A usable model folder. Implements [LanguagePair] so folders chain through English like downloaded models. */
    data class Valid(
        override val from: String,
        override val to: String,
        val files: BergamotModelFiles,
        val sizeBytes: Long,
    ) : FolderCheck, LanguagePair

    data class Invalid(val reason: String) : FolderCheck
}

/** Two lowercase letters twice, e.g. `jaen` in `model.jaen.intgemm.alphas.bin`. */
private val pairSegment = Regex("""[a-z]{2}[a-z]{2}""")

/**
 * Checks [dir] for a Bergamot (Marian) model: `model*.bin`, an optional `lex*.bin` shortlist, and either one
 * `vocab*.spm` or a `srcvocab*.spm` + `trgvocab*.spm`. Vocabs must end in `.spm` because Marian picks the vocab
 * loader by extension. The language pair comes from the file names (`<from><to>`, e.g. `model.jaen...`).
 */
fun inspectFolder(dir: File): FolderCheck {
    if (!dir.isDirectory) return FolderCheck.Invalid("This folder doesn't exist anymore")
    val files = dir.listFiles { f -> f.isFile }?.sortedBy { it.name }.orEmpty()
    val model = files.firstOrNull { it.name.startsWith("model") && it.extension == "bin" }
        ?: return FolderCheck.Invalid("No model file found (expected model*.bin)")
    val lex = files.firstOrNull { it.name.startsWith("lex") && it.extension == "bin" }

    fun vocab(prefix: String) = files.firstOrNull { it.name.startsWith(prefix) && it.extension == "spm" }
    val shared = vocab("vocab")
    val src = vocab("srcvocab") ?: shared
    val trg = vocab("trgvocab") ?: shared
    if (src == null || trg == null) {
        return FolderCheck.Invalid("No vocab file found (expected vocab*.spm, or srcvocab*.spm and trgvocab*.spm)")
    }
    val pair = model.name.split('.').firstOrNull { pairSegment.matches(it) }
        ?: return FolderCheck.Invalid("Can't tell the languages from the file names (expected something like model.jaen.bin)")
    val size = listOfNotNull(model, lex, src, trg).distinct().sumOf { it.length() }
    return FolderCheck.Valid(pair.take(2), pair.drop(2), BergamotModelFiles(model, lex, src, trg), size)
}
