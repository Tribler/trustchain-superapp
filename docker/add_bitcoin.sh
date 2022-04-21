#!/bin/sh
set -e

ADDRESS=$1

if [ -z "$ADDRESS" ]; then
  echo 'usage: ./add_bitcoin.sh ADDRESS'
  exit 1
fi

docker exec -u bitcoin trustchain-superapp-bitcoin-node \
  bitcoin-cli -regtest generatetoaddress 100 "$ADDRESS"
