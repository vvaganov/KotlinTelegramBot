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
const val BASE_URL =  "https://api.telegram.org/bot"


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

    val trainer = LearnWordsTrainer()
    val httpClient = TelegramBotService(botToken)

    val json = Json {
        ignoreUnknownKeys = true
    }

    while (true) {
        Thread.sleep(2000)
        val getUpdateUrl = "$BASE_URL$botToken/getUpdates?offset=$lastUpdateId"
        val responseString = httpClient.getClient(getUpdateUrl)
        val firstUpdate = httpClient.getUpdateDataClass(responseString, json)
        lastUpdateId = firstUpdate?.updateId?.plus(1) ?: continue
        val message = firstUpdate.message?.text
        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        val data = firstUpdate.callbackQuery?.data



        if (message?.lowercase() == "hello") {
            sendMessage(json, chatId, "Hello", httpClient, botToken)
        }

        if (message?.lowercase() == "/start") {
            sendMenu(json,chatId, httpClient, botToken)
        }

        if (data?.lowercase() == "statistic_clicked") {
            val statistic = trainer.getStatistic()
            sendMessage(json,
                chatId,
                "Выучено ${statistic.learned} из ${statistic.total} слов || ${statistic.percent}%",
                httpClient,
                botToken
            )
        }

        if (data?.lowercase() == LEARN_WORD_BUTTON) {
            checkNextQuestionAndSend(trainer, chatId, httpClient, json, botToken)
        }

        if (startsWith(data)) {
            val index = data?.substringAfter("_")?.toInt()
            if (trainer.checkAnswer(index)) {
                sendMessage(json,chatId, "Правильно", httpClient, botToken)
                checkNextQuestionAndSend(trainer, chatId, httpClient, json, botToken)
            } else {
                sendMessage(json,
                    chatId,
                    "Не правильно! Правильный ответ - ${
                        (trainer.getQuestion()?.correctAnswer?.translate)
                            ?.replaceFirstChar { it.uppercase() }
                    }",
                    httpClient, botToken
                )
                checkNextQuestionAndSend(trainer, chatId, httpClient, json, botToken)
            }
        }
    }
}

fun startsWith(data: String?): Boolean {
    return data?.contains(CALLBACK_DATA_ANSWER_PREFIX) ?: false
}

fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, chatId: Long?, httpClient: TelegramBotService, json: Json, botToken: String) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        sendMessage(json, chatId, message = "Вы выучили все слова в базе!", httpClient, botToken)
    } else {
        sendQuestion(json, chatId, question, httpClient, botToken)
    }
}

fun sendMessage(json: Json, chatId: Long?, message: String, httpClient: TelegramBotService, botToken: String): String? {

    val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8)

    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = message
    )
    val requestBodyString = json.encodeToString(requestBody)
    val sendQuestionUrl = "$BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encoded"
    return httpClient.sendMessageClient(requestBodyString, sendQuestionUrl)
}

fun sendMenu(json: Json, chatId: Long?, httpClient: TelegramBotService, botToken: String): String? {

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
    val sendMenuUrl = "$BASE_URL$botToken/sendMessage"
    return httpClient.sendMessageClient(requestBodyString,sendMenuUrl)
}

fun sendQuestion(json: Json, chatId: Long?, question: Question, httpClient: TelegramBotService, botToken: String): String? {
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
    val sendQuestionUrl = "$BASE_URL$botToken/sendMessage"
    return httpClient.sendMessageClient(requestBodyString, sendQuestionUrl)
}