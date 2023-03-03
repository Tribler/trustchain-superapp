import os
from asyncio import ensure_future, get_event_loop

from pyipv8.ipv8.community import Community
from pyipv8.ipv8.configuration import (
    ConfigBuilder,
    Strategy,
    WalkerDefinition,
    default_bootstrap_defs,
    BootstrapperDefinition,
    Bootstrapper,
)

# from pyipv8.ipv8.bootstrapping.dispersy.bootstrapper import DispersyBootstrapper
from pyipv8.ipv8_service import IPv8


class MyCommunity(Community):
    community_id = bytes.fromhex("c86a7db45eb3563ae047639817baec4db2bc7c25")

    def started(self):
        async def print_peers():
            print(
                "I am:", self.my_peer, "\nI know:", [str(p) for p in self.get_peers()]
            )

        # We register a asyncio task with this overlay.
        # This makes sure that the task ends when this overlay is unloaded.
        # We call the 'print_peers' function every 5.0 seconds, starting now.
        self.register_task("print_peers", print_peers, interval=5.0, delay=0)


async def start_communities():
    for i in [2]:
        builder = ConfigBuilder().clear_keys().clear_overlays()
        # If we actually want to communicate between two different peers
        # we need to assign them different keys.
        # We will generate an EC key called 'my peer' which has 'medium'
        # security and will be stored in file 'ecI.pem' where 'I' is replaced
        # by the peer number (1 or 2).
        builder.add_key("my peer", "medium", f"ec{i}.pem")
        # Instruct IPv8 to load our custom overlay, registered in _COMMUNITIES.
        # We use the 'my peer' key, which we registered before.
        # We will attempt to find other peers in this overlay using the
        # RandomWalk strategy, until we find 10 peers.
        # We do not provide additional startup arguments or a function to run
        # once the overlay has been initialized.
        builder.add_overlay(
            "MyCommunity",
            "my peer",
            [WalkerDefinition(Strategy.RandomWalk, 3, {"timeout": 1.0})],
            default_bootstrap_defs,
            {},
            [("started",)],
        )
        ipv8 = IPv8(builder.finalize(), extra_communities={"MyCommunity": MyCommunity})
        await ipv8.start()


ensure_future(start_communities())
get_event_loop().run_forever()
