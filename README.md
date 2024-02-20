# TrustChain Super App 
[![Build Status](https://github.com/Tribler/trustchain-superapp/workflows/build/badge.svg)](https://github.com/Tribler/trustchain-superapp/actions) [![ktlint](https://img.shields.io/badge/ktlint%20code--style-%E2%9D%A4-FF4081)](https://pinterest.github.io/ktlint/)

This repository contains a collection of Android apps built on top of [IPv8](https://github.com/MattSkala/kotlin-ipv8) (our P2P networking stack) and [TrustChain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md) (a scalable, distributed, pair-wise ledger). All applications are built into a single APK, following the concept of [super apps](https://home.kpmg/xx/en/home/insights/2019/06/super-app-or-super-disruption.html) – an emerging trend that allows to provide an ecosystem for multiple services within a single all-in-one app experience.

## Build Instructions

If you want to build an APK, run the following command:

```
./gradlew :app:buildDebug
```

The resulting APK will be stored in `app/build/outputs/apk/debug/app-debug.apk`.

### Install

You can also build and automatically install the app on all connected Android devices with a single command:

```
./gradlew :app:installDebug
```

*Note: It is required to have an Android device connected with USB debugging enabled before running this command.*

### Check
Run the Gradle check task to verify that the project is correctly set up and that tests pass:

```
./gradlew check
```

*Note: this task is also run on the CI, so ensure that it passes before making a PR.*  

### Tests

Run unit tests:
```
./gradlew test
```

Run instrumented tests:
```
./gradlew connectedAndroidTest
```

### Code style

[Ktlint](https://ktlint.github.io/) is used to enforce a consistent code style across the whole project. It is recommended to install the [ktlint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) for your IDE to get real-time feedback.


Check code style:
```
./gradlew ktlintCheck
```

Run code formatter:
```
./gradlew ktlintFormat
```

## Adding Your Own App
If you want to add your own app to the TrustChain Super App, you can follow the tutorial in the [AppTutorial.md](doc/AppTutorial.md) document.

## Apps

### On-Chain Democracy
We build a DAO for a better world. On-Chain Democracy is an Android application built on top of [IPv8](https://github.com/Tribler/kotlin-ipv8) and [Trustchain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md), and is integrated into the [Trustchain Superapp](https://github.com/Tribler/trustchain-superapp). It is a proof-of-concept implementation of a DAO system using Trustchain and Bitcoin. Trustchain is used for communication and bookkeeping while the Bitcoin blockchain is used to have collective multi-signature wallets for each DAO. The content of the app is split up in several tabs:
* **First Time Launch**: The first time the app is launched, the user must setup his bitcoin wallet. Afterwhich the chain will sync and he is routed to the main screens.
* **My DAO's**: A list of all DAO's that the user participates in. Selecting a DAO will allow a user to create a transfer proposal from that DAO.
* **All DAO's**: A list of all discovered DAO's in the network which the user can propose to join.
* **Proposals**: A list of all proposals that the user can vote on. This can either be join proposals or proposals from someone else to transfer funds from one of the DAO's.
* **My Wallet**: Overview of the used Bitcoin wallet and the ability to chain this to another.
* **Duplicate Wallet**: In case the user has wallet files for TestNet, Production or Regtest, the user is allowed to select which one to keep. After the user selected either one, the files belonging to other network type are backed up. This, thus, ensures that the wallet is not lost.

Currently, the On-Chain Democracy app only allows Regtest, since it uses a future update of Bitcoin called Taproot. Once Taproot is officially released, the app can support TestNet or Production again. Taproot allows the DAO to scale to thousands or even millions of users. The beauty of Taproot is that it uses Schnorr signatures for each transaction. This enables transaction sizes that are equal independent of the number of users in a DAO, since each user combines their signature collaberatively into one for the whole DAO. This also ensures privacy, since it is no longer possible to tell if a transaction's is from a single person, or a million of persons.

 <img src="currencyii/docs/images/screenshot_7.png" width="200px"> <img src="currencyii/docs/images/screenshot_6.png" width="200px"> <img src="currencyii/docs/images/screenshot_10.png" width="200px">
 <br />

 <p float="left">
 <img src="https://user-images.githubusercontent.com/23526224/111478102-0c54dc00-8730-11eb-9fbb-3cd65e2ee7ad.gif" width="200"/>
 <img src="https://user-images.githubusercontent.com/23526224/111478323-42925b80-8730-11eb-9bb9-d90b703385a3.jpeg" width="200"/>
 <img src="https://user-images.githubusercontent.com/23526224/111479002-e2e88000-8730-11eb-9246-dc487e5268b4.jpeg" width="200"/>
 </p>
 <br />

https://user-images.githubusercontent.com/23526224/116259903-85efd900-a776-11eb-93b1-384936d215c4.mp4


[More about On-Chain Democracy](currencyii/README.md)


### PeerChat

PeerChat implements a fully functional prototype of a distributed messaging app. First, the users have to exchange the public keys by scanning each other's QR code, or by copy-pasting the hexadecimal public keys. This guarantees authenticity of all messages which are signed by their author. It prevents man-in-the-middle and impersonation attacks.

An online indicator and the last message is shown for each contact. Users can exchange text messages and get acknowledgments when a message is delivered.

<img src="https://user-images.githubusercontent.com/1122874/82873653-1c979280-9f35-11ea-9d47-cea4e134a5b4.png" width="180"> <img src="https://user-images.githubusercontent.com/1122874/82873656-1dc8bf80-9f35-11ea-84b7-7139401560a4.png" width="180"> <img src="https://user-images.githubusercontent.com/1122874/82873659-1ef9ec80-9f35-11ea-95f6-99cbbc0510c9.png" width="180">

 <img src="https://user-images.githubusercontent.com/1122874/82873643-1a353880-9f35-11ea-8da3-24ce189c939d.png" width="180"> <img src="https://user-images.githubusercontent.com/1122874/82873661-1f928300-9f35-11ea-9955-6a7488936b02.png" width="180">

### Digital Euro

The Superapp is connected to the European IBAN Euro system.
You can send and receive digital Euros using QR-codes or build-in chat. **Experimental**.
Sending Euros is as easy as sending a smiley.
We did a test with native implementation of Trustchain and [a digital Euro last week](https://twitter.com/TriblerTeam/status/1367526077422256128).
Field test date: 4 March 2021 at 10:30am.
The native Android implementation in Kotlin is slowly getting mature.
Location: the bar Doerak (with a liquor license! This is a special place, therefore selected as the site for our trail.
Shops which sell coffee or closed canisters of alcohol are "essential shops" and therefore open in Corona times.) Loading real money on your phone requires an operational an open source [gateway](https://github.com/rwblokzijl/stablecoin-exchange) of Euros to digital Euros.
Discussed in this master thesis issue: https://github.com/Tribler/tribler/issues/4629

#### Double Spending mitigation
Double spending in EuroToken occurs when a malicious user sends a transaction to a wallet, and then sends the same transaction to another wallet whilst the second receiver is not aware of the first transaction.
This has been mitigated by introducing a web-of-trust, read more about this in the [EuroToken README.MD](eurotoken/README.MD)

Creative Commons CC0 license - share freely:

<IMG src="https://user-images.githubusercontent.com/325224/110597367-c1135a00-8180-11eb-9a75-207f4630ebb4.jpg" width=300>

Zooming into the actual mechanism of QR-Codes (Creative Commons CC0 license - share freely)

<IMG src="https://user-images.githubusercontent.com/325224/110597621-15b6d500-8181-11eb-828a-0f3409b6608c.jpg" width=150>
<img src="https://user-images.githubusercontent.com/446634/107397810-47e00300-6aff-11eb-8abe-5d345a096ade.jpeg" width=200>

![Demo](eurotoken/images/demo.gif)

### Debug

**Debug** shows various information related to connectivity, including:

- The list of bootstrap servers and their health. The server is considered to be alive if we received a response from it within the last 120 seconds.
- The number of connected peers in the loaded overlays.
- The LAN address estimated from the network interface and the WAN address estimated from the packets received from peers outside of our LAN.
- The public key and member ID (SHA-1 hash of the public key)
- TrustChain statistics (the number of stored blocks and the length of our own chain)

<img src="https://raw.githubusercontent.com/Tribler/kotlin-ipv8/master/doc/demo-android-debug.png" width="180">

### Freedom-of-Computing App

Freedom-of-Computing provides users with the ability to freely distribute and execute code in the form of APK applications on the trustchain superapp. In order to facilitate the sharing of applications, Freedom-of-Computing contains a gossiping mechanism which periodically shares local applications to other users and downloads unseen applications from other users. This sharing is conducted through a torrent peer-to-peer (P2P) network and uses the EVA Protocol as a fallback. Once the application has been downloaded by the users, they can dynamically load and execute it. The application, apart from being an .APK file, needs to have a specific format for the execution to work, the requirements/constraints are listed inside [the documentation](freedomOfComputing/README.md).

The left demo shows the upload procedure, while the right demo shows the download and code execution procedure.

<img src="doc/freedomOfComputing/create_torrent.gif" width="280"> <img src="doc/freedomOfComputing/download_seeded_apk.gif" width="280">

[More about Freedom-of-Computing App](freedomOfComputing/README.md)

### MusicDAO
In short, the MusicDAO  is an IPv8 app where users can share and discover tracks on the trustchain. Track streaming, downloading, and seeking interactions are done using JLibtorrent.

A user can publish a Release (which is an album/EP/single/...), after which the app creates a magnet link referring to these audio tracks. Then, the app creates a proposal block for the trustchain which contains some metadata (release date, title, ...) this metadata is submitted by the user with a dialog. When a signed block is discovered (currently are self-signed), the app tries to obtain the file list using JLibtorrent. Each file can be streamed independently on clicking the play button.

<img src="doc/musicdao/screen2.png" width="280"> <img src="doc/musicdao/screen1.png" width="280"> <img src="doc/musicdao/screen3.png" width="280">


**Videos**

Video 1: <a href="doc/musicdao/thesis2.mp4">Load example.</a> This uses a default magnet link for an album that has a decent amount of peers. The user submits the metadata and the block gets proposed and signed. Then playback.

Video 2: <a href="doc/musicdao/thesis3.mp4">Share track.</a> Note: as a fresh magnet link is generated in this video, there is only 1 peer. For this reason it will be difficult to obtain the metadata of the magnet link (cold start issue, write about this in thesis) so the video stops there.
