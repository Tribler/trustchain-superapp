import os
import random
from pyipv8.ipv8.community import Community
from gossiper.torrent_gossiper import TorrentGossiper
from gossiper.network_size_gossiper import NetworkSizeGossiper
from gossiper.boot_gossiper import BootGossiper
from gossiper.watchtime_gossiper import WatchtimeGossiper

import messages


class DetoksCommunity(Community):
    community_id = bytes.fromhex(os.getenv("COMMUNITY_ID"))

    def started(self):

        torrent_gossiper = TorrentGossiper(delay=5.0, peers=5, community=self)
        self.register_task(f"torrent_gossip", torrent_gossiper.gossip, interval=torrent_gossiper.delay, delay=0)

        network_size_gossiper = NetworkSizeGossiper(delay=5.0, peers=5, community=self)
        self.register_task(f"networksize_gossip", network_size_gossiper.gossip, interval=network_size_gossiper.delay, delay=0)

        boot_gossiper = BootGossiper(delay=5.0, peers=5, community=self, network_size_gossiper=network_size_gossiper)
        self.register_task(f"boot_gossip", boot_gossiper.gossip, interval=boot_gossiper.delay, delay=0)

        watch_time_gossiper = WatchtimeGossiper(delay=5.0, peers=5, community=self)
        self.register_task(f"wathtime_gossip", watch_time_gossiper.gossip, interval=watch_time_gossiper.delay, delay=0)

        async def print_peers():
            print(
                "I am:", self.my_peer, "\nI know:", [str(p) for p in self.get_peers()]
            )

        self.register_task("print_peers", print_peers, interval=5.0, delay=0)

        self.add_message_handler(messages.MESSAGE_TORRENT_ID, self.on_message)
        self.add_message_handler(messages.MESSAGE_BOOT_REQUEST, boot_gossiper.received_request)
        self.add_message_handler(messages.MESSAGE_BOOT_RESPONSE, boot_gossiper.received_response)
        self.add_message_handler(messages.MESSAGE_NETWORK_SIZE_ID, network_size_gossiper.received_response)


    def on_message(self, peer, payload):
        print(f"TORRENT MESSAGE {payload}")
