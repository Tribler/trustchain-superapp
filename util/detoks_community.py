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

    def started(self):
        print(hexlify(self.my_peer.mid).decode())
        if (
            "8f3948f78d815a2c779de3dbf13ae7d76d0b3d0b"
            == hexlify(self.my_peer.mid).decode()
        ):
            self.should_print = True
            print(f"Will be printing for node {hexlify(self.my_peer.mid).decode()}")
        else:
            self.should_print = False

        self.logger.disabled = True
        extra_delay = random.random() * 2
        self.torrent_gossiper = TorrentGossiper(
            delay=5.0, peers=5, community=self, signed=False
        )
        self.register_task(
            f"torrent_gossip",
            self.torrent_gossiper.gossip,
            interval=self.torrent_gossiper.delay,
            delay=0,
        )

        self.network_size_gossiper = NetworkSizeGossiper(
            delay=15.0, peers=5, community=self, num_leaders=1, signed=True
        )

        self.register_task(
            f"networksize_gossip",
            self.network_size_gossiper.gossip,
            interval=self.network_size_gossiper.delay,
            delay=0,
        )

        self.boot_gossiper = BootGossiper(
            delay=5.0,
            peers=5,
            community=self,
            network_gossiper=self.network_size_gossiper,
            signed=False,
        )
        self.register_task(
            f"boot_gossip",
            self.boot_gossiper.gossip,
            interval=self.boot_gossiper.delay,
            delay=0,
        )

        self.watch_time_gossiper = WatchtimeGossiper(
            delay=5.0, peers=5, community=self, signed=False
        )
        self.register_task(
            f"wathtime_gossip",
            self.watch_time_gossiper.gossip,
            interval=self.watch_time_gossiper.delay,
            delay=0,
        )

        async def print_peers():
            print(
                "I am:", self.my_peer, "\nI know:", [str(p) for p in self.get_peers()]
            )

        # self.register_task("print_peers", print_peers, interval=5.0, delay=0)

        self.add_message_handler(
            messages.MESSAGE_TORRENT_ID, self.torrent_gossiper.received_response
        )
        self.add_message_handler(
            messages.MESSAGE_BOOT_REQUEST, self.boot_gossiper.received_request
        )
        self.add_message_handler(
            messages.MESSAGE_BOOT_RESPONSE, self.boot_gossiper.received_response
        )
        self.add_message_handler(messages.MESSAGE_NETWORK_SIZE_ID, self.on_message)
        self.add_message_handler(
            messages.MESSAGE_WATCH_TIME_ID, self.watch_time_gossiper.received_response
        )

    @lazy_wrapper(NetworkSizePayload)
    def on_message(self, peer, payload):
        self.network_size_gossiper.received_response(peer, payload)
