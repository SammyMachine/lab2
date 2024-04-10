import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import kotlin.time.Duration


fun main(args: Array<String>) {
    run(arrayOf("8080", "10"))
}

fun run(args: Array<String>) {
    val serverPort = args.getOrElse(0) { "${(1024..65535).random()}" }.toInt()
    if (serverPort !in 0..65535) throw IllegalStateException("Port $serverPort should be in range in 0..65535")

    val numberOfPings = args.getOrElse(1) { "10" }.toInt()
    if (numberOfPings <= 0) throw IllegalStateException("Number or pings should be > 0")

    val udpServer = UdpServer(serverPort)
    val udpClient = UdpClient()

    runBlocking {
        launch {
            withContext(Dispatchers.IO) {
                udpServer.start()
            }
        }
        launch {
            delay(100)
            var fails = 0
            val rtt = mutableListOf<Duration>()
            var i = 0
            while (i < numberOfPings) {
                try {
                    rtt.add(udpClient.ping("127.0.0.1", serverPort))
                } catch (_: SocketTimeoutException) {
                    fails++
                }
                i++
            }
            udpClient.close()
            udpServer.close()
            println("\n###Stats###")
            println("###Received### ${numberOfPings - fails}\n###Sent### $numberOfPings\n###Lost### $fails (${(fails.toDouble() / numberOfPings) * 100}% loss)")
            println("###RTT stats in ms###")
            println("###Min time### ${rtt.minOrNull()?.inWholeMilliseconds ?: "None"} ms\n###Max time### ${rtt.maxOrNull()?.inWholeMilliseconds ?: "None"} ms\n###Avg time### ${rtt.sumOf { it.inWholeMilliseconds } / rtt.size} ms")
        }

    }
}