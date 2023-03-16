# Dummy Nodes

## Setup

The community simulator uses docker to run the nodes. Make sure you have docker installed on your system, then run the following command to build the community sim container.

```bash
docker-compose up --build
```

After docker is done building it will automatically run the script for you. From now on you can simply run

```bash
docker-compose up
```

or if you do not need the output of the script, you can run it deamonized with

```bash
docker-compose up -d
```

## Configuration

### Parameters

There are a few parameters that can be tweaked for the community sim. In order to do this, you need to create a file called `.env` in the current directory. This file should then contain the parameters you want to change. Below is an example of what a `.env` could look like:

```bash
NUM_NODES=4
TIMEOUT=20
MAX_NUM_PEERS=10
COMMUNITY_ID=c86a7db45eb3563ae047639817baec4db2bc7c25
```

This example spawns 4 nodes with a timeout for discovery of 20 seconds. Each will try and look for 10 peers in the community given by `COMMUNITY_ID`.

### Torrents

Torrents can simply be added to file `torrents` folder. The nodes will use this folder to randomly pick torrents that they will send to other nodes.
