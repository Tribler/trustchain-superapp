import os
import random
from binascii import hexlify
from pyipv8.ipv8.community import Community
from gossiper.torrent_gossiper import TorrentGossiper
from gossiper.network_size_gossiper import NetworkSizeGossiper, NetworkSizePayload
from gossiper.boot_gossiper import BootGossiper
from gossiper.watchtime_gossiper import WatchtimeGossiper

from pyipv8.ipv8.lazy_community import lazy_wrapper

import messages


class DetoksCommunity(Community):
    community_id = bytes.fromhex(os.getenv("COMMUNITY_ID"))
    should_print = False

    def started(self, node): 
        self.should_print = False

        self.logger.disabled = True
        self.torrent_gossiper = TorrentGossiper(
            delay=5.0, peers=5, community=self, signed=False, profiles=node["profiles"]
        )
        self.register_task(
            f"torrent_gossip",
            self.torrent_gossiper.gossip,
            interval=self.torrent_gossiper.delay,
            delay=0,
        )

        async def print_peers():
            print(
                "I am:", self.my_peer, "\nI know:", [str(p) for p in self.get_peers()]
            )

        if self.should_print:
            self.register_task("print_peers", print_peers, interval=5.0, delay=0)

        self.add_message_handler(
            messages.MESSAGE_TORRENT_ID, self.torrent_gossiper.received_response
        )
