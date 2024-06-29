package org.example

import java.io.File

fun main() {

    val dictionary: MutableList<Word> = mutableListOf()

    val wordFile = File("words.txt")
    val stringList = wordFile.readLines()
    for (i in stringList) {
        val split = i.split("|")
        dictionary.add(Word(split[0], split[1], split[2].toInt() ?: 0))
    }
    dictionary.forEach { println("${it.original} - ${it.translate}. Правильных ответов - ${it.correctAnswersCount}") }
}

