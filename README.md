## EuroToken Multi-Currency Payment Request Feature For the SuperApp

The payment request system (Dutch: Betaalverzoek) has been increasingly popular among the population due to its simplicity in payment request and management. Implementing payment requests would potentially enhance the usability of the EuroToken among the consumers. This feature is widely implemented nowadays, however, it has not been developed into the SuperApp yet. Furthermore, our developed feature also enhances adoptability by enabling future users to request and pay in multiple currencies, by automatically connecting to a trusted exchange and carry out the "swap" on behave of the user. At the moment, the implementation only supports EUR and EuroToken. This implementation also tests the usability of the REST API of the exchange by putting it in use in the different features. At the same time we would like to identify potential weakness of the Value Transfer app and provide suggestion for the future.


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


## Challenges
Since the SuperApp is still a prototype developed by students from prior years, documentation, guide, and testing of the SuperApp are minimal. There were several challenges that we overcame in order to develop this feature:
- Setting the gateway up and running during week 2: Our implementation requires the payment request to involve the exchange, we had trouble with setting up the exchange, particularly the exchange front end. (Erwin)
- We had trouble working with the Exchange's rest API during week 3: We had to set up our own exchange on the local host which by default uses HTTP. Since Android does not allow connecting through HTTP, we could not implement the feature with the REST API and had to hardcode some elements. (Mostafa)
- As mentioned previously, there were no tests for the Value Transfer app, hence we have developed some tests, mostly focused on our implementation to make sure that no issues will occur with future implementation 
- Signing the sharing link enhances security for the feature, however, there was an issue with the code being open-sourced. Hence everyone can have see how the private and public key pair being generated (Theodoros)

##Suggestions:



## Support:
If you come across issues with this feature or simply have implementation questions, you can send them to:
t.l.nguyen@student.tudeflt.nl

