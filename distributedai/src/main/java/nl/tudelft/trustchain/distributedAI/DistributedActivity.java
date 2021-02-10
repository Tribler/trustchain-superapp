package nl.tudelft.trustchain.distributedAI;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.androidplot.xy.XYPlot;

import nl.tudelft.trustchain.common.BaseActivity;
import nl.tudelft.trustchain.distributedAI.java.Entity;

public class DistributedActivity extends BaseActivity {
    @Override
    public int getNavigationGraph() {
        return R.navigation.nav_graph_distributed;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distributed);

    }

    public void sendMessage(View v) {
        EditText x_val = findViewById(R.id.dna_x_value);
        String x0 = x_val.getText().toString();

        EditText x_val_2 = findViewById(R.id.dna_x_value2);
        String x1 = x_val_2.getText().toString();

        EditText y_val = findViewById(R.id.dna_y_value);
        String y0 = y_val.getText().toString();

        EditText y_val_2 = findViewById(R.id.dna_y_value2);
        String y1 = y_val_2.getText().toString();

        EditText nr_iterations = findViewById(R.id.dna_iterations);
        String itr = nr_iterations.getText().toString();

        //we add an extra point (1,2), in order to never get a line with 0 error
        //so no need to deal with NaN or infinity
        double[] x = {Double.parseDouble(x0), Double.parseDouble(x1), 1.0};
        double[] y = {Double.parseDouble(y0), Double.parseDouble(y1), .0};


        XYPlot plot = findViewById(R.id.plot);

        plot.clear();

        Entity entity = new Entity(x, y, plot, Integer.parseInt(itr));

        entity.run();
        plot.invalidate();


    }

}
