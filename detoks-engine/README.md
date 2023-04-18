# Blockchain Engineering 2023 - Token Transaction Engine II

This folder contains our implemented and working token transaction engine. This engine allows for a fast and lightweight solution for fast token transactions based on the [Trustchain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md). We make use of an [IPv8](https://github.com/Tribler/kotlin-ipv8) overlay on which we've built forward. The concept of Trustchain is one used for communicating and bookkeeping, to make sure the transactions are communicated properly, and that also the integrity can be maintained and ensured. 

*insert title screenshot here* 

## Content

Our app has the following content:

- **Token generation:** With our app it is possible to generate tokens on a large scale. The purpose of this is to create some form of currency to send to other peers currently active, you will also be able to see what tokens you have, and which ones you've recieved. To generate a token, simply use the button to generate one, then, select a peer in the current peers list, and then hit send to give them a token! If for some reason you don't like one of the tokens you've generated, you can select them and delete them too, or just delete them all.

*insert screenshot here*
  
- **Visible peers:** We maintain a list of the peers currently in proximity. It's very easily possible to see what peers are currently around with their id's in a handy list. It's also possible to refresh, as other peers may have joined in as well during the usage.

*insert screenshot here*

## The structure of the project

The project and the code can be found in the current folder, the ```trustchain-superapp/detoks-engine``` folder. Our code is split up into multiple files, with each having their own specific role to play. Most of the related code to the transaction engine can be found in the TransactionCommunty file, where we handle tasks such as message grouping. In addition, we split up the code handling the SQLite database management and the token management code up as well. In ```db``` the tasks regarding the former are handled, while in the ```manage_tokens``` folder the latter is done.

## The transaction protocol

## Design

When opening the application, you see a page with two buttons. These buttons both bring you to a different page. Manage tokens, where you can send and generate tokens, and benchmarks, where you can also see some statistics.

<img src="https://raw.githubusercontent.com/bbrockbernd/trustchain-superapp/blob/Documentation/detoks-engine/Screenshot_20230417_182253.png" width="180">

On the manage tokens page, you will find a list of all your tokens at the top, followed by a list of currently available peers. To create a new token, you can click on the "gen token" button located at the bottom of the page. Once you have accumulated enough tokens, you can send them to a selected peer by clicking on either the "send", "send 2", or "send 5" buttons, which will respectively send 1, 2, or 5 tokens. The application operates on the FIFO principle, meaning that the tokens generated first will be sent first. Lastly, you can refresh the page by clicking on the refresh button.

[SCREENSHOT]

The benchmarks page displays various statistics related to the application. At the top, you can view the number of tokens that you currently possess. Following that, you can find the percentage of sent packets that have successfully reached the designated peer. The throughput, measured in tokens per second, is displayed below the percentage. At the end of the statistics, you see the latency. After the statistics, a list of the currently available peers is displayed. At the bottom of the screen, you can find buttons specifically meant for generating or sending a large number of tokens.

[SCREENSHOT]

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

