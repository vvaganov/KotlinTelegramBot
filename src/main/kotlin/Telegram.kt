import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[0]
    val urlGetMi = "https://api.telegram.org/bot$botToken/getMe"
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates"

    val client: HttpClient = HttpClient.newBuilder().build()
    var request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMi)).build()
    var response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    println("getMy - ${response.body()}")
    request = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    response = client.send(request, HttpResponse.BodyHandlers.ofString())
    println("getUpdates - ${response.body()}")
}