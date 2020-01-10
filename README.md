# kotlin-ipv8

## What is IPv8?

IPv8 is a P2P protocol providing authenticated communication. Peers in the network are identified by public keys, and physical IP addresses are abstracted away. The protocol comes with integrated NAT puncturing, allowing P2P communication without using any central server. The protocol is easily extensible with the concept of *communities*, which represent services implemented on top of the protocol.

## Why Kotlin implementation?

IPv8 has been originally implemented in [Python](https://github.com/Tribler/py-ipv8) more than a decade ago and continuously improved since then. However, in the current age of smartphones, a mobile phone has become the primary communication device. Until now, there has been no library facilitating practical direct device to device communication. As there is no efficient way to run Python on Android, we have decided to re-implement the IPv8 protocol in [Kotlin](https://kotlinlang.org/), and provide this missing library.

Kotlin is a relatively new, but increasingly popular, modern statically typed programming language. Compared to Java, it features null safety, type inference, coroutines, and is more expressive. Moreover, it is 100% interoperable with Java, so applications using this library can still be built in Java.

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

The generated report will be stored in `app/build/reports/jacoco/html/index.html`.

### Read more

- [Message Serialization](doc/Serialization.md)
- [py-ipv8 Documentation](https://py-ipv8.readthedocs.io/en/latest/)
