package nl.tudelft.trustchain.distributedAI.java;


import android.graphics.Color;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.ArrayList;

import nl.tudelft.trustchain.distributedAI.java.utils.LinearRegression;


public class Entity {


    /**
     * The confidence that we want
     */
    public static double theta = 0.99;
    /**
     * The ABSOLUTE MINIMUM confidence that we are able to tolerate
     */
    public static double theta_min = 0.95;
    double[] x;
    double[] y;
    /**
     * List of machines.
     */
    ArrayList<Machine> list = new ArrayList<>();
    /**
     * The number of interations (cycles).
     */
    private int N;
    /**
     * used for graphing
     */
    //XYSeries series = new XYSeries("Convergence over time");
    private XYPlot plot;


    public Entity(double[] x, double[] y, XYPlot plot, int itr) {
        this.x = x;
        this.y = y;
        this.plot = plot;
        this.N = itr;
        pupulate(x, y);
    }


    public void run() {

        perform();

        Pair<Double> average = average();

        System.out.println("OUR_AVERAGE SLOPE: " + average.X() + "  OUR AVERAGE INTERCEPT: " + average.Y());

        System.out.println("[BEST_FIT LINE]: SLOPE:" + new LinearRegression(x, y).slope() + " intercept: " + new LinearRegression(x, y).intercept());

        System.out.println("BEST FIT LINE error:" + LSQRS(x, y, new LinearRegression(x, y).slope(), new LinearRegression(x, y).intercept()));

        System.out.println("OUR LINE error:" + LSQRS(x, y, average.X(), average.Y()));
        double perfectError_maybeNan = LSQRS(x, y, new LinearRegression(x, y).slope(), new LinearRegression(x, y).intercept());
        double perfect_error = Double.isNaN(perfectError_maybeNan) ? 0.01 : perfectError_maybeNan;
        System.out.println("RELATIVE ERROR: " + perfect_error / LSQRS(x, y, average.X(), average.Y()));

        double relative_error = LSQRS(x, y, new LinearRegression(x, y).slope(), new LinearRegression(x, y).intercept()) / Math.max(0.001, LSQRS(x, y, average.X(), average.Y()));
    }


    private double LSQRS(double[] x, double[] y, double slope, double intercept) {

        double RESULT = 0;

        for (int i = 0; i < x.length; i++) {
            RESULT += (y[i] - (x[i] * slope + intercept)) * (y[i] - (x[i] * slope + intercept));
        }

        return RESULT;
    }


    private void perform() {


        int it = N;
        int div = Math.max(1, N / 10);

        ArrayList<Double> xAxis = new ArrayList<>();
        ArrayList<Double> yAxis = new ArrayList<>();

        while (it > 0) {

            for (int i = 0; i < list.size(); i++) {
                int rand2 = (int) (Math.random() * (list.size()));

                list.get(rand2).updateUM(list.get(i));
            }
            Pair<Double> average = average();

            if (it % div == 0) {
                xAxis.add((double) N - it);

                double perfectError_maybeNan = LSQRS(x, y, new LinearRegression(x, y).slope(), new LinearRegression(x, y).intercept());
                double perfect_error = Double.isNaN(perfectError_maybeNan) ? 0.00001 : perfectError_maybeNan;

                yAxis.add(perfect_error / LSQRS(x, y, average.X(), average.Y()));

            }

            it--;
        }
        XYSeries series1 = new SimpleXYSeries(xAxis, yAxis, "series1");

        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null);

        plot.addSeries(series1, series1Format);


    }


    private Pair average() {

        double sumx = 0, sumy = 0;

        for (int i = 0; i < list.size(); i++) {
            sumx += list.get(i).getIntercept();
            sumy += list.get(i).getBias();
        }
        return new Pair(sumx / list.size(), sumy / list.size());
    }


    private void pupulate(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++)
            list.add(new Machine(x[i], y[i]));
    }


    private void print(double[] x, double[] y) {
        for (int i = 0; i < list.size(); i++)
            System.out.println(list.get(i));

        System.out.println("THE REAL VALUE IS:");
        System.out.println("SLOPE: " + new LinearRegression(x, y).slope());
        System.out.println("INTERCEPT: " + new LinearRegression(x, y).intercept());
    }


    public XYPlot getPlot() {
        return this.plot;
    }
}
