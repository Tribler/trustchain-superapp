package nl.tudelft.trustchain.FOC;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;

public class TestClass {

    public static void testFunction() {
        DhtSettings dhtSettings = new com.frostwire.jlibtorrent.DhtSettings();
        AddTorrentParams torrentParams = com.frostwire.jlibtorrent.AddTorrentParams.createInstance();
        AnnounceEntry announceEntry = new com.frostwire.jlibtorrent.AnnounceEntry("");
        TorrentBuilder torrentBuilder = new TorrentBuilder();

        Thread thread = new Thread(){
            public void run(){
                while (true){
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        thread.start();
    }

}
