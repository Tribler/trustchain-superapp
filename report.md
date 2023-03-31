# DeToks: Upvote Token

DeToks is a decentralized TikTok skeleton application where several different groups implemented parts of the application. The application will utilize a peer-to-peer network consisting strictly of smartphones only. Currently, the application only works on Android-powered smartphones. The focus of this application will be the token economy, Torrent-based streaming videos, and basic app logic. Our responsibility for the project was the Upvote Token, and we will explain the components of this functionality in this README. 

We will also show screenshots of the actions which are possible in our implementation:
- TODO: When implementation is finalized

- [DeToks: Upvote Token](#detoks--upvote-token)
  * [Project Structure](#project-structure)
  * [Design Choices](#design-choices)
    + [TrustChain](#trustchain)
    + [Token design](#token-design)
  * [Known Issues and Limitations](#known-issues-and-limitations)
  * [Future Work](#future-work)

This document will contain the project structure, design choices we made for our upvote token economy, the underlying protocol used for our upvoting token, and the known issues and limitations. Finally, we will also discuss the potential future work of the upvote token implementation. 


## Project Structure

## Design Choices

### TrustChain
We have implemented the TrustChain blockchain in the DeToks application to store information regarding the upvote token. We created an open-ended proposal block for each video such that every viewer can approve and complete the block when the user upvotes a video. A user can create such a proposal block when clicking a button on the screen, which can then be signed. We also implemented a daily balance checkpoint block to keep track of the total balances. This checkpoint will prevent the network from tracking back to the first transaction block to find the current balance. We can use this to indicate which video to go viral by checking how many times a proposal block is signed. We have implemented four different ways to recommend specific content to users:
- The most liked video (this can be recommended using the most signed block in the blockchain). 
- A random video (this can be randomly chosen from a proposal block in the blockchain). We have also chosen to randomly choose a proposal block to give users who do not have many upvotes a chance of being recommended and thus going viral.
- On receiving an upvote token from someone, it sends three videos of the receiver back to the sender.
- Requesting from all peers in the network to send five videos they uploaded last. 

### Token design
A user can mint ten upvote tokens daily when using the application. These tokens can be used when double pressing on a video to send the token to the content creator. The limit of 10 tokens will be reset each day, but the unminted tokens will not be passed over to the next day, which is done to prevent token stacking. We have decided to store the following information in the token: token_id, date, public_key of the minter, and video_id. The token id is a value between 0 and 10 and will be unique in combination with the date and public key of the minter. We store the sent and received upvote tokens in a personal sqldelight table to use the information to find better-recommended content for the sender. 

In order to keep the network alive, there must be enough seeders for the leechers to download (torrent) the content. We decided that each user that uploads content to the application will also seed that content to the network. Furthermore, when a user double-clicks on a video to send an upvote token, it will start to seed that content to the network. The implementation is done by creating a magnet URI link from the torrent handle corresponding to the seed. This URI will then be sent to all the peers currently online in the network. Once a peer receives this magnet URI, they can download the torrent in the background and watch the content. 

## Known Issues and Limitations

## Future Work
