# TrustChain Super App [![Build Status](https://github.com/Tribler/trustchain-superapp/workflows/build/badge.svg)](https://github.com/Tribler/trustchain-superapp/actions)

This repository contains a collection of Android apps built on top of [IPv8](https://github.com/MattSkala/kotlin-ipv8) (our P2P networking stack) and [TrustChain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md) (a scalable, distributed, pair-wise ledger). All applications are built into a single APK, following the concept of [super apps](https://home.kpmg/xx/en/home/insights/2019/06/super-app-or-super-disruption.html) – an emerging trend that allows to provide an ecosystem for multiple services within a single all-in-one app experience.

## Apps

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

### Federated, privacy-preserving music recommendations via gossiping

This is a demonstration of machine learning which relies exclusively on edge computing. Music recommendation inside the MusicDAO is used to demonstrate gossip-based machine learning.

Every time a user opens MusicDAO, they are asked to reload the page in order to get recommendations. The recommendation engine yields two recommendations made by two different models: a musical feature-based model and a collaborative filtering model. The collaborative filtering model is based on federated matrix factorization as introduced in [this paper](https://dmle.iais.fraunhofer.de/papers/hegedus2019decentralized.pdf). The feature-based models are from this [paper](https://arxiv.org/pdf/1109.1396.pdf), called Adaline and Pegasos. These models are trained on audio features extracted from music files with the [Essentia library](https://essentia.upf.edu/).
<img src="gossipML/docs/imgs/overview.png" height="400px">

The feature-based models are gossiped along random walks through the network. At each peer they are merged and re-trained on peer's local data. The matrix factorization model seeks to learn a factorization of the user-song matrix. This means that one of the two factors contains only information on how users generally rate each song. This matrix can then be gossiped around the network while a user's personal vector as well as their listening history are kept private.
 - [More about federated machine learning using gossiping for music recommendations](gossipML/README.md)

### Do you want to add your own app?

- [Adding your own app to the TrustChain Super App](doc/AppTutorial.md)

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

Run unit tests:
```
./gradlew test
```

Run instrumented tests:
```
./gradlew connectedAndroidTest
```

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
