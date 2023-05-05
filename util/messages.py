from pyipv8.ipv8.messaging.serialization import Serializable

MESSAGE_TORRENT_ID = 1
MESSAGE_TRANSACTION_ID = 2
MESSAGE_WATCH_TIME_ID = 3
MESSAGE_NETWORK_SIZE_ID = 4
MESSAGE_BOOT_REQUEST = 5
MESSAGE_BOOT_RESPONSE = 6


class TorrentPayload(Serializable):
    def __init__(self, message):
        self.message = message

    def to_pack_list(self):
        return [("Q", MESSAGE_TORRENT_ID), ("raw", self.message.encode("UTF-8"))]

    @classmethod
    def from_unpack_list(cls, *args):
        return cls(*args)
