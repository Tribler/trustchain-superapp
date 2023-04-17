# DeToks

Detoks is a decentralized version of TikTok implemented using [IPv8](https://github.com/Tribler/kotlin-ipv8) and [Trustchain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md).

TODO: add screenshots of each of the screens

## Liking videos
All communication in Detoks is done through a single message type, a "like".
Like message fields:
- Liker
- Video name
- Torrent
- Author
- Timestamp

Each like message is encoded as a trustchain block and shared with the network.
When users receive a like, they retrieve the torrent link from the like message to achieve content discovery.
Furthermore, the user can count the number of likes for each video and use that to recommend new videos based on what is currently trending.
The user can also see how many likes they have received themselves.

## Recommender
The recommender recommends videos based on which ones are the most liked at the moment, on ties, the most recent video is shown.

TODO: add screenshots of the statistics page

## Torrenting
The torrenting is handled by the TorrentManager class (located in TorrentManager.kt). A single instance needs to exist for it for the entire duration of the app, as different fragments and classes interact with it. Hence why to get access to it, one needs to use TorrentManager.getInstance() method.

Since the user uploads their own videos, clearing the cache is not recommended (as then the app can no longer seed).
