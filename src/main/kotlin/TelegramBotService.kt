import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets


open class TelegramBotService(
    private val botToken: String
) {

    private val client: HttpClient = HttpClient.newBuilder().build()


    fun getUpdateDataClass(responseString: String, json: Json): Update? {
        val response = json.decodeFromString<Response>(responseString)
        val update = response.result
        val firstUpdate = update.firstOrNull()
        return firstUpdate
    }

    fun getClient(url:String): String {
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
    fun sendMessage(json: Json, chatId: Long?, message: String, botToken: String): String? {

        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8)

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = message
        )
        val requestBodyString = json.encodeToString(requestBody)
        val sendQuestionUrl = "$BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encoded"
        return sendMessageClient(requestBodyString, sendQuestionUrl)
    }

    private fun sendMessageClient(responseString: String, url: String): String? {
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(responseString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
    fun sendMenu(json: Json, chatId: Long?, botToken: String): String? {

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
        return sendMessageClient(requestBodyString,sendMenuUrl)
    }

    fun sendQuestion(json: Json, chatId: Long?, question: Question, botToken: String): String? {
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
        return sendMessageClient(requestBodyString, sendQuestionUrl)
    }
}
