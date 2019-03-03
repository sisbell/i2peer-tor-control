package org.i2peer.tor.control

import okio.ByteString
import org.i2peer.tor.control.Encoder.hex

sealed class TorControlMessage {

    abstract fun encode(): ByteArray

    protected fun joinArgs(command: String, keywords: List<String?>): ByteArray =
        keywords.filter { !it.isNullOrBlank() }.joinToString(
            separator = " ",
            prefix = "$command ",
            postfix = "\r\n"
        ).toByteArray()
}

class NoArgsMessage(private val command: String) :TorControlMessage() {
    override fun encode(): ByteArray = noArgs(command)
}

class OneArgMessage(private val command: String, private val arg: String) :TorControlMessage() {
    override fun encode(): ByteArray = joinArgs(command, listOf(arg))
}

class MapMessage(private val command: String, private val params: Map<String, String>) :TorControlMessage() {
    override fun encode(): ByteArray = joinArgs(command, params.map { "${it.key}=${it.value}" })
}

class ListMessage(private val command: String, private val elements: List<String>) :TorControlMessage() {
    override fun encode(): ByteArray = joinArgs(command, elements)
}

fun noArgs(command: String) = "$command\r\n".toByteArray()

class AddOnion(
    private val keyType: KeyType,
    private val keyBlob: String,
    private val ports: List<Port>,
    private val flags: List<OnionFlag>? = null,
    private val numStreams: Int? = 0,
    private val clientName: String? = null,
    private val clientBlob: String? = null
) : TorControlMessage() {

    override fun encode(): ByteArray {
        var result = "ADD_ONION $key"
        if (flags != null) result += " ${flag()}"
        result += " $maxStreams $port"
        if (clientName != null) result += " $client"
        return ("$result\r\n").toByteArray()
    }

    private val key = "${keyType.name}:$keyBlob"

    private fun flag() = "Flags=${flags!!.joinToString()}"

    private val maxStreams = "MaxStreams=$numStreams"

    private val port = ports.joinToString(separator = " ")

    private val client = clientName + if (clientBlob != null) ":$clientBlob" else ""
}

class Authenticate(private val value: ByteArray? = null) : TorControlMessage() {
    override fun encode(): ByteArray = if (value == null) noArgs(command) else joinArgs(
        command,
        listOf(hex(value))
    )

    private val command = "AUTHENTICATE"
}

class AuthChallenge : TorControlMessage() {
    override fun encode() = joinArgs("AUTHCHALLENGE", listOf("SAFECOOKIE", nonce()))

    private fun nonce() = hex("12") + "/${CodeGenerator.generateCode(32)}"
}

class ExtendCircuit(
    private val circuitID: String, private val serverSpec: List<String>? = null,
    private val purpose: Purpose? = Purpose.general
) : TorControlMessage() {
    override fun encode(): ByteArray =
        joinArgs(
            "EXTENDCIRCUIT", listOf(
                circuitID, serverSpec!!.joinToString { "," },
                if (purpose != null) "purpose=$purpose" else null
            )
        )
}

class LoadConfiguration(private val configText: String) : TorControlMessage() {
    override fun encode() = "+LOADCONF\r\n$configText\r\n.\r\n".toByteArray()
}

class SaveConfiguration(private val force: Boolean) : TorControlMessage() {
    override fun encode() = if (force) joinArgs("SAVECONF", listOf("FORCE")) else noArgs("SAVECONF")
}

/*
class AuthChallenge(private val clientNonce: ClientNonce) : TorControlMessage() {
    fun encode() = "AUTHCHALLENGE SAFECOOKIE ${clientNonce.asText()}\r\n".getBytes()
}
*/

object Encoder {
    fun quote(value: String) = "\"$value\""

    fun hex(value: String): String = ByteString.encodeUtf8(value).hex()

    fun hex(value: ByteArray): String = ByteString.of(value, 0, value.size).hex()
}

enum class Purpose {
    general, controller
}

enum class Events {
    CIRC, STREAM, ORCONN, BW, DEBUG, INFO, NOTICE, WARN, ERR, NEWDESC, ADDRMAP, AUTHDIR_NEWDESCS
}

enum class SignalType {
    RELOAD, SHUTDOWN, DUMP, DEBUG, HALT, HUP, INT, USR1, USR2, TERM, NEWNYM, CLEARDNSCACHE, HEARTBEAT
}

enum class Feature {
    EXTENDED_EVENTS, VERBOSE_NAMES
}

enum class Reason {
    NONE, TORPROTOCOL, INTERNAL, REQUESTED,
    HIBERNATING, RESOURCELIMIT, CONNECTFAILED,
    OR_IDENTITY, OR_CONN_CLOSED, TIMEOUT,
    FINISHED, DESTROYED, NOPATH, NOSUCHSERVICE,
    MEASUREMENT_EXPIRED
}

enum class OnionFlag {
    DiscardPk, Detach, BasicAuth, NonAnonymous, MaxStreamsCloseCircuit
}

enum class KeyType(value: String) {
    NEW("NEW"), RSA1024("RSA1024"), ED25519V3("ED25519-V3")
}

class Port(private val virtualPort: Int, private val target: String? = null) {
    override fun toString() = "Port=$virtualPort" + if (target != null) ",$target" else ""
}

