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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;

public class ExecutionActivity extends AppCompatActivity {
    private Fragment mainFragment;
    private FragmentManager manager;

    @SuppressWarnings("deprecation")
    private void storeState() {
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/state.dat";
        try {
            FileOutputStream stream = new FileOutputStream(fileName);
            Parcel p = Parcel.obtain();
            Objects.requireNonNull(manager.saveFragmentInstanceState(mainFragment)).writeToParcel(p, 0);
            byte[] bytes = p.marshall();
            stream.write(bytes);
            stream.close();
            p.recycle();
        } catch (IOException e) {
            e.printStackTrace();
            this.printToast(e.toString());
        }
    }

    @SuppressWarnings("deprecation")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Fragment.SavedState getState() {
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/state.dat";
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
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        this.storeState();
    }

    @SuppressLint({"ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String apkName;
        Bundle extras = this.getIntent().getExtras();
        if (extras.containsKey("fileName")) {
            apkName = this.getIntent().getStringExtra("fileName");
            assert apkName != null;
        } else {
            this.printToast("No APK name supplied");
            return;
        }

        setContentView(R.layout.activity_execution);
        Context context = getApplicationContext();

        //uncomment if you want to read from the actual phone storage (needs "write" permission)
        final String apkPath = apkName;
        String activeApp = apkName.substring(apkName.lastIndexOf("/") + 1, apkName.lastIndexOf("."));
        //final String apkPath = context.getExternalFilesDir(null).getAbsolutePath() + "/" + apkName;
        final ClassLoader classLoader = new DexClassLoader(apkPath, context.getCacheDir().getAbsolutePath(), null, this.getClass().getClassLoader());

        try {
            String mainFragmentClass = getMainFragmentClass(apkPath);
            Class<?> fragmentClass = classLoader.loadClass((mainFragmentClass != null) ? mainFragmentClass : "com.execmodule." + activeApp + ".MainFragment");
            mainFragment = (Fragment) fragmentClass.newInstance();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Fragment.SavedState state = this.getState();
                if (state != null) {
                    mainFragment.setInitialSavedState(state);
                }
            }

            LinearLayout tmpLayout = new LinearLayout(context);
            tmpLayout.setId(1);

            manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(tmpLayout.getId(), mainFragment, "mainFragment");
            transaction.commit();

            ((LinearLayout) findViewById(R.id.llcontainer)).addView(tmpLayout);
        } catch (Exception e) {
            this.printToast(e.toString());
            Log.i("personal", "Something went wrong");
        }
    }

    @SuppressWarnings("deprecation")
    private String getMainFragmentClass(String path) {
        try {
            DexFile dx = DexFile.loadDex(path, File.createTempFile("opt", "dex",
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
     * Display a short message on the screen
     */
    private void printToast(String s) {
        Toast.makeText(this.getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
