package com.programmersbox.kmpuiviews.translation

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageDetectorTest {
    private val detector = LibraryLanguageDetector()

    @Test
    fun `detects supported languages from one sentence`() {
        val samples = mapOf(
            "ja" to "平凡な高校生の少年が、ある日突然、不思議な力を手に入れて世界を救う冒険に出ることになった。",
            "ko" to "평범한 고등학생 소년이 어느 날 갑자기 신비한 힘을 얻고 세계를 구하기 위한 모험을 떠나게 된다.",
            "zh" to "一个普通的高中少年有一天突然获得了神秘的力量，从此踏上了拯救世界的冒险旅程。",
            "es" to "Un estudiante de secundaria común obtiene de repente un poder misterioso y se embarca en una aventura para salvar el mundo.",
            "fr" to "Un lycéen ordinaire obtient soudainement un pouvoir mystérieux et part à l'aventure pour sauver le monde.",
            "de" to "Ein ganz gewöhnlicher Schüler erhält plötzlich eine geheimnisvolle Kraft und zieht aus, um die Welt zu retten.",
            "en" to "An ordinary high school boy suddenly gains a mysterious power and sets out on an adventure to save the world.",
            "ru" to "Обычный старшеклассник внезапно получает таинственную силу и отправляется в приключение, чтобы спасти мир.",
        )
        samples.forEach { (code, text) -> assertEquals(code, detector.detect(text), "for $code") }
    }

    @Test
    fun `blank returns null`() {
        assertEquals(null, detector.detect("   "))
    }
}
