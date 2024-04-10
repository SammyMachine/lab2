import java.io.Closeable
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.coroutineContext

class UdpServer internal constructor(private val datagramSocket: DatagramSocket) : Closeable {
    constructor(serverPort: Int) : this(DatagramSocket(serverPort))

    private lateinit var job: Job
    private var isClosed = false

    private val buffer = ByteArray(128)

    suspend fun start() {
        if (isClosed) return
        println("Server is running on port ${datagramSocket.localPort}")
        job = CoroutineScope(coroutineContext).launch {
            while (coroutineContext.isActive && !isClosed) {
                val datagramPacket = DatagramPacket(buffer, buffer.size)
                try {
                    datagramSocket.receive(datagramPacket) ?: break
                } catch (e: IOException) {
                    break
                }
                val receivedMessage = String(datagramPacket.data, 0, datagramPacket.length)
                println("Request from ${datagramPacket.address.hostAddress}:${datagramPacket.port}: $receivedMessage")
                val randomToDrop = (1..10).random()
                if (randomToDrop > 7) {
                    println("Drop packet")
                    continue
                }
                println("Response to ${datagramPacket.address.hostAddress}:${datagramPacket.port}")
                datagramSocket.send(
                    DatagramPacket(
                        receivedMessage.uppercase().toByteArray(),
                        receivedMessage.length,
                        datagramPacket.address,
                        datagramPacket.port
                    )
                )
            }
        }
    }

    override fun close() {
        isClosed = true
        runBlocking {
            job.cancel()
            datagramSocket.close()
        }
    }
}