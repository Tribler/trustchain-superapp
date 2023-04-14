# DeToks

This document describes the functionallity of DeToks.

## Gossiping

Several gossipers are used to share useful information between peers in the DeToks community. This section gives short descriptions of each of them.

Gossiping works by selecting a random subset of all known peers of a node and passing messages between them. New information then slowly traverses the community.

### BootGossiper

The [BootGossiper Class](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/BootGossiper.kt) is used to bootstrap the session when starting up the app. The app mainly uses this to bootstrap the networksize value, which then is further kept up to date by the [NetworkSizeGossiper](#NetworkSizeGossiper)

### NetworkSizeGossiper

The [NetworkSizeGossiper Class](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/NetworkSizeGossiper.kt) is used to estimate the current total size of the DeToks Community network. This is done by utilizing the counting algorithm as described in [Gossip-Based Aggregation in Large Dynamic Networks](https://dl-acm-org.tudelft.idm.oclc.org/doi/pdf/10.1145/1082469.1082470)

### TorrentGossiper

The [TorrentGossiper Class](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/TorrentGossiper.kt) is used to spread torrent files throughout the network. A random torrent is selected and then send to a random subset of peers. This happens every few seconds to ensure a good spread of torrents throughout the network.

### WatchTimeGossiper

The [WatchTimeGossiper Class](./src/main/java/nl/tudelft/trustchain/detoks/gossiper/WatchTimeGossiper.kt) is used to spread profile metrics through the network. This is used to update peers on the current profiles of each other which can then be used to update their seeding and leeching strategies.

## Torrent Profiles

In order to be able to work with recommendations, DeToks keeps track of a profile of each torrent. Useful metadata is stored in these profiles that help the other parts of DeToks with choosing which torrents to seed leech and recommend to the user. This section describes stored parameters in the profile.

####

## Leeching Strategies

`TODO: add leeching strategies`

## Seeding Strategies

DeToks allows the user to choose between multiple seeding strategies. This section explains the different strategies available.

#### `STRATEGY_RANDOM`

The random strategy simply chooses torrents to seed at random.

#### `STRATEGY_HIGHEST_WATCH_TIME`

This strategy sorts the torrents by watchtime in descending order.

#### `STRATEGY_LOWEST_WATCH_TIME`

This strategy sorts the torrents by watchtime in ascending order.

#### `STRATEGY_HOT`

This strategy sorts the torrents by hopcount, a torrent is considered hot if it is passed along peers a lot.

#### `STRATEGY_RISING`

This strategy first takes the ratio between likes and hops of a torrent to predict wether or not a torrent will be likely to be passed along.

#### `STRATEGY_NEW`

This strategy sorts the torrents for seeding by looking at their first appearance date.

#### `STRATEGY_TOP`

This strategy sorts torrents by number of likes in descending order.

## Tokens

`TODO: add tokens`
