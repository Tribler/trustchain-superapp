# Creating your first overlay

In this tutorial, we will learn how to create a simple overlay network, and send and handle custom messages. Finally, we will configure and start the IPv8 stack, and load our overlay with a discovery strategy, which will allow us to discover other peers in the community using the bootstrap server.

## Project setup

You can check the `demo-android` module to see how to configure an Android app module, initialize IPv8, and interact with the overlays. In essence, Android applications should depend on the `ipv8-android` module, which includes and exposes APIs of `ipv8` module, and defines some Android-specific dependencies and helper classes (e.g. `IPv8Android`).

The dependency is defined with the following line in `build.gradle`:

```
implementation project(':ipv8-android')
```

## Create a community

All communities have to extend an abstract `Community` class, which implements the `Overlay` interface. The only field left for us to define is `serviceId`. This should be an arbitrary 20-byte array represented as a hexadecimal string that uniquely identifies the community.

We therefore generate a `serviceId` and define `DemoCommunity`:

```kotlin
class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"
}
```

## Generate a private key

Every peer in has to generate a Curve25519 key pair, which provides an identity, and is used to sign and verify messages. A peer is identified by a *member ID* (*mid*), which is simply a SHA-1 hash of its public key.

We can generate a private key with `AndroidCryptoProvider.generateKey()`. The key can then be serialized to a byte array with `Key.keyToBin()` method, and again deserialized using `AndroidCryptoProvider.keyFromPrivateBin(ByteArray)`. When the app is launched for the first time, the key should be generated and persisted (e.g. in `SharedPreferences`). The previously generated key should be loaded on subsequent launches. The following code snippet does exactly that:

```kotlin
private const val PREF_PRIVATE_KEY = "private_key"

private fun getPrivateKey(): PrivateKey {
    // Load a key from the shared preferences
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val privateKey = prefs.getString(PREF_PRIVATE_KEY, null)
    return if (privateKey == null) {
        // Generate a new key on the first launch
        val newKey = AndroidCryptoProvider.generateKey()
        prefs.edit()
            .putString(PREF_PRIVATE_KEY, newKey.keyToBin().toHex())
            .apply()
        newKey
    } else {
        AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes())
    }
}
```

## Initialize IPv8

We now proceed to initialize IPv8 and load our overlay. While it is possible to instantiate `IPv8` class directly, on Android it is easier to prepare `IPv8AndroidFactory` and call `IPv8Android.init` method to initialize the stack.

First, we define a configuration for our overlay. `OverlayConfiguration` consists of an overlay factory and a list of discovery strategies. We can either define our own factory extending `Overlay.Factory` if we need to provide custom parameters to an overlay, or use the default implementation. Then, we create a factory for a discovery strategy. We use `RandomWalk`, a simple strategy discovering peers by performing a random walk in the network.

```kotlin
val demoCommunity = OverlayConfiguration(
    Overlay.Factory(DemoCommunity::class.java),
    listOf(RandomWalk.Factory())
)
```

Then, we define `IPv8Configuration`. The only required parameter is a list of overlays we want to load when the service is started. We pass the configuration created in the previous step.

```kotlin
val config = IPv8Configuration(overlays = listOf(
    demoCommunity
))
```

Finally, we create `IPv8AndroidFactory`, set the previously created configuration, our private key, and call `init`. This will start an IPv8 instance and create a foreground Android service to allow it to run even when the app is in the background.

```kotlin
IPv8Android.Factory(this)
    .setConfiguration(config)
    .setPrivateKey(getPrivateKey())
    .init()
```

We can subsequently get the IPv8 singleton instance by calling `IPv8Android.getInstance()`.

## Get a list of peers

If we run the app on multiple devices, they should be able to connect to each other and form an overlay. We can now get the instance of our community and query a list of connected peers by calling `Community.getPeers()`:

```kotlin
val community = IPv8Android.getInstance().getOverlay<DemoCommunity>()!!
val peers = community.getPeers()
for (peer in peers) {
    Log.d("DemoApplication", peer.mid)
}
```

## Send messages

Now that we have multiple peers connected, let's add some communication. First, we define a custom payload type implementing `Serializable` and `Deserializable` interfaces:

```kotlin
class MyMessage(val message: String) : Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<MyMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<MyMessage, Int> {
            return Pair(MyMessage(buffer.toString(Charsets.UTF_8)), buffer.size)
        }
    }
}
```

Next, we define a `broadcastGreeting` function in the `DemoCommunity` class which will iterate over all peers and please everyone with a greeting. We also define a message ID that is included as a prefix to the serialized message. The same ID will be used later to register a message handler for this message type.

```kotlin
private const val MESSAGE_ID = 1

fun broadcastGreeting() {
    for (peer in getPeers()) {
        val packet = serializePacket(MESSAGE_ID, MyMessage("Hello!"))
        send(peer.address, packet)
    }
}
```

## Process incoming messages

Finally, we add a message handler to parse incoming messages and print their sender and content to the log.

```kotlin
init {
    messageHandlers[MESSAGE_ID] = ::onMessage
}

private fun onMessage(packet: Packet) {
    val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
    Log.d("DemoCommunity", peer.mid + ": " + payload.message)
}
```

We can call `broadcastGreeting` function in our application e.g. in response to a button click, or we can add a loop to our `Activity` to make sure we greet everyone every second even without user interaction:

```kotlin
val community = IPv8Android.getInstance().getOverlay<DemoCommunity>()!!
lifecycleScope.launch {
    while (isActive) {
        community.broadcastGreeting()
        delay(1000)
    }
}
```

Congratulations, you have just created your first overlay with authenticated communication!
