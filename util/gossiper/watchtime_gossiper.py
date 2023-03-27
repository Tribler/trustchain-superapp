import os
import random

from torrentool.api import Torrent
from pyipv8.ipv8.messaging.serialization import Serializable

from .gossiper import Gossiper
from messages import MESSAGE_WATCH_TIME_ID


class WatchtimePayload(Serializable):
    def __init__(self, message):
        self.message = message

    def to_pack_list(self):
        return [("Q", MESSAGE_WATCH_TIME_ID), ("raw", self.message.encode("UTF-8"))]

    @classmethod
    def from_unpack_list(cls, *args):
        return cls(*args)

class WatchtimeGossiper(Gossiper):

    def __init__(self, delay, peers, community):
        self.delay = delay
        self.peers = peers
        self.community = community

        self.torrent_list = os.listdir("torrents/")
        self.watch_time_map = {}

        for t in self.torrent_list:
            torrent = Torrent.from_file(f"torrents/{t}")

            watch_time = random.random() * 10
            self.watch_time_map[torrent.magnet_link] = watch_time
        
    def gossip(self):
        
        message = self.serialize_message(self.watch_time_map)

        for peer in self.community.get_peers():
                packet = self.community.ezr_pack(
                    MESSAGE_WATCH_TIME_ID, WatchtimePayload(message),
                    sig=False
                )

                self.community.endpoint.send(peer.address, packet)

    def received_response(self, _peer, payload: bytearray, data_offset=31) -> None: 
        result = payload[data_offset:].decode()
        print(f"WATCHTIME MESSAGE {result}")