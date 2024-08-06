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

const val URL_GET_UPDATE = "https://api.telegram.org/bot\$botToken/getUpdates?offset=\$updateId"

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

    while (true) {
        Thread.sleep(2000)
        val responseString = httpClient.getClient(lastUpdateId)
        val firstUpdate = httpClient.getUpdateDataClass(responseString)
        lastUpdateId = firstUpdate?.updateId?.plus(1) ?: continue
        val message = firstUpdate.message?.text
        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        val data = firstUpdate.callbackQuery?.data

        if (message?.lowercase() == "hello") {
            sendMessage(chatId, "Hello", httpClient)
        }

        if (message?.lowercase() == "/start") {
            sendMenu(chatId, httpClient)
        }

        if (data?.lowercase() == "statistic_clicked") {
            val statistic = trainer.getStatistic()
            sendMessage(
                chatId,
                "Выучено ${statistic.learned} из ${statistic.total} слов || ${statistic.percent}%",
                httpClient

            )
        }

        if (data?.lowercase() == LEARN_WORD_BUTTON) {
            checkNextQuestionAndSend(trainer, chatId, httpClient)
        }

        if (startsWith(data)) {
            val index = data?.substringAfter("_")?.toInt()
            if (trainer.checkAnswer(index)) {
                sendMessage(chatId, "Правильно", httpClient)
                checkNextQuestionAndSend(trainer, chatId, httpClient)
            } else {
                sendMessage(
                    chatId,
                    "Не правильно! Правильный ответ - ${
                        (trainer.getQuestion()?.correctAnswer?.translate)
                            ?.replaceFirstChar { it.uppercase() }
                    }",
                    httpClient
                )
                checkNextQuestionAndSend(trainer, chatId, httpClient)
            }
        }
    }
}

fun startsWith(data: String?): Boolean {
    return data?.contains(CALLBACK_DATA_ANSWER_PREFIX) ?: false
}

fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, chatId: Long?, httpClient: TelegramBotService) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        sendMessage(chatId, message = "Вы выучили все слова в базе!", httpClient)
    } else {
        sendQuestion(chatId, question, httpClient)
    }
}

fun sendMessage(chatId: Long?, message: String, httpClient: TelegramBotService): String {
    val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8)

    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = message
    )
    return httpClient.sendClient(encoded, chatId, requestBody)
}

fun sendMenu(chatId: Long?, httpClient: TelegramBotService): String {

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
    return httpClient.sendMessageClient(requestBody)
}

fun sendQuestion(chatId: Long?, question: Question, httpClient: TelegramBotService): String {

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
    return httpClient.sendMessageClient(requestBody)
}