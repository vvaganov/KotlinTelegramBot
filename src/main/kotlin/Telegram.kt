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
        val updateIdRegex: Regex = "\"update_id\":(.+?),".toRegex()
        val matchResultId: MatchResult? = updateIdRegex.find(updates)
        val valueId = matchResultId?.groups?.get(1)?.value
        updateId = valueId?.toInt()?.plus(1) ?: 0
        println(valueId)

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
        val matchResult: MatchResult? = messageTextRegex.find(updates)
        val text = matchResult?.groups?.get(1)?.value
    }
}

fun getUpdate(botToken: String, updateId: Int): String {
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}