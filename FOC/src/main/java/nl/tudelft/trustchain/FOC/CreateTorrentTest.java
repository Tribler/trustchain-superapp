package nl.tudelft.trustchain.FOC;

import android.os.Environment;
import android.util.Log;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Pair;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.jlibtorrent.TorrentBuilder;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.Vectors;
import com.frostwire.jlibtorrent.WebSeedEntry;
import com.frostwire.jlibtorrent.swig.*;
import com.frostwire.jlibtorrent.swig.create_torrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.frostwire.jlibtorrent.swig.libtorrent.add_files_ex;
import static com.frostwire.jlibtorrent.swig.libtorrent.set_piece_hashes_ex;

/**
 * @author gubatron
 * @author aldenml
 */
public class CreateTorrentTest {


    public static void testFromFile() throws IOException {
        final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/image.png");
        if (!file.exists()){
            Log.i("personal", "doesnt exist");
        }
        //Utils.writeByteArrayToFile(f, new byte[]{0}, false);

        file_storage fs = new file_storage();
        add_files_listener l1 = new add_files_listener() {
            @Override
            public boolean pred(String p) {
                //assertEquals(f.getAbsolutePath(), p);
                return true;
            }
        };
        add_files_ex(fs, file.getAbsolutePath(), l1, new create_flags_t());
        com.frostwire.jlibtorrent.swig.create_torrent ct = new com.frostwire.jlibtorrent.swig.create_torrent(fs);
        set_piece_hashes_listener l2 = new set_piece_hashes_listener() {
            @Override
            public void progress(int i) {
                /*assertTrue(i >= 0);*/
            }
        };

        //ct.add_tracker("udp://tracker.publicbt.com:80/announce", 0);
        //ct.add_tracker("udp://tracker.openbittorrent.com:80/announce", 0);

        error_code ec = new error_code();
        set_piece_hashes_ex(ct, file.getParent(), l2, ec);
        //assertEquals(ec.value(), 0);
        entry torrent = ct.generate();
        byte_vector buffer = torrent.bencode();

        OutputStream os = null;
        try {

        os = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/image.torrent"));
            os.write(Vectors.byte_vector2bytes(buffer), 0, Vectors.byte_vector2bytes(buffer).length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        TorrentInfo ti = TorrentInfo.bdecode(Vectors.byte_vector2bytes(buffer));
        String magnet_link = "magnet:?xt=urn:btih:" + ti.infoHash() + "&dn=" + ti.name();
        Log.i("personal", magnet_link);
        //ti.creator();
        //assertEquals(1, ti.numFiles());

        //SessionManager s = new SessionManager();
        //Entry entry = ti.toEntry();
        //s.dhtPutItem(entry);
    }

    public void testFromDir() throws IOException {
        String s = System.getProperty("user.dir");
        File dir = new File(s);
        File f1 = new File(dir, "test.txt");
        writeByteArrayToFile(f1, new byte[]{0}, false);
        File f2 = new File(dir, "test1.txt");
        writeByteArrayToFile(f2, new byte[]{0}, false);

        file_storage fs = new file_storage();
        add_files_listener l1 = new add_files_listener() {
            @Override
            public boolean pred(String p) {
                return true;
            }
        };
        add_files_ex(fs, dir.getAbsolutePath(), l1, new create_flags_t());
        com.frostwire.jlibtorrent.swig.create_torrent ct = new create_torrent(fs);
        set_piece_hashes_listener l2 = new set_piece_hashes_listener() {
            @Override
            public void progress(int i) {
                //assertTrue(i >= 0);
            }
        };
        error_code ec = new error_code();
        set_piece_hashes_ex(ct, dir.getParent(), l2, ec);
        //assertEquals(ec.value(), 0);
        entry e = ct.generate();
        byte_vector buffer = e.bencode();
        TorrentInfo ti = TorrentInfo.bdecode(Vectors.byte_vector2bytes(buffer));
        //assertEquals(2, ti.numFiles());
    }


    public void testUsingBuilder() throws IOException {
        String s = System.getProperty("user.dir");
        File dir = new File(s);
        File f1 = new File(dir, "test.txt");
        writeByteArrayToFile(f1, new byte[]{0}, false);
        File f2 = new File(dir, "test1.txt");
        writeByteArrayToFile(f2, new byte[]{0}, false);

        TorrentBuilder b = new TorrentBuilder();
        TorrentBuilder.Result r = b.path(dir)
                .comment("comment")
                .creator("creator")
                .addUrlSeed("http://urlseed/")
                .addHttpSeed("http://httpseed/")
                .addNode(new Pair<>("1.1.1.1", 1))
                .addTracker("udp://tracker/")
                .setPrivate(true)
                .addSimilarTorrent(Sha1Hash.min())
                .addCollection("collection")
                .generate();

        TorrentInfo ti = TorrentInfo.bdecode(r.entry().bencode());
        //assertEquals("comment", ti.comment());
        //assertEquals("creator", ti.creator());

        ArrayList<WebSeedEntry> seeds = ti.webSeeds();
        for (WebSeedEntry e : seeds) {
            if (e.type() == WebSeedEntry.Type.URL_SEED) {
                //assertEquals("http://urlseed/", e.url());
            }
            if (e.type() == WebSeedEntry.Type.HTTP_SEED) {
                //assertEquals("http://httpseed/", e.url());
            }
        }

        //assertEquals("1.1.1.1", ti.nodes().get(0).first);
        //assertEquals("udp://tracker/", ti.trackers().get(0).url());
        //assertEquals(true, ti.isPrivate());
        //assertTrue(ti.similarTorrents().get(0).isAllZeros());
        //assertEquals("collection", ti.collections().get(0));
        //assertEquals(2, ti.numFiles());
    }


    public void testBuilderListener() throws IOException {
        String s = System.getProperty("user.dir");
        File dir = new File(s);
        File f1 = new File(dir, "test.txt");
        writeByteArrayToFile(f1, new byte[]{0, 0}, false);
        File f2 = new File(dir, "test1.txt");
        writeByteArrayToFile(f2, new byte[]{0, 0}, false);

        final AtomicBoolean b1 = new AtomicBoolean();
        final AtomicBoolean b2 = new AtomicBoolean();

        TorrentBuilder b = new TorrentBuilder();
        TorrentBuilder.Result r = b.path(dir)
                .listener(new TorrentBuilder.Listener() {
                    @Override
                    public boolean accept(String filename) {
                        b1.set(true);
                        return true;
                    }

                    @Override
                    public void progress(int piece, int total) {
                        b2.set(true);
                    }
                })
                .generate();

        TorrentInfo ti = TorrentInfo.bdecode(r.entry().bencode());
        //assertEquals(2, ti.numFiles());
        //assertTrue(b1.get());
        //assertTrue(b2.get());
    }

    public static void writeByteArrayToFile(File file, byte[] data, boolean append) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file, append);
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            //Files.closeQuietly(out);
        }
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    /*
    @Test
    public void testBuilderMerkle() throws IOException {
        File dir = folder.newFolder();
        File f1 = new File(dir, "test.txt");
        Utils.writeByteArrayToFile(f1, new byte[]{0, 0, 0}, false);
        File f2 = new File(dir, "test1.txt");
        Utils.writeByteArrayToFile(f2, new byte[]{0, 0, 0}, false);

        TorrentBuilder b = new TorrentBuilder();
        TorrentBuilder.Result r = b.path(dir)
                .flags(b.flags() | TorrentBuilder.Flags.MERKLE.swig())
                .generate();

        TorrentInfo ti = TorrentInfo.bdecode(r.entry().bencode());
        assertEquals(2, ti.numFiles());

        ArrayList<Sha1Hash> tree = r.merkleTree();
        assertTrue(tree.size() >= 0);
        ti.merkleTree(tree);
        assertEquals(tree.get(0), ti.merkleTree().get(0));
    }*/

    /*
    @Test
    public void testMerkleFlag() throws IOException {
        TorrentBuilder b = new TorrentBuilder();

        assertFalse(b.merkle());
        b.merkle(false);
        assertFalse(b.merkle());
        b.merkle(true);
        assertTrue(b.merkle());
        b.merkle(true);
        assertTrue(b.merkle());
        b.merkle(false);
        assertFalse(b.merkle());
        b.merkle(false);
        assertFalse(b.merkle());
    }*/
}
