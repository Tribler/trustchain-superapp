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

import java.util.HashMap;

public class ExecutionActivity extends AppCompatActivity {
    private Context context;
    private static HashMap<String, Fragment.SavedState> savedStateMap = new HashMap<>(10);
    LinearLayout mainLayoutContainer = null;
    LinearLayout tmpLayout = null;
    private Class fragmentClass = null;
    private Fragment mainFragment = null;
    private FragmentManager manager;
    private String activeApp;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        this.printToast("Saving state");
        savedStateMap.put(activeApp, manager.saveFragmentInstanceState(mainFragment));

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
        this.printToast("attempting resume");

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

            fragmentClass = classLoader.loadClass("com.execmodule." + activeApp + ".MainFragment");
            mainFragment = (Fragment) fragmentClass.newInstance();
            if (savedStateMap.containsKey(activeApp)) {
                this.printToast("savedState not null");
                mainFragment.setInitialSavedState(savedStateMap.get(activeApp));
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

    /**
     * Display a short message on the screen
     */
    private void printToast(String s) {
        Toast.makeText(this.getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
