import os
import random
import json
from torrentool.api import Torrent
from .gossiper import Gossiper
from messages import TorrentPayload, MESSAGE_TORRENT_ID


class TorrentGossiper(Gossiper):
    def __init__(self, delay, peers, community, signed=True, profiles=[]):
        self.delay = delay
        self.peers = peers
        self.community = community
        self.torrent_list = os.listdir("torrents/")
        self.signed = signed
        self.profiles = profiles

    def get_magnet_hash(self, magnet: str) -> str:
        """
        Extracts the hash from a magnetlink
        """
        temp = magnet.split("xt=urn:btih:")[1]
        temp = temp.split("&")[0]

        return temp

    # sends random torrents to a set of peers
    def gossip(self):
        for p in self.community.get_peers():
            for profile in self.profiles:
                for video in profile["videos"]:
                    data_to_send = json.dumps(
                        [
                            [
                                "Key",
                                f'{self.get_magnet_hash(video["magnet"])}?index={video["index"]}',
                            ],
                            ["WatchTime", str(video["watchTime"])],
                            ["Likes", str(video["likes"])],
                            ["Duration", "0"],
                            ["UploadDate", str(video["uploadDate"])],
                            ["HopCount", str(video["hopCount"])],
                        ]
                    )
                    packet = self.community.ezr_pack(
                        MESSAGE_TORRENT_ID,
                        TorrentPayload(data_to_send),
                        sig=self.signed,
                    )
                    self.community.endpoint.send(p.address, packet)

    def received_response(self, _peer, payload: bytearray, data_offset=31) -> None:
        result = payload[data_offset:].decode()
        # print(f"TORRENT MESSAGE {result}")
