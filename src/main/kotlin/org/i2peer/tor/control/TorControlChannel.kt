package org.i2peer.tor.control

import arrow.core.Either
import arrow.core.Try
import arrow.core.getOrElse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.actor
import okio.BufferedSink
import okio.BufferedSource
import java.util.*

/**
 * Service for communicating with a Tor control port
 */
class TorControlChannel(
    private val source: BufferedSource,
    private val sink: BufferedSink,
    private val controlContext: TorControlContext
) {

    @ObsoleteCoroutinesApi
    suspend fun send(command: String) = torControlWriter.send(NoArgsMessage(command))

    @ObsoleteCoroutinesApi
    suspend fun send(command: String, arg: String) = torControlWriter.send(OneArgMessage(command, arg))

    @ObsoleteCoroutinesApi
    suspend fun send(command: String, items: List<String>) = torControlWriter.send(ListMessage(command, items))

    @ObsoleteCoroutinesApi
    suspend fun send(command: String, params: Map<String, String>) = torControlWriter.send(MapMessage(command, params))

    @ObsoleteCoroutinesApi
    suspend fun send(message: TorControlMessage) = torControlWriter.send(message)

    /**
     * Reads from the Tor control endpoint
     */
    @ObsoleteCoroutinesApi
    private val torControlReader = kotlin.concurrent.fixedRateTimer(
        name = "read-socket",
        initialDelay = 0, period = 300
    ) {
        while (!source.exhausted()) {
            val result = source.readTorControlResponse().fold(
                {

                },
                {
                    GlobalScope.async {
                        torControlMessageTransformer.send(Either.right(it))
                    }
                })
        }
    }

    /**
     * Writes a TorControlMessage to the Tor control endpoint (sink)
     */
    @ObsoleteCoroutinesApi
    private val torControlWriter = GlobalScope.actor<TorControlMessage> {
        for (message in channel) {
            torControlMessageTransformer.send(Either.left(message))
            try {
                sink.write(message.encode())
                sink.flush()
            } catch (e: Exception) {
                e.printStackTrace()
                //TODO: Send error
            }
        }
    }

    /**
     *
     */
    @ObsoleteCoroutinesApi
    private val torControlMessageTransformer = GlobalScope.actor<Either<TorControlMessage, TorControlResponse>> {
        val messages = LinkedList<TorControlMessage>()
        for (message in channel) {
            when (message) {
                is Either.Left -> messages.add(message.a)
                is Either.Right -> when (message.b.code) {
                    250 -> callbackActor.send(TorControlTransaction(messages.pop(), message.b))
                    650 -> callbackActor.send(TorControlEvent(message.b))
                }
            }
        }
    }

    /**
     * Sends tor control events and transactions to any registered listeners (ActorRegistry)
     */
    @ObsoleteCoroutinesApi
    private val callbackActor = GlobalScope.actor<Any> {
        for (message in channel) {
            when (message) {
                is TorControlEvent -> {
                    controlContext.eventChannel().forEach {
                        it.send(TorControlEvent(message.response))
                    }
                }
                is TorControlTransaction -> {
                    controlContext.transactionChannel().forEach {
                        it.send(TorControlTransaction(message.request, message.response))
                    }
                }
            }
        }
    }

    companion object {

        /**
         * Transforms a list of tor control replies into a map of key/value pairs
         */
        fun toMap(lines: List<ReplyLine>?): Map<String, String?> {
            if (lines == null) return mapOf()
            val map = HashMap<String, String?>(lines.size)
            for ((_, msg) in lines) {
                val kv = msg.split("[=]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                map[kv[0]] = if (kv.size == 2) kv[1] else null
            }
            return map
        }
    }
}

fun BufferedSource.readTorControlReply(): Try<LinkedList<ReplyLine>> {
    return Try {
        val reply = LinkedList<ReplyLine>()
        var c: Char
        do {
            var line = readUtf8Line()
            println(line)
            if (line == null && reply.isEmpty()) break

            val status = line!!.substring(0, 3)
            val msg = line.substring(4)
            var rest: String? = null

            c = line.get(3)
            if (c == '+') {
                val data = StringBuilder()
                while (true) {
                    line = readUtf8Line()
                    if (line === ".")
                        break
                    if (line!!.startsWith("."))
                        line = line.substring(1)
                    data.append("$line\n")
                }
                rest = data.toString()
            }
            reply.add(ReplyLine(status.toInt(), msg, rest))
        } while (c != ' ')
        reply
    }
}

fun BufferedSource.readTorControlResponse(): Try<TorControlResponse> {
    return Try {
        val lines = readTorControlReply().getOrElse { null }
        val last = lines!!.peekLast()
        if ("OK" === last.msg) lines.removeLast()
        TorControlResponse(last.status, last.msg, TorControlChannel.toMap(lines))
    }
}

/**
 * Reply line from Tor control protocol
 */
data class ReplyLine(val status: Int, val msg: String, val rest: String?)

data class TorControlResponse(val code: Int, val message: String, val body: Any? = Unit)

data class TorControlEvent(val response: TorControlResponse)

data class TorControlTransaction(val request: TorControlMessage, val response: TorControlResponse)