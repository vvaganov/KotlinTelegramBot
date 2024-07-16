import org.example.LearnWordsTrainer
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val LEARN_WORD_BUTTON = "learn_words_clicked"
const val STATISTICS_BUTTON = "statistic_clicked"
const val EXIT_BUTTON = "exit_clicked"

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0
    val updateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
    val chatIdRegex: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
    val messageRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()

    val trainer = LearnWordsTrainer()
    val statistic = trainer.getStatistic()

    while (true) {
        Thread.sleep(2000)
        val updates = getUpdate(botToken, updateId)
        println(updates)
        val valueId = updateIdRegex.find(updates)?.groups?.get(1)?.value?.toIntOrNull() ?: continue
        updateId = valueId + 1
        val chatId = chatIdRegex.find(updates)?.groups?.get(1)?.value?.toInt()
        val message = messageRegex.find(updates)?.groups?.get(1)?.value
        val data = dataRegex.find(updates)?.groups?.get(1)?.value


        if (message?.lowercase() == "hello" && chatId != null) {
            sendMessage(botToken, chatId, "Hello")
        }

        if (message?.lowercase() == "/start" && chatId != null) {
            sendMenu(botToken, chatId)
        }
        if (data?.lowercase() == "statistic_clicked" && chatId != null) {
            sendMessage(
                botToken,
                chatId,
                "Выучено ${statistic.learned} из ${statistic.total} слов || ${statistic.percent}%"
            )
        }

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
                },
                {
                    "text": "Выход",
                    "callback_data": "$EXIT_BUTTON"
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