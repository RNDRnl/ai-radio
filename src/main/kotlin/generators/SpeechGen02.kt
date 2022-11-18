package generators

import org.openrndr.extra.noise.uniform

// tacotron + 
//private val tokens = listOf("<blank>", "<unk>", "AH0", "T", "N", "S", "R", "IH1", "D", "L", ".", "Z", "DH", "K", "W", "M", "AE1", "EH1", "AA1", "IH0", "IY1", "AH1", "B", "P", "V", "ER0", "F", "HH", "AY1", "EY1", "UW1", "IY0", "AO1", "OW1", "G", ",", "NG", "SH", "Y", "JH", "AW1", "UH1", "TH", "ER1", "CH", "?", "OW0", "OW2", "EH2", "EY2", "UW0", "IH2", "OY1", "AY2", "ZH", "AW2", "EH0", "IY2", "AA2", "AE0", "AH2", "AE2", "AO0", "AO2", "AY0", "UW2", "UH2", "AA0", "AW0", "EY0", "!", "UH0", "ER2", "OY2", """, "OY0", "<sos/eos>")
private val tokens = listOf("<blank>", "<unk>", "AH0", "T", "N", "D", "S", "R", "L", "IH1", "DH", "M", "K", "Z", "EH1", "AE1", "IH0", "AH1", "W", ",", "HH", "ER0", "P", "IY1", "V", "F", "B", "UW1", "AA1", "AY1", "AO1", ".", "EY1", "IY0", "OW1", "NG", "G", "SH", "Y", "AW1", "CH", "ER1", "UH1", "TH", "JH", "'", "?", "OW0", "EH2", "!", "IH2", "OY1", "EY2", "AY2", "EH0", "UW0", "AA2", "AE2", "OW2", "AO2", "AE0", "AH2", "ZH", "AA0", "UW2", "IY2", "AY0", "AO0", "AW2", "EY0", "UH2", "ER2", "AW0", "...", "UH0", "OY2", ". . .", "OY0", ". . . .", "..", ". ...", ". .", ". . . . .", ".. ..", "... .", "<sos/eos>")


private fun generateSentence() : List<Int> {
    val comma = tokens.indexOf(",")
    val question = tokens.indexOf("?")
    val exclaim = tokens.indexOf("!")

    val badTokens = tokens.filter { it.contains(".") || it.contains("<") || it.contains("?") || it.contains("!") || it.contains(",") || it.contains("'")  }.map { tokens.indexOf(it) }

    val characters = (2 until tokens.size-1) - badTokens
    val partitions = Int.uniform(1,4)
    val result = mutableListOf<Int>()


    val period = tokens.indexOf(".")
    for (partition in 0 until partitions) {
        val wordCount = Int.uniform(3,6)
        for (w in 0 until wordCount) {
            val syllables = if (w < wordCount -1) Int.uniform(1,4) else 3

            val uhuh = Math.random() < 0.1
            if (uhuh) {
                for (j in 0 until Int.uniform(10, 20)) {
                    result.add(comma)
                }
                for (i in 0 until 1) {
                    for (j in 0 until Int.uniform(1,4)) {
                        result.add(tokens.indexOf("EH1"))
                    }
                    //result.add(question)
                    for (j in 0 until Int.uniform(10, 20)) {
                        result.add(comma)
                    }
                }
            }


            val stutter = false///Math.random() < 0.25
            for (s in 0 until syllables) {
                result.add(characters.random())

                if (stutter) {
                    for (i in 5 until 5 + Int.uniform(0,5)) {
                        result.add(comma)
                    }
                }
            }
            result.add(exclaim)
        }
        if (Math.random() < 0.25) {
            result.add(question)
        }
        if (partition != partitions-1) {
            for (i in 0 until Int.uniform(10, 20)) {
                result.add(comma)
            }
        }
    }

    val endings = listOf(question, exclaim, period)
    result.add(endings.random())

    for (i in 0 until 30) {
        result.add(period)
    }


    //println(result.joinToString(", "))
    return result
}


fun main() {


    println("phs = [")
    for (i in 0 until 20) {
        val s = (0 until Int.uniform(3, 6)).flatMap { generateSentence() }.joinToString(", ")
        print("   [$s],")
    }
    println("]")

}