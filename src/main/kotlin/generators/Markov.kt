package generators

import org.openrndr.extra.noise.uniform


class MarkovNode(val generator:()->List<Int>)


class MarkovChain() {

    val chain = mutableMapOf<MarkovNode, MutableList<Pair<MarkovNode, Double>>>()


    fun MarkovNode.to(target:MarkovNode, weight:Double) {
        val list = chain.getOrPut(this) { mutableListOf() }
        list.add(Pair(target, weight))
    }

    fun build(builder:MarkovChain.()->Unit) {
        this.builder()
    }

    fun generate(start:MarkovNode) : List<Int> {
        var generated = mutableListOf<Int>()
        var current = start
        for (step in 0 until 100) {
            val options = chain.get(current) ?: return generated

            val sorted = options.sortedBy { it.second }
            val sum = sorted.sumOf { it.second }
            val w = Double.uniform(0.0, sum)
            var cs = 0.0
            for (x in 0 until sorted.size) {

                cs += sorted[x].second
                println("${sum} ${x} ${w} ${cs}")
                if (cs >= w) {
                    current = sorted[x].first
                    break
                }
            }
            generated = (generated + current.generator()).toMutableList()
        }
        return generated
    }
}

fun main() {
    val mc = MarkovChain()
    val neh = MarkovNode { listOf(AY2, EY2).shuffled().take(1) + listOf(`?`)+listOf(PERIOD)}
    val nehPause = MarkovNode { listOf(COMMA, COMMA, COMMA, COMMA ) }
    val end = MarkovNode { listOf(SOSEOS)}

    val wordConsonant = MarkovNode { listOf(N, T, TH, D, F, SH, S, Z, ZH, B, P, W, R, L, K, JH).shuffled().take(1) }
    val wordVowel = MarkovNode { listOf(AA0, AA1, AA2, OW0, OW1, OW2, EH0, EH1, EH2, AY0, AY1, AY2, UW0, UW1, UW2, AE0, AE1, AE2, AH0, AH1, AH2).shuffled().take(1)}
    val wordQuestion = MarkovNode { listOf(`?`)}

    mc.build {
        neh.to(neh, 2.0)
        neh.to(nehPause,2.0)
        nehPause.to(neh, 1.0)
        neh.to(wordConsonant, 1.0)
        neh.to(wordVowel, 1.0)
        nehPause.to(wordConsonant, 1.0)
        wordConsonant.to(wordVowel, 1.0)
        wordVowel.to(wordConsonant, 1.0)
        wordVowel.to(neh, 0.2)
        wordConsonant.to(neh, 0.2)
        wordConsonant.to(wordQuestion, 0.3)
        wordVowel.to(wordQuestion, 0.3)
        wordQuestion.to(wordConsonant, 1.0)
        wordQuestion.to(wordVowel, 1.0)


    }
    println(mc.generate(neh))
}