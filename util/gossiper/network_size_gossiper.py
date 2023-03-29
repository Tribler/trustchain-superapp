import random
from dataclasses import dataclass
from base64 import b64encode
from .gossiper import Gossiper
from messages import MESSAGE_NETWORK_SIZE_ID
from pyipv8.ipv8.messaging.serialization import Serializable
from pyipv8.ipv8.lazy_community import lazy_wrapper
from pyipv8.ipv8.messaging.payload_headers import BinMemberAuthenticationPayload
from pyipv8.ipv8.messaging.payload_dataclass import overwrite_dataclass
from pyipv8.ipv8.peer import Peer

from binascii import hexlify


dataclass = overwrite_dataclass(dataclass)


@dataclass(msg_id=MESSAGE_NETWORK_SIZE_ID)
class NetworkSizePayload:
    message: str


class NetworkSizeGossiper(Gossiper):
    def __init__(self, delay, peers, community, num_leaders, signed=True):
        self.delay = delay
        self.peers = peers
        self.community = community
        self.estimated_size = 1
        self.first_cycle = True
        self.signed = signed

        self.leader_estimates = {}
        self.awaiting_responses = []

        self.num_leaders = num_leaders

        self.serializer = community.serializer
        self._verify_signature = community._verify_signature
        self.gossip_count = 0

    def gossip(self) -> None:
        print(f"Gossip {self.gossip_count}")
        self.gossip_count += 1
        if self.first_cycle:
            self.first_cycle = False

        if self.leader_estimates != {}:
            temp = list(self.leader_estimates.values())
            temp.sort(reverse=False)
            self.estimated_size = round(1 / temp[0]) if temp[0] > 0 else 1

            self.estimated_size = max(1, self.estimated_size)

            if self.community.should_print:
                print(f"Estimated Size: {self.estimated_size}")

        self.awaiting_responses = []

        chance_leader = self.num_leaders / self.estimated_size

        if random.random() < chance_leader:
            print("Leader")
            self.leader_estimates = {hexlify(self.community.my_peer.mid).decode(): 1.0}
        else:
            self.leader_estimates = {}

        message = self.serialize_message(self.leader_estimates)

        for peer in self.community.get_peers():
            self.awaiting_responses.append(hexlify(peer.mid).decode())
            packet = self.community.ezr_pack(
                MESSAGE_NETWORK_SIZE_ID, NetworkSizePayload(message), sig=self.signed
            )

            self.community.endpoint.send(peer.address, packet)

    # @lazy_wrapper(NetworkSizePayload)
    def received_response(self, peer, payload: NetworkSizePayload) -> None:
        other_mid = hexlify(peer.mid).decode()

        if not other_mid in self.awaiting_responses:
            message = self.serialize_message(self.leader_estimates)
            packet = self.community.ezr_pack(
                MESSAGE_NETWORK_SIZE_ID, NetworkSizePayload(message), sig=self.signed
            )

            self.community.endpoint.send(peer.address, packet)

        data = self.deserialize_message(payload.message)
        new_leader_estimates = {}

        # add my keys
        for i in self.leader_estimates:
            if i not in data:
                new_leader_estimates[i] = self.leader_estimates[i] / 2
            else:
                new_leader_estimates[i] = (
                    self.leader_estimates[i] + float(data[i])
                ) / 2

        for i in data:
            if i not in new_leader_estimates:
                new_leader_estimates[i] = float(data[i]) / 2

        self.leader_estimates = new_leader_estimates
