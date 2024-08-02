import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val LEARN_WORD_BUTTON = "learn_words_clicked"
const val STATISTICS_BUTTON = "statistic_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,

    )

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long?,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>
)

@Serializable
data class InlineKeyboard(
    @SerialName("text")
    val text: String,
    @SerialName("callback_data")
    val callbackData: String,
)

fun main(args: Array<String>) {
    val botToken = args[0]
    var lastUpdateId = 0L

    val json = Json {
        ignoreUnknownKeys = true
    }

    val trainer = LearnWordsTrainer()

    while (true) {
        Thread.sleep(2000)
        val responseString = getUpdate(botToken, lastUpdateId)
        val response = json.decodeFromString<Response>(responseString)
        val update = response.result
        val firstUpdate = update.firstOrNull() ?: continue
        lastUpdateId = firstUpdate.updateId + 1

        val message = firstUpdate.message?.text

        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
//
        val data = firstUpdate.callbackQuery?.data

        if (message?.lowercase() == "hello") {
            sendMessage(json, botToken, chatId, "Hello")
        }

        if (message?.lowercase() == "/start") {
            sendMenu(json, botToken, chatId)
        }

        if (data?.lowercase() == "statistic_clicked") {
            val statistic = trainer.getStatistic()
            sendMessage(
                json,
                botToken,
                chatId,
                "Выучено ${statistic.learned} из ${statistic.total} слов || ${statistic.percent}%"
            )
        }

        if (data?.lowercase() == LEARN_WORD_BUTTON) {
            checkNextQuestionAndSend(json, trainer, botToken, chatId)
        }

        if (startsWith(data)) {
            val index = data?.substringAfter("_")?.toInt()
            if (trainer.checkAnswer(index)) {
                sendMessage(json, botToken, chatId, "Правильно")
                checkNextQuestionAndSend(json, trainer, botToken, chatId)
            } else {
                sendMessage(
                    json,
                    botToken,
                    chatId,
                    "Не правильно! Правильный ответ - ${(trainer.getQuestion()?.correctAnswer?.translate)?.replaceFirstChar { it.uppercase() }}"
                )
                checkNextQuestionAndSend(json, trainer, botToken, chatId)
            }
        }
    }
}

fun startsWith(data: String?): Boolean {
    return data?.contains(CALLBACK_DATA_ANSWER_PREFIX) ?: false
}

fun checkNextQuestionAndSend(json: Json, trainer: LearnWordsTrainer, botToken: String, chatId: Long?) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        sendMessage(json, botToken, chatId, message = "Вы выучили все слова в базе!")
    } else {
        sendQuestion(json, botToken, chatId, question)
    }
}

fun getUpdate(botToken: String, updateId: Long): String {
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendMessage(json: Json, botToken: String, chatId: Long?, message: String): String {
    val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8)

    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = message
    )
    val requestBodyString = json.encodeToString(requestBody)

    val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$encoded"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
        .build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendMenu(json: Json, botToken: String, chatId: Long?): String {

    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Основное меню",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard(text = "Изучать слова", callbackData = LEARN_WORD_BUTTON),
                    InlineKeyboard(text = "Показать статистику", callbackData = STATISTICS_BUTTON)
                )
            )
        )
    )
    val requestBodyString = json.encodeToString(requestBody)

    val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
        .build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendQuestion(json: Json, botToken: String, chatId: Long?, question: Question): String? {

    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = question.correctAnswer.original,
        replyMarkup = ReplyMarkup(
            listOf(question.variant.mapIndexed { index, word ->
                InlineKeyboard(
                    text = word.translate,
                    callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                )
            })
        )
    )

    val requestBodyString = json.encodeToString(requestBody)

    val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
        .build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}