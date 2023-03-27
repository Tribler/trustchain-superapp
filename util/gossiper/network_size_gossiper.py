import random
from .gossiper import Gossiper
from messages import MESSAGE_NETWORK_SIZE_ID
from pyipv8.ipv8.messaging.serialization import Serializable

class NetworkSizePayload(Serializable):
    def __init__(self, message):
        self.message = message

    def to_pack_list(self):
        return [("Q", MESSAGE_NETWORK_SIZE_ID), ("raw", self.message.encode("UTF-8"))]

    @classmethod
    def from_unpack_list(cls, *args):
        return cls(*args)

class NetworkSizeGossiper(Gossiper):
    def __init__(self, delay, peers, community, num_leaders):
        self.delay = delay
        self.peers = peers
        self.community = community
        self.estimated_size = 1
        self.first_cycle = True

        self.leader_estimates = {}
        self.awaiting_responses = []

        self.num_leaders = num_leaders

        
    def gossip(self) -> None:
        if self.first_cycle:
            self.first_cycle = False

        if self.leader_estimates != {}:
            temp = list(self.leader_estimates.values())
            temp.sort()
            self.estimated_size = int( 1 / temp[0])

        self.awaiting_responses = []

        chance_leader = self.num_leaders / self.estimated_size

        if random.random() < chance_leader:
            self.leader_estimates = { str(self.community.my_peer): 1.0 }
        else:
            self.leader_estimates = {}

        message = self.serialize_message(self.leader_estimates)

        for peer in self.community.get_peers():
                packet = self.community.ezr_pack(
                    MESSAGE_NETWORK_SIZE_ID, NetworkSizePayload(message),
                    sig=False
                )

                self.community.endpoint.send(peer.address, packet)

    def received_response(self, peer, payload, data_offset=31) -> None:
        result = payload[data_offset:].decode()
        print(f"NETWORKSIZE RESPONSE {result}")
