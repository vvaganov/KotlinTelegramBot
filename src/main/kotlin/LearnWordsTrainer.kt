package org.example

import java.io.File

data class Statistics(
    val learned: Int,
    val total: Int,
    val percent: Int,
)

data class Question(
    val variant: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer(
    private val learnedAnswerCount: Int = 3,
    private val countOfQuestionWord: Int = 4
) {


    private val dictionary = loadDictionary()
    private var question: Question? = null


    fun getStatistic(): Statistics {
        val learned = dictionary.filter { it.correctAnswersCount >= 3 }.size
        val total = dictionary.size
        val percent = learned * 100 / total
        return Statistics(learned, total, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < learnedAnswerCount }
        if (notLearnedList.isEmpty()) return null
        val questionWord = if (notLearnedList.size < countOfQuestionWord) {
            val learnedList = dictionary.filter { it.correctAnswersCount >= learnedAnswerCount }.shuffled()
            notLearnedList.shuffled()
                .take(countOfQuestionWord) + learnedList.take(countOfQuestionWord - notLearnedList.size)
        } else {
            notLearnedList.shuffled().take(countOfQuestionWord)
        }.shuffled()
        val correctAnswer = questionWord.random()
        question = Question(variant = questionWord, correctAnswer = correctAnswer)
        return question
    }

    fun checkAnswer(userAnswerId: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variant.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerId) {
                it.correctAnswer.correctAnswersCount++
                saveDictionary()
                true
            } else {
                false
            }
        } ?: false
    }

    private fun loadDictionary(): List<Word> {
        try {
            val dictionary = mutableListOf<Word>()
            val wordFile = File("words.txt")
            wordFile.readLines().forEach {
                val split = it.split("|")
                dictionary.add(Word(split[0], split[1], split[2].toIntOrNull() ?: 0))
            }
            return dictionary
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("Не корректный файл")
        }
    }

    private fun saveDictionary() {
        val wordFile = File("words.txt")
        wordFile.writeText("")
        for (word in dictionary) {
            wordFile.appendText("${word.original}|${word.translate}|${word.correctAnswersCount}\n")
        }
    }
}