from .gossiper import Gossiper


class BootGossiper(Gossiper):

    def __init__(self, delay, peers, community):
        self.max_loops = int(30 / delay)
        self.delay = delay
        self.peers = peers
        self.community = community

    def gossip(self):
        if self.max_loops > 0:
            self.max_loops -= 1
            print(f"Sending bootstrap {self.max_loops} to go")



