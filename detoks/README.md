# DeToks

## Like Token

The like token has the following format:

| Field     |      Description   |
|-----------|:----------|
| liker     | The public key of the person who liked the video |
| video     | The name of the video liked (since a torrent can have multiple files) |
| torrent   | The name of the torrent (its hash info) |
| author    | The public key of the creator of the video  |
| torrentMagnet | A magnet link for the torrent video (since we can have different magnet links for the same torrent) |
| timestamp   | a simple timestamp indicating the time of the like | 



## Torrenting

The torrenting is handled by the TorrentManager class (located in TorrentManager.kt). A single instance needs to exist for it for the entire duration of the app, as different fragments and classes interact with it. Hence why to get access to it, one needs to use TorrentManager.getInstance() method. Example of getting an instance of the current TorrentManager:

```kotlin
torrentManager = TorrentManager.getInstance(requireActivity().applicationContext)
```


Since the user uploads their own videos, clearing the cache is not recommended (as then the app can no longer seed). Thus, currently, all downloaded videos are kept in the cache.

To create a new torrent, one simply needs to call:

```kotlin
// uri is the android media uri of the file to be added to the torrent. This can be received from a call to ActivityResultContracts.GetContent()
torrentManager.createTorrentInfo(uri, context)
```

When a new torrent is created it will automatically be "downloaded", which will result in the device seeding the new video. A new like is broadcasted, since new video announcements are equivalent to sending out the first like. Thus the user will automatically like their own video. The torrent name is set to the name of the video included in it. The author/creator of it is the public key of the node.

Currently, we rely on trackers for the distribution of torrent information (since nodes can be arbitrally on or off). Hence, a custom tracker is also provided. We recommend for redundancy to also use some other public tracker, as this can result in better download speed. As a second tracker we currently use http://opensharing.org:2710/announce

To download a new torrent with a specified magnet link, simply call:
```kotlin

torrentManager.addMagnet(magnet)
```

It will automatically download the video (and start seeding). Torrent files are created in the torrent folder in cache and the contents of the torrents are saved to the media folder in the cache.
