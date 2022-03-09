package nl.tudelft.trustchain.FOC;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import dalvik.system.DexClassLoader;

import java.util.concurrent.TimeUnit;

//import com.google.gson.Gson;

public class ExecutionActivity extends AppCompatActivity {
    private static Context context;
    private static Fragment.SavedState savedState = null;
    LinearLayout mainLayoutContainer = null;
    LinearLayout tmpLayout = null;
    private Class fragmentClass = null;
    private Fragment mainFragment = null;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        this.printToast("Saving state");
        FragmentManager manager = getSupportFragmentManager();
        savedState = manager.saveFragmentInstanceState(mainFragment);
        FragmentTransaction transaction = manager.beginTransaction();

        transaction.remove(mainFragment);
        try {
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mainLayoutContainer.removeView(tmpLayout);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        this.printToast("attempting restore");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @SuppressLint({"ResourceType"})
    @Override
    public void onResume() {
        this.printToast("attempting resume");
        super.onResume();

        setContentView(R.layout.activity_execution);

        context = getApplicationContext();

        String apkName = "";
        Bundle extras = this.getIntent().getExtras();
        if (extras.containsKey("fileName")) {
            apkName = this.getIntent().getStringExtra("fileName");
        }
        //uncomment if you want to read from the actual phone storage (needs "write" permission)
        final String apkPath = apkName;
        String appName = apkName.substring(apkName.lastIndexOf("/") + 1, apkName.lastIndexOf("."));
        //final String apkPath = context.getExternalFilesDir(null).getAbsolutePath() + "/" + apkName;
        final ClassLoader classLoader = new DexClassLoader(apkPath, context.getCacheDir().getAbsolutePath(), null, this.getClass().getClassLoader());

        mainLayoutContainer = (LinearLayout) findViewById(R.id.llcontainer);

        try {

            fragmentClass = classLoader.loadClass("com.execmodule." + appName + ".MainFragment");
            mainFragment = (Fragment) fragmentClass.newInstance();
            if (savedState != null) {
                this.printToast("savedState not null");
                mainFragment.setInitialSavedState(savedState);
            }

            tmpLayout = new LinearLayout(getApplicationContext());
            tmpLayout.setId(1);

            FragmentManager manager = getSupportFragmentManager();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Display a short message on the screen
     */
    private void printToast(String s) {
        Toast.makeText(this.getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
