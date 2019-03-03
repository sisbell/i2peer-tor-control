### Tor Control Library
This library provides a Kotlin implementation to access a Tor control socket.

#### How to Use

First, you will need to add Kotlin actors (or channels) to an instance of TorControlContext. 
The TorControlChannel instance will use the actors you add to notify your app of incoming 
tor control messages: _Transaction_ - these are synchronous replies with the 250 OK response 
AND _Event_ - these are asynchronous events with the 650 response code

    val controlContext = TorControlContext()
    controlContext.addTransactionChannel(GlobalScope.actor<Any> {
        for (message in channel) {
            val m = message as TorControlTransaction
            //Do Something with message
        }
    })
    controlContext.addEventChannel(GlobalScope.actor<Any> {
        for (message in channel) {
            val m = message as TorControlEvent
            //Do Something with messag
        }
    })

The following is a helper class for IO

    import okio.BufferedSink
    import okio.BufferedSource
    import okio.Okio
    import java.io.*
    object IO {
        fun source(input: InputStream): BufferedSource = Okio.buffer(Okio.source(input))

        fun sink(output: OutputStream): BufferedSink = Okio.buffer(Okio.sink(output))
    }


Next you will create an instance of the TorControlChannel. Notice how you pass in the TorControlContext
instance into the constructor. This is what allows the TorControlChannel to call back (through actors)
to your app. Make sure that a Tor instance is running first so that it has something to connect to.

    val socket = Socket("127.0.0.1", 9050)
    val source = IO.source(socket.getInputStream())
    val sink = IO.sink(socket.getOutputStream())
    val torchannel = TorControlChannel(source, sink, controlContext)
            
There a couple dozen commands you can call on the torChannel, but first you will need to authenticate

    torchannel.authenticate()//simple auth

Now you can request events

    torchannel.setEvents(Lists.newArrayList(
        Events.BW, Events.DEBUG,
        Events.CIRC, Events.ERR,
        Events.INFO, Events.NOTICE, Events.WARN))
        
These will come back on the event channel that you registered with the TorControlContext instance.

You can also do things like creating Onions.

    torchannel.addOnion(
        keyType = KeyType.NEW, keyBlob = "ED25519-V3",
        flags = listOf(OnionFlag.Detach),
        ports = listOf(Port(80, "127.0.0.1:8080"))
    }
    
#### Building

First make sure that you put in some dummy values into your gradle.properties file in the root
of the project. This is so the publishing repository metainfo won't fail the build with unrecognized properties. 

    username=dummy
    password=dummy
    
Now to build

    ./gradlew build
