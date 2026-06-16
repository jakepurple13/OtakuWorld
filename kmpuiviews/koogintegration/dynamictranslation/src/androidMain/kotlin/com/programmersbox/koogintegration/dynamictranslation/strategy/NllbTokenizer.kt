package com.programmersbox.koogintegration.dynamictranslation.strategy

import java.io.File

/**
 * A lightweight SentencePiece tokenizer for NLLB models.
 *
 * `tensorflow-lite-support:0.4.4` does not expose `SentencePieceTokenizer` in its public AAR
 * (the `org.tensorflow.lite.support.text` package is absent from the distributable). This class
 * implements BPE-style unigram tokenization using the SPM vocab file directly.
 *
 * SPM vocab files for NLLB (flores200_sacrebleu_tokenizer_spm.model) include a plain-text
 * companion `.vocab` file. If only the `.spm` binary is available, this tokenizer falls back to
 * a whitespace-split + UNK strategy, which is sufficient for integration-level testing. Full
 * subword accuracy requires the vocab list.
 *
 * The `.vocab` file is expected alongside the `.spm` file (same directory, `.vocab` extension).
 * Each line: `<piece>\t<log_prob>` (standard SentencePiece vocab export format).
 */
class NllbTokenizer(private val modelPath: String) {

    // piece → id mapping (index = id)
    private val pieceToId: Map<String, Int> by lazy { loadVocab() }

    // id → piece mapping
    private val idToPiece: Map<Int, String> by lazy { pieceToId.entries.associate { (k, v) -> v to k } }

    private fun loadVocab(): Map<String, Int> {
        val vocabFile = File(modelPath).let { spm ->
            // Try .vocab alongside .spm
            File(spm.parent, spm.nameWithoutExtension + ".vocab").takeIf { it.exists() }
                ?: File(spm.parent, "nllb_vocab.vocab").takeIf { it.exists() }
        }
        if (vocabFile == null) {
            // No vocab file — return a minimal map with only special tokens.
            // Runtime tokenization will fall back to whitespace splitting.
            return buildMap {
                put("<unk>", 0)
                put("<s>", 1)
                put("</s>", EOS_TOKEN_ID)
                put("<pad>", PAD_TOKEN_ID)
            }
        }
        val map = mutableMapOf<String, Int>()
        vocabFile.bufferedReader().lineSequence().forEachIndexed { index, line ->
            val piece = line.substringBefore('\t')
            map[piece] = index
        }
        return map
    }

    /**
     * Encodes [text] using greedy longest-match over the vocab.
     * Prepends the NLLB source-language token and appends EOS.
     */
    fun encode(text: String, srcLangCode: String): List<Int> {
        val langTokenId = pieceToId[srcLangCode] ?: run {
            // Language tokens like "__eng_Latn__" may be stored with underscores in some models.
            // Try the alternate "__xx__" form as fallback.
            val alt = "__${srcLangCode}__"
            pieceToId[alt] ?: UNKNOWN_TOKEN_ID
        }

        val wordIds = if (pieceToId.size <= 4) {
            // Minimal vocab — fall back to whitespace splitting with UNK
            text.trim().split(Regex("\\s+")).map { pieceToId[it] ?: UNKNOWN_TOKEN_ID }
        } else {
            encodeBpe(text)
        }

        return listOf(langTokenId) + wordIds + listOf(EOS_TOKEN_ID)
    }

    /**
     * Greedy longest-match BPE encoding.
     * Prepends "▁" (U+2581 LOWER ONE EIGHTH BLOCK) to mark word boundaries, as SentencePiece does.
     */
    private fun encodeBpe(text: String): List<Int> {
        val normalised = text.trim().replace(" ", "▁").let { "▁$it" }
        val ids = mutableListOf<Int>()
        var pos = 0
        while (pos < normalised.length) {
            var end = normalised.length
            var found = false
            while (end > pos) {
                val sub = normalised.substring(pos, end)
                val id = pieceToId[sub]
                if (id != null) {
                    ids.add(id)
                    pos = end
                    found = true
                    break
                }
                end--
            }
            if (!found) {
                // Single character not in vocab → UNK
                ids.add(UNKNOWN_TOKEN_ID)
                pos++
            }
        }
        return ids
    }

    /**
     * Decodes a list of token IDs back to a string.
     * IDs not found in the vocab are rendered as the empty string (dropped).
     */
    fun decode(tokenIds: List<Int>): String {
        return tokenIds
            .filter { it != EOS_TOKEN_ID && it != PAD_TOKEN_ID && it != UNKNOWN_TOKEN_ID }
            .joinToString("") { id -> idToPiece[id] ?: "" }
            .replace("▁", " ")
            .trim()
    }

    companion object {
        const val EOS_TOKEN_ID = 2
        const val PAD_TOKEN_ID = 3
        const val UNKNOWN_TOKEN_ID = 0
    }
}
