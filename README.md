## EuroToken Multi-Currency Payment Request Feature For the SuperApp

The payment request system (Dutch: Betaalverzoek) has been increasingly popular among the population due to its simplicity in payment request and management. Implementing payment requests would potentially enhance the usability of the EuroToken among the consumers. This feature is widely implemented nowadays, however, it has not been developed into the SuperApp yet. Furthermore, our developed feature also enhances adoptability by enabling future users to request and pay in multiple currencies, by automatically connecting to a trusted exchange and carry out the "swap" on behave of the user. 

At the moment, the implementation only supports EUR and EuroToken. This implementation also tests the usability of the REST API of the exchange by putting it in use in the different features. At the same time we would like to identify potential weakness of the Value Transfer app and provide suggestion for the future.


## Prerequisite
[EuroToken: An offline-capable Central Bank Digital Currency](https://repository.tudelft.nl/islandora/object/uuid%3A132faae8-6883-454f-a8ce-94735340dce9?collection=education)

[EuroToken implementation in the Superapp](https://github.com/Tribler/trustchain-superapp/tree/master/eurotoken/src/main/java/nl/tudelft/trustchain/eurotoken)

## Design & Implementation
Next to the extension of front-end elements to facilitate payment request and (encrypted) link handling, the project also modified both the tokenization and detokenization flow in order to "swap" the currencies in order for the multi-currency payment to work.

**Modified tokenization flow**
![Untitled Diagram drawio(5)](https://user-images.githubusercontent.com/16018391/161035703-7d97b8a4-fb3f-49a4-8287-2f0711887e2d.png)

The figure above is the modified tokenization flow (red elements). The original can be found in the EuroToken Thesis. In this flow, we work with the exchange API in order to pay out EuroToken directly to the payment requester. In a similar fashion, we modified the detokenization flow by setting the IBAN of the receiver as the payout address.


## Installation
### Requirements:
### Steps:


##Suggestions:
Even though our implementation of link sharing is very straight forward, during this progress there is no guarantee that the person behind the public key is the intended benficiary. We have enable link signature to make sure that the message cannot be valid if tampered with, i.e. the information in the link being switched. However, there is a chance that even if the signature is valid, but the adversary managed to swap the original valid signed link with another signed link but with the keys from him/her self. In order to (partially) counteract this, we replaced the information in the link (receiver public key, amount, etc) with a transaction ID. The suggestion for the future is that, a validation mechanism should be implemented either in the server (with a trusted authority) or user to make sure the link is shared by a receiver linked to the strong identity (for example passport-enrolled identity in the app)


##Testings:

Tests were not written for Valuetransfer app previously, we managed to add several tests as well, bringing the test coverage slightly higher.
The following components were tested (check our code):

Validation of correct IBAN input

Validation of correct Amount input

Validation of correct link signing and verification

Test if eurotoken is received at scenario 1

Test if eurotoken is received at scenario 2

Test if euros is received at scenario 3

Test link creation at scenario 1/2 and 3


## Support:
If you come across issues with this feature or simply have implementation questions, you can send them to:
t.l.nguyen@student.tudeflt.nl

