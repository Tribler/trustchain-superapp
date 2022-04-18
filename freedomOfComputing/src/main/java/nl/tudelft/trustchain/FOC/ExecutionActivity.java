package nl.tudelft.trustchain.FOC;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;

import static nl.tudelft.trustchain.FOC.util.ExtensionUtils.dataDotExtension;
import static nl.tudelft.trustchain.FOC.util.ExtensionUtils.dexExtension;

public class ExecutionActivity extends AppCompatActivity {
    private Fragment mainFragment;
    private FragmentManager manager;
    private String apkName;
    private final static String FILE_NAME = "fileName";

    /**
     * Stores the current state of the dynamically loaded code.
     */
    private void storeState() {
        // Store state next to apk
        String fileName = this.apkName + dataDotExtension;
        try {
            FileOutputStream stream = new FileOutputStream(fileName);
            Parcel p = Parcel.obtain();
            Objects.requireNonNull(manager.saveFragmentInstanceState(mainFragment)).writeToParcel(p, 0);
            byte[] bytes = p.marshall();
            stream.write(bytes);
            stream.close();
            p.recycle();
        } catch (IOException e) {
            this.printToast(e.toString());
        }
    }

    /**
     * Retrieves the state of the dynamically loaded code (including any performed UI actions)
     *
     * @return the latest known state of the dynamically loaded code or null if it does not exist
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Fragment.SavedState getState() {
        // states are stored in the same directories as apks themselves (in the app specific files)
        String fileName = this.apkName + dataDotExtension;
        try {
            Path path = Paths.get(fileName);
            byte[] data = Files.readAllBytes(path);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            Parcelable.Creator<Fragment.SavedState> classLoader = Fragment.SavedState.CREATOR;
            return classLoader.createFromParcel(parcel);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * This method is called by Android indicating that the user no longer interacts with the app and
     * that the state should be saved.
     */
    @Override
    public void onPause() {
        super.onPause();
        this.storeState();
    }

    /**
     * Performs all required initials actions when loading the dynamic code:
     * - Retrieve filename of APK from MainActivityFOC
     * - Dynamically load code of the APK using the DexClassLoader
     * - Restore the state of the dynamically loaded code, if any
     * - Load the dynamic code on a view on the users screen.
     *
     * @param savedInstanceState Default Android savedInstanceState
     */
    @SuppressLint({"ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = this.getIntent().getExtras();
        if (extras.containsKey(FILE_NAME)) {
            this.apkName = this.getIntent().getStringExtra(FILE_NAME);
            assert this.apkName != null;
        } else {
            this.printToast("No APK name supplied");
            return;
        }

        setContentView(R.layout.activity_execution);
        Context context = getApplicationContext();

        //uncomment if you want to read from the actual phone storage (needs "write" permission)
        final String apkPath = this.apkName;
        String activeApp = this.apkName.substring(this.apkName.lastIndexOf("/") + 1, this.apkName.lastIndexOf("."));

        final ClassLoader classLoader = new DexClassLoader(apkPath, context.getCacheDir().getAbsolutePath(), null, this.getClass().getClassLoader());

        try {
            String mainFragmentClass = getMainFragmentClass(apkPath);
            Class<?> fragmentClass = classLoader.loadClass((mainFragmentClass != null) ? mainFragmentClass : "com.execmodule." + activeApp + ".MainFragment");
            this.mainFragment = (Fragment) fragmentClass.newInstance();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Fragment.SavedState state = this.getState();
                if (state != null) {
                    this.mainFragment.setInitialSavedState(state);
                }
            }

            LinearLayout tmpLayout = new LinearLayout(context);
            tmpLayout.setId(1);

            this.manager = getSupportFragmentManager();
            FragmentTransaction transaction = this.manager.beginTransaction();
            transaction.add(tmpLayout.getId(), this.mainFragment, "mainFragment");
            transaction.commit();

            ((LinearLayout) findViewById(R.id.llcontainer)).addView(tmpLayout);
        } catch (Exception e) {
            this.printToast(e.toString());
            Log.i("personal", "Something went wrong");
        }
    }

    /**
     * Retrieves the main fragment class from the specified APK.
     * This class can be in any package. The only requirement is the main fragment should be called exactly 'MainFragment'
     *
     * Deprecation suppression required to use DexFile, which we use to loop through all classes.
     *
     * @param path to the APK.
     * @return the exact location of the main fragment class
     */
    @SuppressWarnings("deprecation")
    private String getMainFragmentClass(String path) {
        try {
            DexFile dx = DexFile.loadDex(path, File.createTempFile("opt", dexExtension,
                getCacheDir()).getPath(), 0);
            for (Enumeration<String> classNames = dx.entries(); classNames.hasMoreElements(); ) {
                String className = classNames.nextElement();
                if (className.contains("MainFragment") && !className.contains("$"))
                    return className;
            }
        } catch (IOException e) {
            Log.w("personal", "Error opening " + path, e);
        }
        return null;
    }

    /**
     * Display a short message on the screen (mainly for debugging purposes).
     */
    private void printToast(String s) {
        Toast.makeText(this.getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
