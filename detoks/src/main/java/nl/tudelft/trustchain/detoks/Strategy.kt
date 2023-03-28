package nl.tudelft.trustchain.detoks

import nl.tudelft.trustchain.detoks.TorrentManager.TorrentHandler

class Strategy {

    /**
     * Returns randomly shuffled torrent handlers.
     */
    fun randomStrategy(
        handlers: MutableList<TorrentHandler>,
        profiles: HashMap<String, ProfileEntry>
    ) : MutableList<TorrentHandler> {
        return handlers.shuffled().toMutableList()
    }

    /**
     * Returns the torrent handlers based on decreasing watch time.
     */
    fun watchTimeStrategy(
        handlers: MutableList<TorrentHandler>,
        profiles: HashMap<String, ProfileEntry>
    ) : MutableList<TorrentHandler> {
        return applyStrategy(handlers, profiles) { p0, p1 ->
            return@applyStrategy (p0.second!!.watchTime compareTo p1.second!!.watchTime)
        }
    }

    /**
     * Applies the sorting function sent as parameter, to the handlers, based on the profiles.
     */
    private fun applyStrategy(
        handlers: MutableList<TorrentHandler>,
        profiles: HashMap<String, ProfileEntry>,
        func: (
            Pair<TorrentHandler, ProfileEntry?>,
            Pair<TorrentHandler, ProfileEntry?>
        ) -> Int
    ): MutableList<TorrentHandler> {
        val handlerProfile = handlers.map {
            val key = it.handle.infoHash().toString()
            if (!profiles.contains(key)) Pair(it, ProfileEntry())
            Pair(it, profiles[key])
        }

        val sortedHandlerProfile = handlerProfile.sortedWith(func)
        return sortedHandlerProfile.map { it.first }.toMutableList()

    }
}
