import org.example.LearnWordsTrainer
import org.example.Question
import org.example.Word
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val LEARN_WORD_BUTTON = "learn_words_clicked"
const val STATISTICS_BUTTON = "statistic_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0
    val updateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
    val chatIdRegex: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
    val messageRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()

    val trainer = LearnWordsTrainer()

    while (true) {
        Thread.sleep(2000)
        val updates = getUpdate(botToken, updateId)
        println(updates)
        val valueId = updateIdRegex.find(updates)?.groups?.get(1)?.value?.toIntOrNull() ?: continue
        updateId = valueId + 1
        val chatId = chatIdRegex.find(updates)?.groups?.get(1)?.value?.toInt() ?: break
        val message = messageRegex.find(updates)?.groups?.get(1)?.value
        val data = dataRegex.find(updates)?.groups?.get(1)?.value

        if (message?.lowercase() == "hello") {
            sendMessage(botToken, chatId, "Hello")
        }

        if (message?.lowercase() == "/start") {
            sendMenu(botToken, chatId)
        }

        if (data?.lowercase() == "statistic_clicked") {
            val statistic = trainer.getStatistic()
            sendMessage(
                botToken,
                chatId,
                "Выучено ${statistic.learned} из ${statistic.total} слов || ${statistic.percent}%"
            )
        }

        if (data?.lowercase() == LEARN_WORD_BUTTON) {
            checkNextQuestionAndSend(trainer, botToken, chatId)
        }

        if (startsWith(data)) {
            val index = data?.substringAfter("_")?.toInt()
            if (trainer.checkAnswer(index)) {
                sendMessage(botToken, chatId, "Правильно")
                checkNextQuestionAndSend(trainer, botToken, chatId)
            } else {
                sendMessage(
                    botToken,
                    chatId,
                    "Не правильно! Правильный ответ - ${(trainer.getQuestion()?.correctAnswer?.translate)?.replaceFirstChar { it.uppercase() }}"
                )
                checkNextQuestionAndSend(trainer, botToken, chatId)
            }
        }
    }
}

fun startsWith(data: String?): Boolean {
    return data?.contains(CALLBACK_DATA_ANSWER_PREFIX) ?: false
}

fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, botToken: String, chatId: Int) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        sendMessage(botToken, chatId, message = "Вы выучили все слова в базе!")
    } else {
        sendQuestion(botToken, chatId, question)
    }
}

fun getUpdate(botToken: String, updateId: Int): String {
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendMessage(botToken: String, chatId: Int, message: String): String {
    val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8)
    val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$encoded"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendMenu(botToken: String, chatId: Int): String {
    val sendMenuBody = """
        {
    "chat_id": $chatId,
    "text": "Основное меню",
    "reply_markup": {
       "inline_keyboard": [
            [
                {
                    "text": "Изучать слова",
                    "callback_data": "$LEARN_WORD_BUTTON"
                },
                {
                    "text": "Статистика",
                    "callback_data": "$STATISTICS_BUTTON"
                }
            ]
        ]
    }
}
    """.trimIndent()
    val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
        .build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}


fun sendQuestion(botToken: String, chatId: Int, question: Question): String? {
    val sendQuestionBody = """
        {
    "chat_id": $chatId,
    "text": "${question.correctAnswer.original}",
    "reply_markup": {
       "inline_keyboard": [
            [${
        question.variant
            .mapIndexed { index, word ->
                "{\"text\": \"${word.translate}\", \"callback_data\": \"$CALLBACK_DATA_ANSWER_PREFIX${index}\" }"
            }.joinToString()
    }
            ]
        ]
    }
}
    """.trimIndent()
    val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(sendQuestionBody))
        .build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}