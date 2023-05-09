async function loadServer() {

	const { Server } = await import('bittorrent-tracker');
	return Server;
}
const querystring = require('node:querystring');
const net = require('net');
const express = require('express')
const app = express()
var pump = new net.Socket()
pump.connect(8082, "localhost", function() {
    
//  pump.write(pumpStr)       // <-- this sends 0xc3 0xbe 0x74 0x01 instead
})
const binaryToHex = str => {
    if (typeof str !== 'string') {
      str = String(str)
    }
    return Buffer.from(str, 'binary')
  }
const querystringParse = q => querystring.parse(q, null, null, { decodeURIComponent: unescape })
const hexToBinary = str => {
    if (typeof str !== 'string') {
      str = String(str)
    }
    return Buffer.from(str, 'hex')
  }

loadServer().then(Server => {
const server = new Server({
    http: false, // we do our own
    udp: false, // not interested
    ws: false, // not interested
   
  })
  server.on('error', err => {
    console.error(`ERROR: ${err.message}`)
  })
  server.on('warning', err => {
    console.log(`WARNING: ${err.message}`)
  })
  server.on('update', addr => {
    console.log(`update: ${addr}`)
  })
  server.on('complete', addr => {
    console.log(`complete: ${addr}`)
  })
  server.on('start', addr => {
    console.log(`start: ${addr}`)
  })
  server.on('stop', addr => {
    console.log(`stop: ${addr}`)
  })


  const onHttpRequest = server.onHttpRequest.bind(server)
    app.get('/announce', (req, res)=> {
        console.log("g")
        const s = req.url.split('?')
        const params = querystringParse(s[1])
        console.log(binaryToHex(params.info_hash).toString("hex"))
        if(server.torrents[binaryToHex(params.info_hash).toString("hex")] == undefined){
            pump.write(binaryToHex(params.info_hash).toString("hex"))    // <-- this throws a type error
        } 
        onHttpRequest(req,res)})


    
    app.get('/scrape', onHttpRequest)

    app.get('/keys', (req, res) =>{
        console.log("KEYS")
        console.log(server.torrents)
    })
    app.listen(8080)
    
    

});

// const Server = require('bittorrent-tracker').Server

