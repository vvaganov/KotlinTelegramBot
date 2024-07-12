import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0

    while (true) {
        Thread.sleep(2000)
        val updates = getUpdate(botToken, updateId)
        println(updates)

        val updateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
        val chatId: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
        val messageText: Regex = "\"text\":\"(.+?)\"".toRegex()

        val valueId = updateIdRegex.find(updates)?.groups?.get(1)?.value?.toIntOrNull() ?: continue
        updateId = valueId + 1
        println(valueId)
        val valueChatId = chatId.find(updates)?.groups?.get(1)?.value?.toInt()
        println(valueChatId)
        val message = messageText.find(updates)?.groups?.get(1)?.value
        println(message)
    }
}

fun getUpdate(botToken: String, updateId: Int): String {
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}