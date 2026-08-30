package com.example.verestscannerapp

object OcrCorrector {

    fun correctTextByWords(recognizedText: String, dictionary: Set<String>): String {
        val words = recognizedText.split(Regex("(?<=\\W)|(?=\\W)"))
        val correctedText = StringBuilder()

        for (word in words) {
            if (!word.matches(Regex("[А-Яа-яЄєІіЇїҐґA-Za-z]+"))) {
                correctedText.append(word)
                continue
            }

            val upperWord = word.uppercase()

            if (dictionary.contains(upperWord)) {
                correctedText.append(upperWord)
                continue
            }

            var bestMatch = upperWord
            var minDistance = Int.MAX_VALUE

            for (dictWord in dictionary) {
                if (kotlin.math.abs(upperWord.length - dictWord.length) > 4) continue

                val distance = calculateLevenshteinDistance(upperWord, dictWord)
                if (distance < minDistance) {
                    minDistance = distance
                    bestMatch = dictWord
                }
            }

            val threshold = when {
                upperWord.length <= 4 -> 1
                upperWord.length <= 8 -> 2
                else -> 4
            }

            if (minDistance <= threshold) {
                correctedText.append(bestMatch)
            } else {
                correctedText.append(upperWord)
            }
        }

        return correctedText.toString()
    }

    private fun calculateLevenshteinDistance(lhs: String, rhs: String): Int {
        var cost = IntArray(rhs.length + 1) { it }
        var newCost = IntArray(rhs.length + 1)

        for (i in 1..lhs.length) {
            newCost[0] = i
            for (j in 1..rhs.length) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost; cost = newCost; newCost = swap
        }
        return cost[rhs.length]
    }
}