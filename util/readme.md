# Dummy Nodes

## Setup

Use pip to install the requirements for this script

```bash
pip install -r requirements.txt
```

### pyipv8

Run the following command from within the `util` folder

```bash
git clone https://github.com/Tribler/py-ipv8.git pyipv8
cd pyipv8
pip install -r requirements.txt
```

### Folders

A few folders are required, though the script will create them if not present.

```bash
mkdir keys
mkdir torrents
```

The `keys` folder will be used to store the keys used by each node. The `torrents` folder can be used to store torrents in. From here nodes will randomly pick torrents to gossip to each other.

## Running

Run the following command to start a session with 3 nodes.

```bash
python communitysim.py 3
```

More nodes can be added simply by changing the argument of the script

```bash
python communitysim.py <num_nodes>
```
