package nl.tudelft.trustchain.dna.java.utils;//
//
//package nl.tudelft.cs4160.trustchain_android.dna.utils;
//
//import org.jfree.chart.ChartFactory;
//import org.jfree.chart.ChartPanel;
//import org.jfree.chart.JFreeChart;
//import org.jfree.chart.plot.PlotOrientation;
//import org.jfree.data.xy.XYSeries;
//import org.jfree.data.xy.XYSeriesCollection;
//import org.jfree.ui.ApplicationFrame;
//import org.jfree.ui.RefineryUtilities;
//
//import java.util.ArrayList;
//
//
//public class XYSeriesDemo extends ApplicationFrame {
//
//
//
//    /**
//     * A demonstration application showing an XY series containing a null value.
//     *
//     * @param title  the frame title.
//     */
//    public XYSeriesDemo(final String title , XYSeries series) {
//
//        super(title);
//
//
//        final XYSeriesCollection data = new XYSeriesCollection(series);
//        final JFreeChart chart = ChartFactory.createXYLineChart(
//                "Demo",
//                "Number of cycles",
//                "relative error to best-fit line",
//                data,
//                PlotOrientation.VERTICAL,
//                true,
//                true,
//                false
//        );
//
//        final ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
//        setContentPane(chartPanel);
//
//    }
//}
