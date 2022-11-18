package generators

import org.openrndr.extra.noise.uniform


private fun generateSentence() : List<Int> {

    val characters = (1 until 72) - 35 - 70 - 45 - 10
    val partitions = Int.uniform(1,4)
    val result = mutableListOf<Int>()
    for (partition in 0 until partitions) {
        val wordCount = Int.uniform(3,6)
        for (w in 0 until wordCount) {
            val syllables = Int.uniform(1,4)
            for (s in 0 until syllables) {
                result.add(characters.random())
            }
        }
        if (partition != partitions-1) {
            for (i in 0 until Int.uniform(1, 4)) {
                result.add(35)
            }
        }
    }
    val endings = listOf(70, 45, 10)
    result.add(endings.random())

    for (i in 0 until Int.uniform(0, 3)) {
        result.add(10)
    }

    //println(result.joinToString(", "))
    return result
}


fun main() {


    println("phs = [")
    for (i in 0 until 200) {
        val s = (0 until Int.uniform(3)).flatMap { generateSentence() }.joinToString(", ")
        print("   [$s],")
    }
    println("]")

}