package org.example

import java.io.File

fun main() {

    val wordFile = File("words.txt")
    val stringList = wordFile.readLines()
    stringList.forEach { println(it) }
}