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
                        val question = answerOptions.random()
                        println("Переведите слово: ${question.translate}")
                        println("Варианты ответов:")
                        answerOptions.forEachIndexed { index, word -> println("${index + 1} - ${word.original}") }
                        println("(0) - ВЫХОД")
                        println("Выберете правильный ответ")
                        val option = readln().toInt()
                        if (option == 0) {
                            break
                        } else if ((option - 1) == answerOptions.indexOf(question)) {
                            println("Ответ верный:)")
                            println("________________")
                            question.correctAnswersCount++
                            saveDictionary(dictionary, wordFile)

                        } else {
                            println("Ответ не верный:(")
                            println("________________")
                        }
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

fun saveDictionary(list: MutableList<Word>, file: File) {
    val newString = list.map { "${it.original}|${it.translate}|${it.correctAnswersCount}\n" }
    file.writeText(newString.joinToString(""))
}