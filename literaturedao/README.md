# Literature Dao

LiteratureDao app aims to be a decentralized scientific literature repository, proving:
Sharing, storing, and searching of scientific publications through the p2p ipv8 network.

## Features

- Peers in the LiteratureDao community can contribute literature in .PDF format
- Literature contribution files get parsed and extracted information (keywords)
- The extracted information can be searched locally
- A peer can choose to contribute files through a url to a pdf. This pdf will then be ingested.
- The app automatically downloads some of the files from other peers so the files are more visible in the network

<img src="https://user-images.githubusercontent.com/33283063/167591620-a9547f63-e778-4ea9-a594-a7d1d9a4f169.gif" width="180">
<img src="https://user-images.githubusercontent.com/33283063/167591634-6e0b8aaf-11c8-4ee7-b36d-616255fa5347.PNG" width="180">
<img src="https://user-images.githubusercontent.com/33283063/167591635-a68c0d2c-16de-4a44-ba73-fc52688252eb.gif" width="180">
<img src="https://user-images.githubusercontent.com/33283063/167591643-75a305ae-2098-4138-9f60-14818f63000e.gif" width="180">

## Lacking
The remote search functionality did not get through the testing phase, it has backend components and front end ones.
There is local searching and a communication protocol implemented for this. However it is not working yet somewhere in the connectivity.
For the app to function completely as intended this should be fully completed first. Also the remote search and local search should be merged into one window.

## Improvement opportunities
There are two areas of improvement: Higher quality parsing of contributed literature, and scaling of the network.
- Extract the title from the literature content automatically
- Extract author information from the literature content automatically
- Allow remote key word search and download of a specific literature
