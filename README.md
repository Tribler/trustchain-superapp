# kotlin-ipv8 [![Build Status](https://travis-ci.org/MattSkala/kotlin-ipv8.svg?branch=master)](https://travis-ci.org/MattSkala/kotlin-ipv8)

## What is IPv8?

IPv8 is a P2P protocol providing authenticated communication. Peers in the network are identified by public keys, and physical IP addresses are abstracted away. The protocol comes with integrated NAT puncturing, allowing P2P communication without using any central server. The protocol is easily extensible with the concept of *communities* which represent services implemented on top of the protocol.

If you want to deep dive into technical details of the protocol and understand how existing communities work, please check out the [IPv8 Protocol Specification](doc/INDEX.md). You can also refer to the [py-ipv8 documentation](https://py-ipv8.readthedocs.io/en/latest/).

## Why Kotlin implementation?

[IPv8 has been originally implemented in Python](https://github.com/Tribler/py-ipv8) more than a decade ago and continuously improved since then. However, smartphones have become the primary communication device, and there has been yet no library facilitating direct device to device communication. As there is no efficient way to run Python on Android, we have decided to re-implement the IPv8 protocol stack in Kotlin, and provide this missing library.

Kotlin is a relatively new, but increasingly popular, modern, statically typed programming language. Compared to Java, it features null safety, type inference, coroutines, and is more expressive. Moreover, it is 100% interoperable with Java, so applications using this library can still be built in Java.


## Sample app

The repository includes a sample app that depends on the ipv8 library and demonstrates its usage. Currently, it only shows a list of discovered peers, the time since the last sent and received message, and average ping duration for each peer. The app can be installed with the following command:

```
./gradlew installPeerchatDebug
```

## Tests

We strive for a high code coverage to keep the project maintainable and stable. There are two types of tests: unit tests running locally on JVM, and instrumented tests relying on Android SDK that must be run on a physical device or emulator. Jacoco is used to report the unified code coverage for both types of tests.

Run unit tests:
```
./gradlew test
```

Run instrumented tests:
```
./gradlew connectedAndroidTest
```

Generate code coverage report:
```
./gradlew jacocoTestReport
```

The generated report will be stored in `ipv8/build/reports/jacoco/html/index.html`.

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
