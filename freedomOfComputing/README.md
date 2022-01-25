# Freedom-of-Computing App

## Overview

Freedom-of-Computing is an extension-app of the trustchain app. It enables the users to share files in the forms of torrents, through a torrent peer-to-peer (P2P) network, which is the same peer-to-peer network that we call "DemoCommunity" within the app. More specifically though, the purpose of the torrent network is to enable users to freely distribute code in the form of .apk files. The code can be uploaded (seeded) and downloaded by the users, who can then dynamically load that code and execute it. The code, apart from being an .apk file, needs to have a specific format for its execution to work, the requirements/constraints are listed below.

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

## User Guide

### Upload
Enter the apk name in the upper input box and press "Upload torrent". The name needs to include the location of the file if the file is not stored in the phone's main storage folder.
Press "Inform peers about seeding" to let the other connected peers know about the file you are uploading (seeding).

### Download/Execute
1. Retrieve the list of currently seeded torrents by clicking the "Retrieve list of torrents" button. Press on one of the available options.

<img src="../doc/freedomOfComputing/Screenshot%202020-04-29%20at%2023.37.52.png" width="180">

2. Download

The corresponding magnet link should auto-fill in the first input box. Proceed with the download by pressing "Download magnet link".

<img src="../doc/freedomOfComputing/Screenshot%202020-04-29%20at%2023.38.15.png" width="180">

3. Execute apk

This should result in the torrent information being displayed and in the apk name auto-filling up the lower text box. You can press "Execute module (apk)" once the download has finished, indicated by a full progress bar.
Once the button has been pressed the downloaded application should launch.

<img src="../doc/freedomOfComputing/Screenshot%202020-04-29%20at%2023.38.40.png" width="180">

## How to develop a module for execution in our app

### Code and layout constraints
Our execution platform is currently only able to load fragments from an Android Package Kit(APK). Our platform loads one fixed fragment class from the APK to run inside of our platform's activity. The fragment that is loaded can still depend on other classes in different files inside of the APK.

Here are the constraints a developer needs to strictly follow to develop an app for execution in the Freedom of Computing execution platform:

* Our platform starts executing from the class named "com.execmodule._name of the APK_.MainFragment" so it is necessary to have this fragment as the entry point for the module

* As our platform internally uses DexClassLoader in order to load classes from an APK, it is really difficult to load precompiled layout resources from the APK. Therefore the developer needs to programatically build the layout instead of using XML files or pre compiled resources to build a UI.

Other then these constraints, a developer can follow the normal android development procedures for further functionalities.

**NOTE:** Our platform follows the normal android activity behavior, for example when the screen orientation changes the activity is destroyed and created again and thus the MainFragment is destroyed and recreated as well but not reloaded from the APK. Therefore, it is the developer's responsibility to implement state persistence as it suits the developer Further information on saving UI states temporarily/persistantly can be found on: https://developer.android.com/topic/libraries/architecture/saving-states and https://developer.android.com/training/data-storage/shared-preferences

### A "Hello World!" example
```java
package com.execmodule.helloworld;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MainFragment extends Fragment {

    public MainFragment() {
        // Required empty public constructor
    }
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        LinearLayout mylayout = new LinearLayout(container.getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Button mybutton = new Button(container.getContext());
        mybutton.setText("hello world");
        mybutton.setId(2);
        mybutton.setLayoutParams(lp);
        mylayout.addView(mybutton);

        View view = mylayout;
        return view;
    }
}
```

<img src="../doc/freedomOfComputing/Screenshot%202020-04-29%20at%2023.26.46.png" width="180">

**NOTE:** A more advanced example/demo can be found at: https://github.com/rootmonkey/trustchain-foc-demoapp

## Extra info

At any point, a download can be stopped by pressing at the stop button. Additionally, initializing a different download will cause the last download to stop, but the progress remains until the next time.

The torrent inputBox must include either a magnet link or a torrent file (extension .torrent included), except for the case of uploading, where the actual extension of the file is required (e.g. image.png). If an inputBox is left empty, the default value is assumed by the app.
