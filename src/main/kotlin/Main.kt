package org.example

const val NUMBER_CORRECT_ANSWERS = 3
const val NUMBER_POSSIBLE_ANSWERS = 4

fun Question.asConsoleString(): String {
    val variants = this.variant.mapIndexed { index, word -> "${index + 1} - ${word.translate}" }.joinToString("\n")
    return this.correctAnswer.original + "\n" + variants + "\n" + "0 - Выйти в меню"
}

fun main() {

    val trainer = LearnWordsTrainer()

    while (true) {
        println("Меню:\n1 – Учить слова\n2 – Статистика\n0 – Выход")
        when (readln().toIntOrNull()) {
            1 -> {
                while (true) {
                    val question = trainer.getNextQuestion()
                    if (question == null) {
                        println("Все слова выучены")
                        break
                    } else {
                        println(question.asConsoleString())
                        val userAnswerInput = readln().toIntOrNull()
                        if (trainer.checkAnswer(userAnswerInput?.minus(1))) {
                            println("Правильно\n")
                        } else {
                            println("Не правильный ответ. ${question.correctAnswer.original} - ${question.correctAnswer.translate}\n")
                        }
                        if (userAnswerInput == 0) break
                    }
                }
            }

            2 -> {
                val statistic = trainer.getStatistic()
                println("Выучено ${statistic.learned} из ${statistic.total} слов | ${statistic.percent} %")
            }

            0 -> break
            else -> println("Выберете корректный пункт меню")
        }
    }
}