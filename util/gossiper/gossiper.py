from dataclasses import dataclass
from pyipv8.ipv8.community import Community


@dataclass
class Gossiper:
    delay: float  # delay between gossip rounds
    peers: int  # number of peers to gossip with
    host_community: Community  # the ipv8 overlay used for messaging

    def serialize_message(self, map: dict) -> str:
        pairs = []
        for i in map:
            pairs.append(f"{i}~{map[i]}")            
        return ",".join(pairs)

    def deserialize_message(self, msg: str) -> dict:
        to_return = {}

        items = msg.split(",")

        for i in items:
            split = i.split("~")
            if len(split) != 2:
                continue
            to_return[split[0]] = split[1]

        return to_return
            

    def gossip(self):
        raise NotImplementedError("Gossiper should have gossip function")
