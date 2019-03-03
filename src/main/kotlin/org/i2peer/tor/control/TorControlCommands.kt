package org.i2peer.tor.control

import kotlinx.coroutines.ObsoleteCoroutinesApi

/**
 * 3.1 Change the value of one or more configuration variables
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.setConfiguration(params: Map<String, String>) =
    send("SETCONF", params)

/**
 * 3.1 Sets password for configuration
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.setPassword(password: String) =
    setConfiguration(hashMapOf("HashedControlPassword" to password))

/**
 * 3.2 Remove all settings for a given configuration option entirely, assign its default value (if any), and
 * then assign the String provided.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.resetConfiguration(params: Map<String, String>) =
    send("RESETCONF", params)

/**
 * 3.3 Request the value of a configuration variable
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.getConfiguration(keywords: List<String>) = send("GETCONF", keywords)

/**
 * 3.4 Any events *not* listed in the SETEVENTS line are turned off; thus, sending SETEVENTS with an empty body
 * turns off all event reporting
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.setEvents(events: List<Events>) = send("SETEVENTS", events.map { it.name })

/**
 * 3.4
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.clearEvents() = send("SETEVENTS")

/**
 * 3.5 Authenticate to the server
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.authenticate(value: ByteArray? = null) = send(Authenticate(value))

/**
 * 3.6 Instructs the server to write out its config options into its torrc
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.saveConfiguration(force: Boolean = false) = send(SaveConfiguration(force))

/**
 * 3.7
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.signal(type: SignalType) = send("SIGNAL", type.name)

/**
 * 3.8 The client sends this message to the server in order to tell it that future SOCKS requests for connections
 * to the original address should be replaced with connections to the specified replacement address.
 *
 * The first address in each pair (the map key) is an "original" address; the second is a "replacement" address (the map value)
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.mapAddress(params: Map<String, String>) =
    send("MAPADDRESS", params)

/**
 * 3.9
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.getInfo(info: List<String>) = send("GETINFO", info)

/**
 * 3.10
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.extendCircuit(
    circuitID: String,
    serverSpec: List<String>? = null,
    purpose: Purpose? = Purpose.general
) = send(ExtendCircuit(circuitID, serverSpec, purpose))

/**
 * 3.11 Changes the circuit's purpose
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.setCircuitPurpose(circuitID: String, purpose: Purpose) =
    send("SETCIRCUITPURPOSE", listOf(circuitID, "purpose=${purpose.name}"))

/**
 * 3.12 Changes the descriptor's purpose
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.setRouterPurpose(nicknameOrKey: String, purpose: Purpose) =
    send("SETROUTERPURPOSE", listOf(nicknameOrKey, purpose.name))

/**
 * 3.13 Informs the server that the specified stream should be associated with the specified circuit.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.attachStream(streamID: String, circuitID: String, hopNum: Int)
        = send("ATTACHSTREAM", listOf(streamID, circuitID, "HOP=$hopNum"))

//TODO: 3.14

/**
 * 3.15 Tells the server to change the exit address on the specified stream.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.redirectStream(streamID: String, address: String) =
    send("REDIRECTSTREAM", listOf(streamID, address))

/**
 * 3.15 Tells the server to change the exit address on the specified stream.  If Port is specified, changes the destination
 * port as well.  No remapping is performed on the new provided address.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.redirectStream(streamID: String, address: String, port: Int) =
    send("REDIRECTSTREAM", listOf(streamID, address, port.toString()))

/**
 * 3.16
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.closeStream(streamID: String, reason: Reason) =
    send("CLOSESTREAM", listOf(streamID, reason.name))

/**
 * 3.17   Tells the server to close the specified circuit.   If "IfUnused" is provided, do not close the
 * circuit unless it is unused.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.closeCircuit(circuitID: String, unused: Boolean) =
    send("CLOSECIRCUIT", if (unused) listOf(circuitID, "IfUnused") else listOf(circuitID))

/**
 * 3.18 Tells the server to hang up on this controller connection
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.quit() = send("QUIT")

/**
 * 3.19 Enable these additional features
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.useFeature(features: List<Feature>) = send("USEFEATURE", features.map { it.name })

/**
 * TODO: 3.20 launches a remote hostname lookup request for every specified request (or reverse lookup if "mode=reverse" is specified)
 */

/**
 * 3.21 tells the controller what kinds of authentication are supported.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.protocolInfo() = send("PROTOCOLINFO")

/**
 * 3.22 allows a controller to upload the text of a config file to Tor over the control port.  This config file
 * is then loaded as if it had been read from disk.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.loadConfiguration(configText: String) = send(LoadConfiguration(configText))

/**
 * 3.23 This command instructs Tor to shut down when this control connection is closed.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.takeOwnership() = send("TAKEOWNERSHIP")

/**
 * 3.24 used to begin the authentication routine for the SAFECOOKIE method of authentication.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.authchallenge() = send(AuthChallenge())

/**
 *  3.25 Tells the server to drop all guard nodes. Do not invoke this command lightly; it can increase vulnerability
 *  to tracking attacks over time.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.dropGuards() = send("DROPGUARDS")

/**
 * TODO: 3.26 HSFetch
 */

/**
 * 3.27 Tells the server to create a new Onion ("Hidden") Service, with the specified private key and algorithm.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.addOnion(
    keyType: KeyType,
    keyBlob: String,
    ports: List<Port>,
    flags: List<OnionFlag>? = null,
    numStreams: Int? = 0,
    clientName: String? = null,
    clientBlob: String? = null
) = send(
    AddOnion(
        keyType = keyType, keyBlob = keyBlob, ports = ports,
        flags = flags, numStreams = numStreams, clientName = clientName, clientBlob = clientBlob
    )
)

/**
 * 3.28 Tells the server to remove an Onion ("Hidden") Service, that was previously created via an "ADD_ONION" command.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.deleteOnion(serviceId: String) = send("DEL_ONION", serviceId)

/**
 * TODO: 3.29 HSPOST
 */

/**
 * 3.30 This command instructs Tor to relinquish ownership of its control connection. As such tor will not shut down when
 * this control connection is closed.
 */
@ObsoleteCoroutinesApi
suspend fun TorControlChannel.dropOwnership() = send("DROPOWNERSHIP")


