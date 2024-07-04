package org.example

import java.io.File

const val ALL_WORDS_LEARNED = "Все слова выучены"

fun main() {

    val dictionary: MutableList<Word> = mutableListOf()

    val wordFile = File("words.txt")
    val stringList = wordFile.readLines()
    for (i in stringList) {
        val split = i.split("|")
        dictionary.add(Word(split[0], split[1], split[2].toIntOrNull() ?: 0))
    }

    while (true) {
        println(
            "Меню: \n" +
                    "1 – Учить слова\n" +
                    "2 – Статистика\n" +
                    "0 – Выход"
        )
        val answer = readln().toInt()
        when (answer) {
            1 -> {
                while (true) {
                    val listUnlearnedWords = dictionary.filter { it.correctAnswersCount < 3 }
                    if (listUnlearnedWords.isEmpty()) {
                        println(ALL_WORDS_LEARNED)
                        break
                    } else {
                        val answerOptions = listUnlearnedWords.shuffled().take(4)
                        println("Переведите слово: ${answerOptions.random().translate}")
                        println("Варианты ответов:")
                        answerOptions.forEach { println(" - ${it.original}") }
                        break
                    }
                }
            }

            2 -> {
                val learnedWordsListSize = dictionary.filter { it.correctAnswersCount >= 3 }.size
                println(
                    "Выучено $learnedWordsListSize из ${dictionary.size} " +
                            "слов | ${(learnedWordsListSize / dictionary.size.toDouble() * 100).toInt()}%"
                )
            }

            0 -> break
            else -> println("Выберете корректный пункт меню")
        }
    }
}