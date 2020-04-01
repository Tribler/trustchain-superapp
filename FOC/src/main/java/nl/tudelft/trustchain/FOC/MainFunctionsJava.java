package nl.tudelft.trustchain.FOC;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.SessionParams;
import com.frostwire.jlibtorrent.SettingsPack;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import dalvik.system.DexClassLoader;

import static android.content.Context.MODE_PRIVATE;

public class MainFunctionsJava {
    final static int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;

    public static void requestPermission(Activity thisActivity){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(thisActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(thisActivity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.

        } else {
            // Permission has already been granted
        }
    }

    public static void getTorrent(){

        //String uri = "magnet:?xt=urn:btih:86d0502ead28e495c9e67665340f72aa72fe304e&dn=Frostwire.5.3.6.+%5BWindows%5D&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.istole.it%3A6969&tr=udp%3A%2F%2Fopen.demonii.com%3A1337";
        //String uri = "magnet:?xt=urn:btih:737d38ed01da1df727a3e0521a6f2c457cb812de&dn=HOME+-+a+film+by+Yann+Arthus-Bertrand+%282009%29+%5BEnglish%5D+%5BHD+MP4%5D&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969";
        //String uri = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c";
        String uri = "magnet:?xt=urn:btih:209c8226b299b308beaf2b9cd3fb49212dbd13ec&dn=Tears+of+Steel&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Ftears-of-steel.torrent";
        //String uri = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Fbig-buck-bunny.torrent";

        final SessionManager s = new SessionManager();

        SettingsPack sp = new SettingsPack();

        SessionParams params = new SessionParams(sp);

        final CountDownLatch signal = new CountDownLatch(1);

        s.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null;
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                switch (type) {
                    case ADD_TORRENT:
                        Log.i("personal", "Torrent added");
                        //System.out.println("Torrent added");
                        ((AddTorrentAlert) alert).handle().resume();
                        break;
                    case BLOCK_FINISHED:
                        BlockFinishedAlert a = (BlockFinishedAlert) alert;
                        int p = (int) (a.handle().status().progress() * 100);
                        Log.i("personal", "Progress: " + p + " for torrent name: " + a.torrentName());
                        Log.i("personal", Long.toString(s.stats().totalDownload()));
                        //System.out.println("Progress: " + p + " for torrent name: " + a.torrentName());
                        //System.out.println(s.stats().totalDownload());
                        break;
                    case TORRENT_FINISHED:
                        Log.i("personal", "Torrent finished");
                        //System.out.println("Torrent finished");
                        signal.countDown();
                        break;
                }
            }
        });

        s.start(params);

        boolean isMagnet = true;
        if (isMagnet) {


            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long nodes = s.stats().dhtNodes();
                    // wait for at least 10 nodes in the DHT.
                    if (nodes >= 10) {
                        Log.i("personal", "DHT contains " + nodes + " nodes");
                        //System.out.println("DHT contains " + nodes + " nodes");
                        //signal.countDown();
                        timer.cancel();
                    }
                }
            }, 0, 1000);



//        Log.i("personal", "Waiting for nodes in DHT (10 seconds)...");
//        //System.out.println("Waiting for nodes in DHT (10 seconds)...");
//        boolean r = false;
//        try {
//            r = signal.await(15, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        if (!r) {
//            Log.i("personal", "DHT bootstrap timeout");
//            //System.out.println("DHT bootstrap timeout");
//            System.exit(0);
//        }


            Log.i("personal", "Fetching the magnet uri, please wait...");
            //System.out.println("Fetching the magnet uri, please wait...");
            byte[] data = s.fetchMagnet(uri, 30);

            if (data != null) {
                Log.i("personal", Entry.bdecode(data).toString());
                //System.out.println(Entry.bdecode(data));
            } else {
                Log.i("personal", "Failed to retrieve the magnet");
                //System.out.println("Failed to retrieve the magnet");
            }

            TorrentInfo ti = TorrentInfo.bdecode(data);
            s.download(ti, new File("/storage/emulated/0"));

        } else {

            final String torrent = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sintel.torrent";
            try {
                readTorrent(torrent);
            } catch (IOException e) {
                e.printStackTrace();
            }
            File torrentFile = new File(torrent);
            TorrentInfo ti = new TorrentInfo(torrentFile);
            Log.i("personal", "Storage of downloads: " + torrentFile.getParentFile().toString());
            s.download(ti, torrentFile.getParentFile());

        }

        try {
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        s.stop();
    }

    public static void loadDynamicCode(Activity thisActivity){

        try {
            final String libPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Injected.jar";
            File dexOutputDir = thisActivity.getDir("dex", MODE_PRIVATE);
            File tmpDir = new File(libPath);
            ;
            boolean exists = tmpDir.exists();
            boolean extStore = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
            if (exists && extStore)
                Log.i("personal", "yes");

            final DexClassLoader classloader = new DexClassLoader(libPath, dexOutputDir.getAbsolutePath(), null,
                thisActivity.getClass().getClassLoader());
            final Class<Object> classToLoad = (Class<Object>) classloader.loadClass("com.example.injected.Injected");
            //final Class<Object> classToLoad = (Class<Object>) classloader.loadClass("p000.Example");

            final Object myInstance  = classToLoad.newInstance();

            final Method printStuff = classToLoad.getMethod("printStuff");

            printStuff.invoke(myInstance);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void readTorrent(String torrent) throws IOException {
        File torrentFile = new File(torrent);

        Log.i("personal", "Reading all in memory");
        //System.out.println("Reading all in memory");
        TorrentInfo ti = new TorrentInfo(torrentFile);
        Log.i("personal", "info-hash: " + ti.infoHash());
        Log.i("personal", ti.toEntry().toString());
        //System.out.println("info-hash: " + ti.infoHash());
        //System.out.println(ti.toEntry());

        Log.i("personal", "Reading with memory mapped");
        //System.out.println("Reading with memory mapped");
        FileChannel fc = new RandomAccessFile(torrent, "r").getChannel();
        MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        TorrentInfo ti2 = new TorrentInfo(buffer);
        Log.i("personal", "info-hash: " + ti2.infoHash());
        Log.i("personal", "creator: " + ti2.creator());
        Log.i("personal", "comment: " + ti2.comment());
        Log.i("personal", ti2.toEntry().toString());
        //System.out.println("info-hash: " + ti2.infoHash());
        //System.out.println("creator: " + ti2.creator());
        //System.out.println("comment: " + ti2.comment());
        //System.out.println(ti2.toEntry());
    }
}
