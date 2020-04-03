# Trustchain Bid/Ask generator

This app can generate bid and asks which are received by the community where it exists in.
The bid and asks can either be send automatically or manually. Those bids and asks will be send as IPv8 messages.

##Sending

1. Automatically:
   Toggle the "Generate payloads" switch to send random bid and asks to the community.
   Those bids and asks can only be seen from other peers, as not to overflow the GUI.

1. Manually:
    Press the "New Payload" button.
    You will be guided to the next fragment where you can either, bid or ask from BTC to Dymbe Dollars.

##Buying

In the mainframe, upon clicking on an bid or ask offer, a proposal block is being send to make a transaction.
The receiving peer sends an agreement block (Signs block) to make the trade valid.
![payloadFragment](/GeneratorImages/payloadFragment.png)
![payloadCreateFragment](/GeneratorImages/payloadCreateFragment.png)
