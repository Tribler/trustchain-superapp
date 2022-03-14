package nl.tudelft.trustchain.FOC;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.*;
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

public class ExecutionActivity extends AppCompatActivity {
    private Context context;
    LinearLayout mainLayoutContainer = null;
    LinearLayout tmpLayout = null;
    private Class fragmentClass = null;
    private Fragment mainFragment = null;
    private FragmentManager manager;
    private String activeApp;

    private void storeState() {
        String fileName= Environment.getExternalStorageDirectory().getAbsolutePath()  + "/state.dat";
        try {
            FileOutputStream stream = new FileOutputStream(fileName);
            Parcel p = Parcel.obtain();
            manager.saveFragmentInstanceState(mainFragment).writeToParcel(p, 0);
            byte[] bytes = p.marshall();
            stream.write(bytes);
            stream.close();
            p.recycle();
        } catch (IOException e) {
            e.printStackTrace();
            this.printToast(e.toString());
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Fragment.SavedState getState() {
        String fileName= Environment.getExternalStorageDirectory().getAbsolutePath() + "/state.dat";
        try {
            Path path = Paths.get(fileName);
            byte[] data = Files.readAllBytes(path);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            Parcelable.Creator<Fragment.SavedState> classLoader = Fragment.SavedState.CREATOR;
            return classLoader.createFromParcel(parcel);
        } catch (IOException e) {
            e.printStackTrace();
            this.printToast(e.toString());
            return null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        this.storeState();

        // TODO:: Check why this was added, this creates problems when app is tabbed out
        // Remove if not needed, if it is then create a new transaction in onResume to restore
//        FragmentTransaction transaction = manager.beginTransaction();
//
//        transaction.remove(mainFragment);
//        try {
//            transaction.commit();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        mainLayoutContainer.removeView(tmpLayout);

        super.onSaveInstanceState(savedInstanceState);
    }

    @SuppressLint({"ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_execution);

        context = getApplicationContext();

        String apkName = "";
        Bundle extras = this.getIntent().getExtras();
        if (extras.containsKey("fileName")) {
            apkName = this.getIntent().getStringExtra("fileName");
        }
        //uncomment if you want to read from the actual phone storage (needs "write" permission)
        final String apkPath = apkName;
        activeApp = apkName.substring(apkName.lastIndexOf("/") + 1, apkName.lastIndexOf("."));
        //final String apkPath = context.getExternalFilesDir(null).getAbsolutePath() + "/" + apkName;
        final ClassLoader classLoader = new DexClassLoader(apkPath, context.getCacheDir().getAbsolutePath(), null, this.getClass().getClassLoader());

        mainLayoutContainer = (LinearLayout) findViewById(R.id.llcontainer);

        try {
            String mainFragmentClass = getMainFragmentClass(apkPath);
            fragmentClass = classLoader.loadClass((mainFragmentClass != null) ? mainFragmentClass : "com.execmodule." + activeApp + ".MainFragment");
            mainFragment = (Fragment) fragmentClass.newInstance();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Fragment.SavedState state = this.getState();
                if (state != null) {
                    mainFragment.setInitialSavedState(state);
                }
            }

            tmpLayout = new LinearLayout(context);
            tmpLayout.setId(1);

            manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(tmpLayout.getId(), mainFragment, "mainFragment");
            transaction.commit();

            mainLayoutContainer.addView(tmpLayout);
        } catch (Exception e) {
            this.printToast(e.toString());
            Log.i("personal", "Something went wrong");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private String getMainFragmentClass(String path) {
        try {
            DexFile dx = DexFile.loadDex(path, File.createTempFile("opt", "dex",
                getCacheDir()).getPath(), 0);
            for(Enumeration<String> classNames = dx.entries(); classNames.hasMoreElements();) {
                String className = classNames.nextElement();
                if(className.contains("MainFragment") && !className.contains("$"))
                    return className;
            }
        } catch (IOException e) {
            Log.w("personal", "Error opening " + path, e);
        }
        return null;
    }

    /**
     * Display a short message on the screen
     */
    private void printToast(String s) {
        Toast.makeText(this.getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
