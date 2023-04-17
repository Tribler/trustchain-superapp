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
  * [Benchmarking](#benchmarking)
  * [Known Issues and Limitations](#known-issues-and-limitations)
  * [Future Work](#future-work)

This document will contain the project structure, design choices we made for our upvote token economy,
the underlying protocol used for our upvoting token, and the known issues and limitations.
Finally, we will also discuss the potential future work of the upvote token implementation.

## Project Structure

The project has been forked from the original DeToks application to avoid compilation issues later when the project is finished. Then all other groups merge their project into the main DeToks application. The project is composed of a few packages and files; the most important ones are:

- ``community`` - This package contains the ``UpvoteCommunity``, which handles the Trustchain part of the protocol and calls relevant recommendations and upvoting code. Furthermore, it also contains the serializer and deserializer of the ``MagnetURIPayload``, ``RecommendedVideoPayload``, and ``UpvoteTokenPayload. ``
- ``token`` - This package contains the ``ProposalToken`` and ``UpvoteToken``. These contain the most important block creation logic for upvoting content (minting tokens) and the proposal block, created every time a peer posts a video. 
- ``trustchain`` - This package contains the logic ``Balance`` to check for the daily balance of each network peer without going through the entire TrustChain blockchain. It also contains a way to check a user's token balance. 
- ``TorrentManager`` - This file contains the logic to manage the torrent files and the video pool. It is responsible for downloading torrent files and caching videos. It also provides the logic for seeding the videos to the network when upvoting. More on this will be in the [Design Choices](#design-choices) section.

## Design Choices

### TrustChain

We have implemented the TrustChain blockchain in the DeToks application to store information regarding the upvote token.
A user can post a video by clicking a button on the screen. Whenever a video is posted,
an open-ended proposal block of the type ``give_upvote_token_block`` is created and signed with the user's private key for each video that the user posts,
any viewer who likes a video posted by another user will create an agreement block for that video's open-ended proposal block.

We also implemented a TrustChain block of the type ``balance_checkpoint``. A block of this type is created once a day when
the user goes online. It is a balance checkpoint block to keep track of the total balance of upvotes received and sent.
Users who use the Detoks app more will upvote and post more videos. These actions add more proposal and agreement blocks
to their TrustChain. Calculating the balance of upvotes of each user by crawling their entire TrustChain does not bode well for scalability.
This checkpoint will prevent the network from returning to the user's genesis block to calculate the current balance.

We use the TrustChain to determine which video should go viral by checking how often a proposal block of the type ``give_upvote_token_block`` is signed.
We have implemented four different ways to recommend specific content to users:

- The most liked video (this video is found by searching the TrustChain for the proposal block, which has the most signed agreement blocks linked to it)
- A random video (this video can be selected by randomly choosing a proposal block in the TrustChain).
- We have also chosen to randomly choose a proposal block to give users who do not have many upvotes a chance of being recommended and thus going viral.
- On receiving an upvote token from someone, it sends three videos of the receiver back to the sender.
- Requesting from all peers in the network to send five videos they uploaded last.

### Tokenenomics

In the application, a few system settings are defined, determining several aspects of tokenomics. These limits are designed to add scarcity of UpvoteTokens in the network. Currently, there are three predefined limits; these are (current value is in brackets):

- The number of tokens sent for each upvote (3)
- The number of videos a user can upvote daily (10)
- The number of tokens a creator has to send as a reward for seeding a video (1)

When using the application, a user can mint a fixed amount of upvote tokens daily. This limit is calculated by multiplying the system settings tokens sent for each upvote and the daily upvote limit.
These tokens can be used when double tapping on a video to send the token to the content creator.
This limit of tokens will be reset each day, but the unminted tokens will not be passed over to the next day, which is done to prevent token stacking.
We have decided to store the following information in the token: token_id, date, public_key of the minter, video_id, and the public_key of the seeder.
The token id is a value between 0 and the limit and will be unique in combination with the date and public key of the minter.
We store the sent and received upvote tokens in a personal sqldelight table to use the information to find better-recommended content for the sender.

In order to keep the network alive, there must be enough seeders for the leechers to download (torrent) the content.
We decided that each user that uploads content to the application will also seed that content to the network.
Furthermore, when a user double-clicks on a video to send an upvote token, it will start to seed that content to the network.
The implementation is done by creating a magnet URI link from the torrent handle corresponding to the seed.
This URI will then be sent to all the peers currently online in the network.
Once a peer receives this magnet URI, they can download the torrent in the background and watch the content.

To keep the content network alive, we have let the users store, seed, and disseminate the references to videos not
necessarily posted by themselves. To incentivize seeding other users' content, users are rewarded with minted upvote tokens from others who downloaded pieces of the content from them.
The TrustChain is utilized to get content to download and seed: random videos are selected by randomly choosing five proposal blocks.
The user will download and seed the torrent using the magnet URI link in the transaction map of the proposal blocks.
Once the content is seeded, all online users will be greedily sent the magnet URI link.
To ensure that the user's bandwidth is not only used for seeding content, we also set to restrict the number of videos a user can seed to a maximum of 5.

We have designed a reward system to incentivize peers to seed other creators' videos. Whenever a peer upvotes a video, it sends the creator 5 (adjustable as per system setting) UpvoteTokens. In those tokens, the public key of the seeder delivered the most data for the seed of the video. The creator has to send a subset of the received UpvoteTokens to that seeder. Alongside the token, the creator also sends a hash of a TrustChain block of type `RewardSeeder.` After receiving the tokens, the seeder can sign the block to complete the reward. Integrating a TrustChain block in the transaction makes all rewards public for all peers. This way, seeders can check whether the creators they want to seed for are trustworthy and reward the peers that seed, or if they are not and should therefore be excluded from the network. 

## Benchmarking

We have created a setup to run and save benchmarks from benchmarking our module. Since we must benchmark for multiple devices simultaneously, we created a database in which all benchmark results were saved. We will start with an explanation of the benchmarks we did and their results and conclude with recommendations for future benchmarks to test the performance of the entire DeToks app.

### Recommendations Benchmark

The recommendation system we implemented can be benchmarked by measuring how long it takes to return a new recommendation after calling the recommendation function. The recommender will fetch new recommendations whenever its current list of recommendations is empty. For the benchmarking setup, we used the following:

- 15 devices running the superapp with all modules except DeToks disabled
  
  - 13 Emulators running Android API 33
  
  - 2 Android phones

- Each phone called the `getNextRecommendation` function 100 times in one sequential loop

- All results stored in a Firestore database

In Figure (**FIGURE_REF!!!**), the time per function call is plotted with the 100 calls on the x-axis and the time it took in ms on the y-axis. Each line represents one device with its corresponding public key shown in the legend in the top right corner.

Looking at the graph, there are some significant peaks for the response time of one public key. Since we believe this might be due to a bad internet connection, we have also plotted the same results without the data from the outlier public key in Figure (**FIGURE_REF!!!**).

<img src="../doc/detoks-uvt/timings_per_public_key-v2.jpg" width="700" height="400" />


Below, the same graph as before has been plotted, but now the blue outlier has been left out to give a better overview of the response time of the recommendation system.

<img src="../doc/detoks-uvt/timings_per_public_key_without_outlier-v2.jpg" width="700" height="400" />

With the outlier removed, we can see that most of the response times lie in the 0-20 ms range, with some public keys experiencing slower response times, sometimes upwards of 100 ms. What must be noted, however, is that these devices did run on different network configurations. Therefore the speed of the internet can play a significant role in the experienced response times. To illustrate this further, please refer to Figure (**FIGURE_REF!!!**), where we plotted the average total time spent by any device waiting for the 100 recommendation function calls to finish. Thus if a device $X$ did $3$ benchmark runs, where each benchmark run consists of $100$ recommendation function calls and the total time of those $3$ runs is 12s, then we plot the average total time as 4s. 

![](../doc/detoks-uvt/bar_chart_per_publickey.png)

The average total times have been sorted in ascending order to show the variance in response times per public key.

## Known Issues and Limitations

## Future Work

At the current state of the applications, several next steps are needed to extend to current functionality. However, the improvements listed below fall out of scope for the course and are thus not implemented (yet). We also provide some future work for benchmarking the DeToks module. 

- There is no way to spend the UpvoteTokens a user has received. To add more value to the tokens besides social status and incentivize peers more to create videos, it would be nice to have an in-app store where the UpvoteTokens can be traded for other collectible items, such as custom filters (potentially made by other peers), or other tokens that are designed to be used in the app.
- Currently, the reward system for seeders uses a fixed (system setting) amount of UpvoteTokens as a reward. However, it would be interesting if content creators could set their token reward amount for each video so that seeders can trade off about which videos they want to seed. A video with a high reward might attract many seeders, lowering the chance of a single seeder seeding the most bytes. For the content creator, offering more UpvoteTokens for seeding their video might increase the number of peers that want to seed the content creator's video, increasing the chance that the video might go viral. This concept can be extended more by creating a reward pool rather than just rewarding a single seeder. Creating a reward pool will reward more seeders instead of just one seeder, making seeding more attractive.
- When benchmarking our module, we set up an AWS Device Farm. This tool could allow us to test our application with many concurrent devices. However, this service only offered 1000 device minutes of free usage, and afterward, each device minute used would cost a specific price, or a steep subscription fee had to be paid. Device minutes are measured by installing the .apk, performing the tests, and wiping the device for subsequent usage. These minutes will go down swiftly when used with many devices. We saw the possibility of picking numerous automated tests, such as fuzzing and crawling; however, the most crucial aspect which caught our attention was the Espresso testing. Android Studio can record an Espresso Test script where it is possible to perform specific steps, such as clicking buttons or swiping through fragments. These executions are recorded and then stored in a file. This file and the .apk can be uploaded into AWS Device Farm to simulate the executions performed in the application on many devices concurrently. We could then provide methods according to button clicks, such as upvoting videos or recommending content, which would then be saved into Firebase for post-data analysis. Unfortunately, the entire TrustChain-superapp is a Compose project, which is not supported to record Espresso tests. Luckily, it is possible to manually create an Espresso test script with the commands that need to be executed; however, this would likely be a lengthy and trial-and-error process because the entire superapp consists of many modules that interact. Therefore, we have chosen to add this benchmarking for future work. 
