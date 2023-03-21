import os
import random
from pyipv8.ipv8.community import Community
from gossiper.torrent_gossiper import TorrentGossiper


class DetoksCommunity(Community):
    community_id = bytes.fromhex(os.getenv("COMMUNITY_ID"))

    def started(self):
        gossipers = [TorrentGossiper(delay=5.0, peers=5, community=self)]

        for i, val in enumerate(gossipers):
            self.register_task(f"gossip_{i}", val.gossip, interval=val.delay, delay=0)

        async def print_peers():
            print(
                "I am:", self.my_peer, "\nI know:", [str(p) for p in self.get_peers()]
            )

        self.register_task("print_peers", print_peers, interval=5.0, delay=0)

        # self.add_message_handler(1, self.on_message)

    def on_message(self, peer, payload):
        print("Got a message from:", peer)
        print("The message includes the first payload:\n", payload)
