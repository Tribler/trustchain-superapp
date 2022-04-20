#!/bin/sh
set -e

ADDRESS=$1

if [ -z "$ADDRESS" ]; then
  echo 'usage: ./add_ethereum.sh ADDRESS'
  exit 1
fi

docker exec trustchain-superapp-ethereum-node \
  geth attach /tmp/geth.ipc \
  --exec "eth.sendTransaction({from:eth.coinbase, to: '$ADDRESS', value: web3.toWei(0.05, 'ether')})"
