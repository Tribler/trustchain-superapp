from .gossiper import Gossiper


class NetworkSizeGossiper(Gossiper):
    def __init__(self, delay, peers, community):
        self.delay = delay
        self.peers = peers
        self.community = community
        self.estimated_size = 0
        
    def gossip(self) -> None:
        pass

    def received_response(self, peer, payload) -> None:
        print(f"NETWORKSIZE RESPONSE {payload}")
