import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val LEARN_WORD_BUTTON = "learn_words_clicked"
const val STATISTICS_BUTTON = "statistic_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val RESET_CLICKED = "reset_clicked"
const val BASE_URL = "https://api.telegram.org/bot"


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
    val trainers = HashMap<Long, LearnWordsTrainer>()

    val json = Json {
        ignoreUnknownKeys = true
    }

    while (true) {
        Thread.sleep(2000)
        val getUpdateUrl = "$BASE_URL$botToken/getUpdates?offset=$lastUpdateId"
        val responseString = httpClient.getClient(getUpdateUrl)
        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, httpClient, trainers, botToken) }
        lastUpdateId = sortedUpdates.last().updateId + 1

    }
}

fun handleUpdate(
    update: Update,
    json: Json,
    httpClient: TelegramBotService,
    trainers: HashMap<Long, LearnWordsTrainer>,
    botToken: String,

    ) {
    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data
    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt") }

    if (message?.lowercase() == "/start") {
        httpClient.sendMenu(json, chatId, botToken)
    }

    if (data?.lowercase() == "statistic_clicked") {
        val statistic = trainer.getStatistic()
        httpClient.sendMessage(
            json,
            chatId,
            "Выучено ${statistic.learned} из ${statistic.total} слов || ${statistic.percent}%",
            botToken
        )
    }

    if (data?.lowercase() == LEARN_WORD_BUTTON) {
        checkNextQuestionAndSend(trainer, chatId, httpClient, json, botToken)
    }

    if (startsWith(data)) {
        val index = data?.substringAfter("_")?.toInt()
        if (trainer.checkAnswer(index)) {
            httpClient.sendMessage(json, chatId, "Правильно", botToken)
            checkNextQuestionAndSend(trainer, chatId, httpClient, json, botToken)
        } else {
            httpClient.sendMessage(json,
                chatId,
                "Не правильно! Правильный ответ - ${
                    (trainer.getQuestion()?.correctAnswer?.translate)
                        ?.replaceFirstChar { it.uppercase() }
                }",
                botToken
            )
            checkNextQuestionAndSend(trainer, chatId, httpClient, json, botToken)
        }
    }

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        httpClient.sendMessage(json, chatId, message = "Прогресс сброшен", botToken)
    }
}

fun startsWith(data: String?): Boolean {
    return data?.contains(CALLBACK_DATA_ANSWER_PREFIX) ?: false
}

fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer,
    chatId: Long?,
    httpClient: TelegramBotService,
    json: Json,
    botToken: String
) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        httpClient.sendMessage(json, chatId, message = "Вы выучили все слова в базе!", botToken)
    } else {
        httpClient.sendQuestion(json, chatId, question, botToken)
    }
}