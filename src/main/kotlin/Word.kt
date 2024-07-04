package org.example

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)