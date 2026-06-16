package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import com.programmersbox.koogintegration.dynamictranslation.model.TranslatedBlock
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.IntBuffer

class AndroidTranslationStrategy : TranslationStrategy {

    private var interpreter: Interpreter? = null
    private var tokenizer: NllbTokenizer? = null

    private fun ensureLoaded(config: DynamicTranslationConfig) {
        if (interpreter != null) return
        val modelFile = File(config.nllbModelPath)
        require(modelFile.exists()) {
            "NLLB model not found at '${config.nllbModelPath}'. Download from Meta/HuggingFace and convert to TFLite."
        }
        val vocabFile = File(modelFile.parent, "nllb_vocab.spm")
        require(vocabFile.exists()) {
            "NLLB SentencePiece vocab not found at '${vocabFile.absolutePath}'. " +
                "Expected flores200_sacrebleu_tokenizer_spm.model renamed to nllb_vocab.spm."
        }
        val interp = Interpreter(modelFile, Interpreter.Options().apply { numThreads = 4 })
        try {
            tokenizer = NllbTokenizer(vocabFile.absolutePath)
            interpreter = interp
        } catch (e: Exception) {
            interp.close()
            throw e
        }
    }

    override suspend fun translate(ocr: OcrResult, config: DynamicTranslationConfig): TranslationResult {
        ensureLoaded(config)
        val interp = interpreter ?: throw DynamicTranslationException("NLLB interpreter not loaded")
        val tok = tokenizer ?: throw DynamicTranslationException("NLLB tokenizer not loaded")

        val srcLang = toNllbLangCode(config.sourceLanguage)
        val tgtLang = toNllbLangCode(config.targetLanguage)

        val blocks = ocr.blocks.map { block ->
            val translated = translateText(interp, tok, block.text, srcLang, tgtLang)
            TranslatedBlock(original = block.text, translated = translated, bounds = block.bounds)
        }
        return TranslationResult(blocks)
    }

    private fun translateText(
        interp: Interpreter,
        tok: NllbTokenizer,
        text: String,
        srcLang: String,
        tgtLang: String,
    ): String {
        val inputIds = tok.encode(text, srcLang)
        val maxOutputLen = (inputIds.size * 2).coerceAtLeast(64)

        val inputBuffer = IntBuffer.wrap(inputIds.toIntArray())
        val outputIds = IntArray(maxOutputLen)
        val outputBuffer = IntBuffer.wrap(outputIds)

        val inputs = mapOf("input_ids" to inputBuffer)
        val outputs = mutableMapOf<String, Any>("output_ids" to outputBuffer)

        interp.runSignature(inputs, outputs, "serving_default")

        val decodedIds = outputIds.takeWhile { it != NllbTokenizer.EOS_TOKEN_ID && it != 0 }
        return tok.decode(decodedIds)
    }

    private fun toNllbLangCode(tesseractLang: String): String = LANG_MAP[tesseractLang]
        ?: throw DynamicTranslationException(
            "No NLLB language code for Tesseract lang '$tesseractLang'. " +
                "Add it to AndroidTranslationStrategy.LANG_MAP."
        )

    override fun close() {
        interpreter?.close()
        interpreter = null
        tokenizer = null
    }

    companion object {
        private val LANG_MAP = mapOf(
            "eng" to "eng_Latn",
            "fra" to "fra_Latn",
            "deu" to "deu_Latn",
            "spa" to "spa_Latn",
            "ita" to "ita_Latn",
            "por" to "por_Latn",
            "rus" to "rus_Cyrl",
            "jpn" to "jpn_Jpan",
            "kor" to "kor_Hang",
            "chi_sim" to "zho_Hans",
            "chi_tra" to "zho_Hant",
            "ara" to "arb_Arab",
        )
    }
}
