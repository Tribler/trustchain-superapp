# kotlin-ipv8 [![Build Status](https://github.com/Tribler/kotlin-ipv8/workflows/build/badge.svg)](https://github.com/MattSkala/kotlin-ipv8/actions) [![codecov](https://codecov.io/gh/Tribler/kotlin-ipv8/branch/master/graph/badge.svg)](https://codecov.io/gh/Tribler/kotlin-ipv8)

## What is IPv8?

IPv8 is a P2P protocol providing authenticated communication. Peers in the network are identified by public keys, and physical IP addresses are abstracted away. The protocol comes with integrated NAT puncturing, allowing P2P communication without using any central server. The protocol is easily extensible with the concept of *communities* which represent services implemented on top of the protocol.

If you want to deep dive into technical details of the protocol and understand how existing communities work, please check out the [IPv8 Protocol Specification](doc/INDEX.md). You can also refer to the [py-ipv8 documentation](https://py-ipv8.readthedocs.io/en/latest/).

## Why Kotlin implementation?

[IPv8 has been originally implemented in Python](https://github.com/Tribler/py-ipv8) more than a decade ago and continuously improved since then. However, smartphones have become the primary communication device, and there has been yet no library facilitating direct device to device communication. As there is no efficient way to run Python on Android, we have decided to re-implement the IPv8 protocol stack in Kotlin, and provide this missing library.

Kotlin is a relatively new, but increasingly popular, modern, statically typed programming language. Compared to Java, it features null safety, type inference, coroutines, and is more expressive. Moreover, it is 100% interoperable with Java, so applications using this library can still be built in Java.

## Communities

The protocol is built around the concept of *communities*. A community (or an *overlay*) represents a service in the IPv8 network. Every peer can choose which communities to join when starting the protocol stack. The following communities are implemented by the IPv8 core:

- [DiscoveryCommunity](doc/DiscoveryCommunity.md) implements peer discovery mechanism. It tries to keep an active connection with a specified number of peers and keeps track of communities they participate in. It performs regular keep-alive checks and drops inactive peers. While it is possible to run IPv8 without using this community, it is not recommended.
- [TrustChainCommunity](doc/TrustChainCommunity.md) implements TrustChain, a scalable, tamper-proof and distributed ledger, built for secure accounting.

## Tutorials

- [Creating your first overlay](doc/OverlayTutorial.md)
- [Creating your first TrustChain application](doc/TrustChainTutorial.md)

## Project structure

The project is a composed of several modules:

- `ipv8` (JVM library) – The core of IPv8 implementation, pure Kotlin library module.
- `ipv8-android` (Android library) – Android-specific dependencies and helper classes (`IPv8Android`, `IPv8Android.Factory`) for running IPv8 on Android Runtime.
- `ipv8-jvm` (JVM library) – JVM-specific dependencies for running IPv8 on JVM.
- `demo-android` (Android app) – The Android app demonstrating the usage of `ipv8-android` library.
- `demo-jvm` (JVM app) - The CLI app demonstrating the usage of `ipv8-jvm` library.

Android apps using IPv8 should depend on the `ipv8-android` module, which also includes and exposes APIs of `ipv8` module.

## Sample app

The repository includes a sample app that depends on the IPv8 library and demonstrates its usage. It is  available both as a native app for Android, and as a command line application running locally on JVM on macOS, Linux, and Windows.

### Android

The **TrustChain Explorer** app mostly demonstrates  interaction with TrustChainCommunity. It defines its own DemoCommunity to ensure that all users using the app are able to discover each other easily. The content of the app is split into several tabs:

- **Peers:** A list of discovered peers in DemoCommunity. For each peer, there is a time since the last sent and received message, and an average ping latency. After clicking on the peer item, a list of mutual blocks in TrustChain is shown. It is possible to create and send a new proposal block by clicking on the plus icon.
- **Chains:** A list of discovered chains in TrustChainComunity, ordered by their length. After clicking on the item, the list of stored blocks is shown.
- **All Blocks:** A stream of all received blocks, updated in real-time as new blocks are received from the network.
- **My Chain:** A list of blocks in which the current user is participating either as a sender or a receiver. It is possible to create a new self-signed block by clicking on the plus icon. It is posible to sign received blocks.
- **Debug:** Various debug information including the number of connected peers in different overlays, estimated LAN and WAN address, and TrustChain statistics.

Install the app on Android:
```
./gradlew :demo-android:installDebug
```

*Note: It is required to have an Android device connected with USB debugging enabled before running this command.*

<img src="https://raw.githubusercontent.com/Tribler/kotlin-ipv8/master/doc/demo-android.png" width="180"> <img src="https://raw.githubusercontent.com/Tribler/kotlin-ipv8/master/doc/demo-android-trustchain.png" width="180"> <img src="https://raw.githubusercontent.com/Tribler/kotlin-ipv8/master/doc/demo-android-debug.png" width="180">

### JVM

The JVM app merely shows a list of connected peers in the CLI, to demonstrate the feasibility of running the stack without any Android dependencies.

Run the app locally in JVM:
```
./gradlew :demo-jvm:run
```

SLF4J with [SimpleLogger](http://www.slf4j.org/api/org/slf4j/impl/SimpleLogger.html) is used for logging. You can configure the behavior of the logger by providing supported system properties as arguments. E.g., if you want to see debug logs:
```
./gradlew :demo-jvm:run -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

<img src="https://raw.githubusercontent.com/Tribler/kotlin-ipv8/master/doc/demo-jvm.png" width="450">

## Tests

We strive for a high code coverage to keep the project maintainable and stable. All unit tests are currently able to run on JVM, there are no Android instrumented tests. Jacoco is used to report the  code coverage.

Run unit tests:
```
./gradlew test
```

Generate code coverage report:
```
./gradlew jacocoTestReport
```

The generated report will be stored in `ipv8/build/reports/jacoco/test/html/index.html`.

## Code style

[Ktlint](https://ktlint.github.io/) is used to enforce a consistent code style across the whole project.

Check code style:
```
./gradlew ktlintCheck
```

Run code formatter:
```
./gradlew ktlintFormat
```
