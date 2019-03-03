package org.i2peer.tor.control

import com.google.common.collect.ArrayListMultimap
import kotlinx.coroutines.channels.SendChannel
import java.util.*

/**
 * Tor control event (code)
 */
const val TOR_CONTROL_EVENT = "system.TOR_CONTROL_EVENT"
const val TOR_CONTROL_TRANSACTION = "system.TOR_CONTROL_TRANSACTION"

class TorControlContext() {

    private val actors = ArrayListMultimap.create<String, SendChannel<Any>>()

    @Synchronized
    fun addEventChannel(actor: SendChannel<Any>) = actors.put(TOR_CONTROL_EVENT, actor)

    @Synchronized
    fun addTransactionChannel(actor: SendChannel<Any>) = actors.put(TOR_CONTROL_TRANSACTION, actor)

    @Synchronized
    fun removeEventChannel(actor: SendChannel<Any>) = actors.remove(TOR_CONTROL_EVENT, actor)

    @Synchronized
    fun removeTransactionChannel(actor: SendChannel<Any>) = actors.remove(TOR_CONTROL_TRANSACTION, actor)

    @Synchronized
    fun eventChannel(): List<SendChannel<Any>> = Collections.unmodifiableList(actors.get(TOR_CONTROL_EVENT))

    @Synchronized
    fun transactionChannel(): List<SendChannel<Any>> = Collections.unmodifiableList(actors.get(
        TOR_CONTROL_TRANSACTION))

    @Synchronized
    fun emptyChannels() = actors.clear()

}