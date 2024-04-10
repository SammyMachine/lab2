import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.time.temporal.ChronoField
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class UdpClient internal constructor(private val datagramSocket: DatagramSocket) : Closeable {
    constructor() : this(DatagramSocket())

    private var counter = 0
    private val buffer = ByteArray(128)

    fun ping(target: String, port: Int, timeout: Duration = 1.seconds): Duration {
        val sendingTime = LocalDateTime.now()
        counter++
        datagramSocket.soTimeout = timeout.inWholeMilliseconds.toInt()
        val message = "Sending request to $target: attempt $counter at ${sendingTime.get(ChronoField.HOUR_OF_DAY)}:${sendingTime.get(ChronoField.MINUTE_OF_HOUR)}:${sendingTime.get(ChronoField.SECOND_OF_MINUTE)}".toByteArray()
        println("Request to $target:$port")
        datagramSocket.send(DatagramPacket(message, message.size, InetAddress.getByName(target), port))

        val receivedDatagramPacket = DatagramPacket(buffer, buffer.size)
        try {
            datagramSocket.receive(receivedDatagramPacket)
        } catch (e: SocketTimeoutException) {
            println("\n$counter Request to $target failed\n")
            throw e
        }
        val receivingTime = LocalDateTime.now()
        val receivedMessage = String(receivedDatagramPacket.data, 0, receivedDatagramPacket.length)
        val rtt =
            (receivingTime.getLong(ChronoField.MILLI_OF_SECOND) - sendingTime.getLong(ChronoField.MILLI_OF_SECOND)).milliseconds
        println("\n$counter Response from $target:$port: $receivedMessage ###RTT### ${rtt.inWholeMilliseconds} ms\n")
        return rtt
    }

    override fun close() {
        datagramSocket.close()
    }
}