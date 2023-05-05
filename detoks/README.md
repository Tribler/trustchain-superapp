# DeToks

This document describes the functionallity provided in DeToks. Currently it contains the following functionality:
* Watch videos that are shared via the DeToks Community.
* Select a strategy with which to determine which video is played next.
* Option to seed videos for token profit based on a selected seeding strategy, maximum storage size and maximum upload bandwidth.
* View torrent information and statistics of seeded torrents.

![core-functionality](https://user-images.githubusercontent.com/31625452/236310196-38465bbc-0471-4c91-9da3-ed5eda8492a2.gif)

Click [here](https://twinkle.aaw.ooo/y73u8h7m55wap2tgy9rt.gif) for a higher definition GIF.

## Gossiping

A Large part of the DeToks back-end depends on information being gossiped over the DeToks community network. Gossiping works by selecting a random subset of all known peers of a node and passing messages between them. Since different kinds of data is shared, several gossipers are used. Namely:
* [BootGossiper](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/BootGossiper.kt): gossips boot related data.
* [NetworkSizeGossiper](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/NetworkSizeGossiper.kt): estimates network size.
* [TorrentGossiper](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/TorrentGossiper.kt): gossips torrents and corresponding profile metrics.

All of these extend `Gossiper`. This section gives short descriptions of each of them.

### BootGossiper

The [BootGossiper Class](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/BootGossiper.kt) is used to bootstrap the session when starting up the app. The app mainly uses this to bootstrap the networksize value, which then is further kept up to date by the [NetworkSizeGossiper](#NetworkSizeGossiper).

The network size specifically is requested on boot because it plays an important role in determining the size of the other metrics. For example, the total watch time and total likes on a video are determined using this value. 

### NetworkSizeGossiper

The [NetworkSizeGossiper Class](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/NetworkSizeGossiper.kt) is used to estimate the current total size of the DeToks community network. This is done by utilizing the count algorithm as described in [Gossip-Based Aggregation in Large Dynamic Networks](https://dl-acm-org.tudelft.idm.oclc.org/doi/pdf/10.1145/1082469.1082470).

### TorrentGossiper

The [TorrentGossiper Class](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/TorrentGossiper.kt) is used to spread torrent files throughout the network. A random subset of torrents is selected and then sent to a random subset of peers. This happens every few seconds to ensure a good spread of torrents throughout the network.

Next to the torrent, this class also distributes profile statistics throughout the network. Some of these are incorporated in one of the leeching and seeding strategies. This is used to update peers on the profile data it has for a specific video. Furthermore, some of these statistics are also used for the for debugging.

Together with the key and associated magnet link of the profile the shared metrics are:
* `hopCount`: incremented on sending, this statistic details how many hops a torrent was shared from its origin. 
* `watchTime`: when received, update the average watch time of torrent on the network.
* `duration`: when received, set duration of torrent if it was unknown.
* `uploadDate`: when received, set upload date of torrent if it was unknown.
* `likes`: when received, update the estimated likes of torrent on the network.

## Profiles

The [Profile](./src/main/java/nl/tudelft/trustchain/detoks/Profile.kt) class is used to store a certain set of metrics in a profile entry for each torrent. Useful metadata is stored in these entries that help the other parts of DeToks with choosing which torrents to seed, leech and recommend to the user. Each node within the network has their own aggregate of profile entries, their profile. This section describes stored parameters in the profile entries. Entries are added to the profile when either a new torrent is received, or if an entry is shared through gossiping. Almost all values are initialised to zero. The exception is the *watched* field, which is initialised to *false*. The class also contains some auxilliary functions for updating parameters.

* `watched`: set to true if a video of the torrent was watched.
* `watchTime`: average watch time of torrent on the network.
* `duration`: the duration of the first watched video in the torrent.
* `uploadDate`: the creation date of the torrent.
* `hopCount`: the amount of hops the torrent made over the network.
* `timesSeen`: the amount of times this node has received the same torrent.
* `likes`: the amount of likes, this is currently a stub and depends on the implementation of another team.

## Strategies 

The user can specify a leeching and optionally also a seeding strategy. The user can pick the same strategies for leeching and seeding, as both need the torrents to be sorted based on profile metrics. However, the leeching strategy determines which videos are played for the user in the video screen, while the seeding strategy determines in which way the torrents are sorted to be considered for seeding. 

### Defined Strategies
The available strategies are defined in [Strategy.kt](./src/main/java/nl/tudelft/trustchain/detoks/Strategy.kt), and include:
* `STRATEGY_RANDOM`: The random strategy simply chooses torrents to seed at random.
* `STRATEGY_HIGHEST_WATCH_TIME`: This strategy sorts the torrents by watchtime in descending order.
* `STRATEGY_LOWEST_WATCH_TIME`: This strategy sorts the torrents by watchtime in ascending order.
* `STRATEGY_HOT`: This strategy splits the torrents into two lists based on a time cut-off, and sorts both lists by watchtime in descending order. These lists are then merged again, where the list under the cut-off is added in front.
* `STRATEGY_RISING`: This strategy is the same as `HOT_STRATEGY`, but the time cut-off is lower.
* `STRATEGY_NEW`: This strategy sorts the torrents by upload date from newest to oldest.
* `STRATEGY_TOP`: This strategy sorts torrents by number of likes in descending order.
* `STRATEGY_HOPCOUNT`: This strategy sorts the torrents by hopcount in descending order.

### Leeching 

As previously mentioned, the leeching strategy determines which videos are played for the user. This happens by applying the strategy to the specific unwatched part of the available torrents. To do so, an index is kept track of that specifies until where the user has watched. When a new torrent is received, this is inserted into the right place in that part of the list as well. If the user has watched all of its known torrents, the entire list will be sorted instead. 

### Seeding

A user can seed torrents to earn tokens on the DeToks community network. This option can be turned on in the Settings view:

<img src="https://user-images.githubusercontent.com/57201085/236303160-d3001371-45de-4356-8a94-710b52e5469f.jpg" alt="seeding" width="25%">

The user has to specify:
* Maximum bandwidth per day (in MB): sets the maximum upload rate.
* Maximum storage used for seeding (in MB): sets the maximum accumulated size of all seeded torrents combined after download.
* Seeding strategy: the default is `STRATEGY_RANDOM` but other strategies can be selected. Used to sort the torrents for consideration of seeding.

The seeded torrents will then be shown to the user in the overview below. For a seeded torrent the name, total amount downloaded in a session (in MB), total amount uploaded in a session (in MB), and balance of earned tokens for seeding that torrent in this session is shown.

The seeding strategy only sorts the torrents into a list of torrents that will be considered for seeding. In order for a video to be seeded it needs to have more leechers than seeders, and then it will only be selected if it doesn't push the size of the selected torrents over the maximum storage threshold. When it is selected it will attempt to download the torrent with a timeout. If the download is successful, the `SHARE_MODE` flag of the torrent will be turned on, completing the process.

## Tokens

A simple token transaction implementation was added in order to test if transactions can be performed while seeding and downloading. Whenever an user finishes downloading a piece of the torrent they are downloading, they send a token to the peers which were seeding it. We check for that by using the alert `AlertType.PIECE_FINISHED`, and we identify the peers that were seeding the downloaded piece based on the information given by the current torrent handle. In addition, peers may also seed torrents to peers outside of the DeToks Community, by which they also get compsated by increasing their wallet balance.

## Debug Screens

### Torrent-Specific Debug Screen
A user may access the debug screen for a specific torrent by clicking on the name of the video in the main DeToks screen or by clicking on its name in the list of seeded torrents shown in [Seeding](#seeding).   
<img src="https://user-images.githubusercontent.com/57201085/236295625-a4113632-3393-46cc-994d-ffe079ba04cf.jpg" width="25%">

It displays libtorrent metadata on the torrent such as:
* `infoHash`: info hash of the torrent.
* `magnetLink`: magnet link of the torrent.
* `files`: list of files that are contained in the torrent.
* `downloadedBytes`: total number of bytes that have been downloaded from the torrent since it started.

It also displays torrent profile metrics as mentioned in [TorrentGossiper](#torrentgossiper):
* `watchTime`
* `hopCount` 
* `duration`
* `timesSeen`
* `uploadDate`   

and two extra profile metrics:

* `watched`: whether or not the video was watched
* `likes`: number of likes that the torrent has

### Global Debug Screen
A  user may access the general debug screen by clicking on the "Debug" button.

<img src="https://user-images.githubusercontent.com/57201085/236295401-33e4fa3b-4875-4221-b06b-fe57c58df32f.jpg" alt="general debug screen" width="25%">

It displays information on  such as:
* `Estimated networksize`: estimated number of peers in the network.
* `Number of connected peers`: number of connected peers.
* `List of connected peer`: peer IDs (mid) of the connected peers.
* `Top 3 leeching torrents`: torrent names of the top 3 leeching torrents with a description of the leeching strategy. 
* `Top 3 seeding torrents with metrics from seeders`: torrent names of the top 3 seeding torrents with a description of the seeding strategy.
* `Seeding status`: whether or not the peer is seeding.
* `Wallet token amount`: amount of tokens in a peer's wallet.
* `Peer ID`: mid of my peer.

### Seeding Debug Screens
We also created a screen with more details on seeding torrents accessible by clicking the "Torrent Infos" button in Settings.
<img src="https://user-images.githubusercontent.com/57201085/236297098-87829223-0a22-4c79-862c-9e729ec7d3a7.jpg" alt="seeding debug screen" width="25%">

