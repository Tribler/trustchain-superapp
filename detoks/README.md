# DeToks

## Torrenting

The torrenting is handled by the TorrentManager class (located in TorrentManager.kt). A single instance needs to exist for it for the entire duration of the app, as different fragments and classes interact with it. Hence why to get access to it, one needs to use TorrentManager.getInstance() method.

Since the user uploads their own videos, clearing the cache is not recommended (as then the app can no longer seed).
