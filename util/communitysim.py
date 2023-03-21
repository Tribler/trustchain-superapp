import os
import sys
from asyncio import ensure_future, get_event_loop
from pyipv8.ipv8.configuration import (
    ConfigBuilder,
    Strategy,
    WalkerDefinition,
    default_bootstrap_defs,
    BootstrapperDefinition,
    Bootstrapper,
)
from pyipv8.ipv8_service import IPv8

from messages import TorrentPayload
from detoks_community import DetoksCommunity

bootstrap_config = {
    "class": "DispersyBootstrapper",
    "init": {
        "ip_addresses": [
            ("130.161.119.206", 6421),
            ("130.161.119.206", 6422),
            ("131.180.27.155", 6423),
            ("131.180.27.156", 6424),
            ("131.180.27.161", 6427),
            ("131.180.27.161", 6521),
            ("131.180.27.161", 6522),
            ("131.180.27.162", 6523),
            ("131.180.27.162", 6524),
            ("130.161.119.215", 6525),
            ("130.161.119.215", 6526),
            ("130.161.119.201", 6527),
            ("130.161.119.201", 6528),
            ("131.180.27.188", 1337),
            ("131.180.27.187", 1337),
            ("131.180.27.161", 6427),
        ],
        "dns_addresses": [
            ("dispersy1.tribler.org", 6421),
            ("dispersy1.st.tudelft.nl", 6421),
            ("dispersy2.tribler.org", 6422),
            ("dispersy2.st.tudelft.nl", 6422),
            ("dispersy3.tribler.org", 6423),
            ("dispersy3.st.tudelft.nl", 6423),
            ("dispersy4.tribler.org", 6424),
            ("tracker1.ip-v8.org", 6521),
            ("tracker2.ip-v8.org", 6522),
            ("tracker3.ip-v8.org", 6523),
            ("tracker4.ip-v8.org", 6524),
            ("tracker5.ip-v8.org", 6525),
            ("tracker6.ip-v8.org", 6526),
            ("tracker7.ip-v8.org", 6527),
            ("tracker8.ip-v8.org", 6528),
        ],
        "bootstrap_timeout": 0.5,
    },
}


bootstrapper = [
    BootstrapperDefinition(Bootstrapper.DispersyBootstrapper, bootstrap_config["init"])
]


async def start_nodes(num_nodes: int, timeout: int, max_peers: int):
    print(
        f"Starting community with {num_nodes} nodes, {timeout}s timeout and {max_peers} max peers."
    )

    if not os.path.exists("keys/"):
        os.mkdir("keys/")

    if not os.path.exists("torrents/"):
        os.mkdir("torrents/")

    for i in range(num_nodes):
        node_name = f"dummy_peer_{i}"
        builder = ConfigBuilder()
        builder.add_key(node_name, "medium", f"keys/ec{i}.pem")
        builder.add_overlay(
            "DeToksCommunity",
            node_name,
            [WalkerDefinition(Strategy.RandomWalk, max_peers, {"timeout": timeout})],
            bootstrapper,
            {},
            [("started",)],
        )
        ipv8 = IPv8(
            builder.finalize(), extra_communities={"DeToksCommunity": DetoksCommunity}
        )
        await ipv8.start()


if __name__ == "__main__":
    NUM_NODES = int(os.getenv("NUM_NODES"))
    TIMEOUT = int(os.getenv("TIMEOUT"))
    MAX_NUM_PEERS = int(os.getenv("MAX_NUM_PEERS"))

    ensure_future(start_nodes(NUM_NODES, TIMEOUT, MAX_NUM_PEERS))
    get_event_loop().run_forever()
