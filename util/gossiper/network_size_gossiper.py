from .gossiper import Gossiper


class NetworkSizeGossiper(Gossiper):
    def __init__(self, delay, peers, community):
        self.delay = delay
        self.peers = peers
        self.community = community
        
    def gossip(self):
        pass
