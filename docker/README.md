# Docker coin nodes

## Prerequisities:

* Docker daemon installed and running (tested on version 20.10.12)

## Running nodes

In order to run the coin nodes, execute the following command in the directory with the _
docker-compose.yml_ file

```shell
docker-compose up
```

To run the nodes in background:

```shell
docker-compose up -d
```

To stop the nodes:

```shell
docker-compose stop
```

To stop the nodes and remove local blockchain data:

```shell
docker-compose down --volumes
```

## Working with the Bitcoin node

In order to view the peers (eg. verify if the Android apps are connected):

```shell
docker exec -u bitcoin trustchain-superapp-bitcoin-node bitcoin-cli -regtest getpeerinfo
```

In order to send certain amount of Bitcoins to the address `<ADDR>`, execute:

```shell
./add_bitcoin.sh <ADDR>
```

or

```shell
docker exec -u bitcoin trustchain-superapp-bitcoin-node bitcoin-cli -regtest generatetoaddress 100 '<ADDR>'
```

## Working with the Ethereum node

In order to send 0.05 ETH to the address `<ADDR>`, execute:

```shell
./add_ethereum.sh <ADDR>
```

or

```shell
docker exec trustchain-superapp-ethereum-node geth attach /tmp/geth.ipc --exec 'eth.sendTransaction({from:eth.coinbase, to: "<ADDR>", value: web3.toWei(0.05, "ether")})'
```
