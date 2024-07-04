package org.example

import java.io.File

const val NUMBER_CORRECT_ANSWERS = 3
const val NUMBER_POSSIBLE_ANSWERS = 4

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
                    val listUnlearnedWords = dictionary.filter { it.correctAnswersCount < NUMBER_CORRECT_ANSWERS }
                    if (listUnlearnedWords.isEmpty()) {
                        println("Все слова выучены")
                        break
                    } else {
                        val answerOptions = listUnlearnedWords.shuffled().take(NUMBER_POSSIBLE_ANSWERS)
                        println("Переведите слово: ${answerOptions.random().translate}")
                        println("Варианты ответов:")
                        answerOptions.forEach { println(" - ${it.original}") }
                        break
                    }
                }
            }

            2 -> {
                val learnedWordsListSize = dictionary.filter { it.correctAnswersCount >= NUMBER_CORRECT_ANSWERS }.size
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