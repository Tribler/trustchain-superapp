from dataclasses import dataclass
from pyipv8.ipv8.community import Community


@dataclass
class Gossiper:
    delay: float  # delay between gossip rounds
    peers: int  # number of peers to gossip with
    host_community: Community  # the ipv8 overlay used for messaging

    def serialize_message(self, map: dict):
        pairs = []
        for i in map:
            pairs.append(f"{i}~{map[i]}")            
        return ",".join(pairs)

    def gossip(self):
        raise NotImplementedError("Gossiper should have gossip function")
