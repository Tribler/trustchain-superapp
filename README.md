# TrustChain Super App [![Build Status](https://github.com/Tribler/trustchain-superapp/workflows/build/badge.svg)](https://github.com/Tribler/trustchain-superapp/actions)

This repository contains a collection of Android apps built on top of [IPv8](https://github.com/MattSkala/kotlin-ipv8) (our P2P networking stack) and [TrustChain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md) (a scalable, distributed, pair-wise ledger). All applications are built into a single APK, following the concept of [super apps](https://home.kpmg/xx/en/home/insights/2019/06/super-app-or-super-disruption.html) – an emerging trend that allows to provide an ecosystem for multiple services within a single all-in-one app experience.

## Apps

### TrustChain Explorer

**TrustChain Explorer** allows to browse the TrustChain blocks stored locally on the device and crawl chains of other connected peers. It also demonstrates how to interact with `TrustChainCommunity`. It defines its own `DemoCommunity` to ensure that all users using the app are able to discover each other easily. The content of the app is split into several tabs:

- **Peers:** A list of discovered peers in `DemoCommunity`. For each peer, there is a time since the last sent and received message, and an average ping latency. After clicking on the peer item, a list of mutual blocks in TrustChain is shown. It is possible to create and send a new proposal block by clicking on the plus icon. A crawl request send be sent by clicking on the refresh button.
- **Chains:** A list of discovered chains in `TrustChainComunity`, ordered by their length. After clicking on the item, the list of stored blocks is shown.
- **All Blocks:** A stream of all received blocks, updated in real-time as new blocks are received from the network.
- **My Chain:** A list of blocks in which the current user is participating either as a sender or a receiver. It is possible to create a new self-signed block by clicking on the plus icon. It is posible to sign received blocks if they are not defined to be signed automatically.

<img src="https://raw.githubusercontent.com/Tribler/kotlin-ipv8/master/doc/demo-android.png" width="180"> <img src="https://raw.githubusercontent.com/Tribler/kotlin-ipv8/master/doc/demo-android-trustchain.png" width="180">

### Debug

**Debug** shows various information related to connectivity, including:

- The list of bootstrap servers and their health. The server is considered to be alive if we received a response from it within the last 120 seconds.
- The number of connected peers in the loaded overlays.
- The LAN address estimated from the network interface and the WAN address estimated from the packets received from peers outside of our LAN.
- The public key and member ID (SHA-1 hash of the public key)
- TrustChain statistics (the number of stored blocks and the length of our own chain)

<img src="https://raw.githubusercontent.com/Tribler/kotlin-ipv8/master/doc/demo-android-debug.png" width="180">

### Do you want to add your own app?

- [Adding your own app to the TrustChain Super App](AppTutorial.md).

## Build

If you want to build an APK, run the following command:

```
./gradlew :app:buildDebug
```

The resulting APK will be stored in `app/build/outputs/apk/debug/app-debug.apk`.

## Install

You can also build and automatically install the app on all connected Android devices with a single command:

```
./gradlew :app:installDebug
```

*Note: It is required to have an Android device connected with USB debugging enabled before running this command.*

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
