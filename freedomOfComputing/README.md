# Freedom-of-Computing App
 
## Describing the main use case of our app
We present the main use case of our app, step by step, through which our contributions to the whole “superapp” project become visible.

### Creating a torrent out of any file
The user has a file he wants to distribute to the rest of the peers in the superapp’s network, say “image.png”. Suppose this file resides in the main directory of his Android phone’s storage (the root directory of Internal Storage). The user writes the name of the file in the torrent inputBox, including the extension (i.e. image.png). He presses on the “Upload torrent” button, which causes both a torrent file and a magnet link to be created, both representing the given file. The file then begins to be seeded by the user.

### Informing the other peers about the seeding torrent
The user presses the “Inform peers about seeding” button, which sends the magnet link to every other peer in the network. Every recipient can now check his incoming magnet links / messages by pressing the “Retrieve list of torrents” button. Upon pressing that button, a list of the available torrents appears at the bottom of the scrollable screen.

### Downloading the seeding torrent, as a recipient
The recipient can press on any of the list entries, which represent a seeded torrent each. The magnet link of that torrent will then fill up the torrent inputBox. Finally, by pressing the “Download (magnet link)” button, and since the corresponding inputBox is filled up, the seeded torrent will begin downloading for the recipient. The download might take some time to actually start, especially when the only seeder is the creator of the torrent. The downloaded file will also be placed in the main storage directory of the phone.

### Executing the downloaded apk/jar
The user can press the “Execute code from jar” button to execute the file specified in the apk inputBox, which should also be stored in the main storage directory of the phone.

## Guide

### Upload
Enter file location in the upper text box and press "upload torrent". Press "inform peers".

### Download/Execute
1. Retrieve Torrents
click Retrieve list of torrents and press on one of the available options.

<img src="https://i.imgur.com/JVNw9U5.jpg" width="180">

2. Download This should auto fill in the first text box. Then proceed with the download by pressing download magnet.

<img src="https://i.imgur.com/qJj4Edv.jpg" width="180">

3. Execute apk
This should result in the information to be displayed such that you are able to enter the apk name into the lower text box and hit execute.
Once the button has been pressed the application should launch.

<img src="https://i.imgur.com/0rAe9Q3.jpg" width="180">

## How to develop a module for execution in our app

Code and layout constraints
A "Hello World!" example
Extra functionalities

## Extra info

At any point, a download can be stopped by pressing at the stop button. Additionally, initializing a different download will cause the last download to stop, but the progress remains until the next time.

The torrent inputBox must include either a magnet link or a torrent file (extension .torrent included), except for the case of uploading, where the actual extension of the file is required (e.g. image.png). If an inputBox is left empty, the default value is assumed by the app.
