import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

open class TelegramBotService(
    private val botToken: String
) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getUpdateDataClass(responseString: String): Update? {
        val response = json.decodeFromString<Response>(responseString)
        val update = response.result
        val firstUpdate = update.firstOrNull()
        return firstUpdate
    }

    fun getClient(updateId: Long): String {
        val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendClient(encoded: String, chatId: Long?, requestBody: SendMessageRequest): String {
        val requestBodyString = json.encodeToString(requestBody)
        val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$encoded"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessageClient(requestBody: SendMessageRequest): String {
        val requestBodyString = json.encodeToString(requestBody)
        val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}
