# DeToks: Upvote Token

DeToks is a decentralized TikTok skeleton application where several different groups implemented parts of the application. The application will utilize a peer-to-peer network consisting strictly of smartphones only. Currently, the application only works on Android-powered smartphones. The focus of this application will be the token economy, Torrent-based streaming videos, and basic app logic. Our responsibility for the project was the Upvote Token, and we will explain the components of this functionality in this README.

We will also show screenshots of the actions which are possible in our implementation:
- TODO: When implementation is finalized

## Table of Contents

- [DeToks: Upvote Token](#detoks--upvote-token)
  * [Table of Contents](#table-of-contents)
  * [Project Structure](#project-structure)
  * [Design Choices](#design-choices)
    + [TrustChain](#trustchain)
    + [Tokenomics](#tokenomics)
  * [Known Issues and Limitations](#known-issues-and-limitations)
  * [Future Work](#future-work)

This document will contain the project structure, design choices we made for our upvote token economy,
the underlying protocol used for our upvoting token, and the known issues and limitations.
Finally, we will also discuss the potential future work of the upvote token implementation.


## Project Structure

## Design Choices

### TrustChain
We have implemented the TrustChain blockchain in the DeToks application to store information regarding the upvote token.
A user can post a video by clicking a button on the screen. Whenever a video is posted,
an open-ended proposal block of the type ``give_upvote_token_block`` is created and signed with the user's private key for each video that a user posts,
any viewer who likes a video posted by another user will create an agreement block for that video's open ended proposal block.

We also implemented a trustchain block of the type ``balance_checkpoint``. A block of this type is created once a day, on days
the user goes online. It is a balance checkpoint block to keep track of the total balance of upvotes received and sent.
As users use the Detoks app more, they will upvote and post more videos. These actions add more proposal and agreement blocks
to their trust chain. Calculating the balance of upvotes of each user by crawling their entire trustchain does not bode well for the scalability.
This checkpoint will prevent the network from going back to the user's genesis block to calculate the current balance.

We use the trustchain to find out which video should viral by checking how many times a proposal block of the type ``give_upvote_token_block`` is signed.
We have implemented four different ways to recommend specific content to users:
- The most liked video (this video is found by searching the trustchain for the proposal block which has the most signed agreement blocks linked to it)
- A random video (this video can be selected by randomly choosing a proposal block in the trustchain).
- We have also chosen to randomly choose a proposal block to give users who do not have many upvotes a chance of being recommended and thus going viral.
- On receiving an upvote token from someone, it sends three videos of the receiver back to the sender.
- Requesting from all peers in the network to send five videos they uploaded last.

### Tokenenomics
A user can mint ten upvote tokens daily when using the application.
These tokens can be used when double tapping on a video to send the token to the content creator.
The limit of 10 tokens will be reset each day, but the unminted tokens will not be passed over to the next day,
which is done to prevent token stacking.
We have decided to store the following information in the token: token_id, date, public_key of the minter, and video_id.
The token id is a value between 0 and 10 and will be unique in combination with the date and public key of the minter.
We store the sent and received upvote tokens in a personal sqldelight table to use the information
to find better-recommended content for the sender.

In order to keep the network alive, there must be enough seeders for the leechers to download (torrent) the content.
We decided that each user that uploads content to the application will also seed that content to the network.
Furthermore, when a user double-clicks on a video to send an upvote token, it will start to seed that content to the network.
The implementation is done by creating a magnet URI link from the torrent handle corresponding to the seed.
This URI will then be sent to all the peers currently online in the network.
Once a peer receives this magnet URI, they can download the torrent in the background and watch the content.

To keep the content network alive, we have chosen to let the users to store, seed and disseminate the references to videos which are not
necessarily posted by themselves. To incentivize seeding other user's content,
users are rewarded with minted upvote tokens from other who downloaded pieces of the content from them.
The trustchain is utilized to get content to download and seed: random videos are selected by randomly choosing 5 proposal blocks.
Using the magnet URI link in the transaction map of the proposal blocks, the user will then download and seed the torrent.
Once the content is seeded, the magnet URI link will be greedily sent to all users who are online.
To make sure that the user's bandwidth is not only used for seeding content, we set restrict the number of videos a user can seed to a maximum of 5 videos.


## Known Issues and Limitations

## Future Work
