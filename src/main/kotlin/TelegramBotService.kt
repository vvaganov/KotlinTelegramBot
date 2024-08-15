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

    fun sendMessageClient(responseString: String, url: String): String? {
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(responseString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}
