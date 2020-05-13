package Statistics;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.io.CSV;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class LineChart {
    JFreeChart chart;
    String CSVFilePath;
    String chartTitle;
    XYSeriesCollection dataset;

    public LineChart(String CSVFilePath, String chartTitle) {
//        DefaultCategoryDataset dataset = createDataset(CSVFilePath);
        dataset = new XYSeriesCollection();
        this.CSVFilePath = CSVFilePath;
        this.chartTitle = chartTitle;

        String line = "";
        ArrayList<XYSeries> series = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(CSVFilePath));
            line = br.readLine();
            if (line == null) {
                return;
            }
            String[] titles = line.split(",");
            double it = 0.0;
            for (int i = 0; i < titles.length; i++) {
                series.add(new XYSeries(titles[i]));
            }

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (int i = 0; i < series.size(); i++)
                    series.get(i).add(Double.valueOf(values[i]), (Number) it);

                it += 1;
            }
            for (XYSeries s : series)
                dataset.addSeries(s);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createChart() {
        if (dataset == null || dataset.getSeriesCount() <= 0)
            return;
        chart = ChartFactory.createXYLineChart(chartTitle,null,null,dataset, PlotOrientation.HORIZONTAL,true, false, false);
    }

    public void saveChart() {
        if (chart == null)
            return;
        String fileName = new Date().toString().replaceAll(" ","_").replaceAll(":","-")+".jpeg";
        File file = new File(fileName);
        try {
            ChartUtils.saveChartAsJPEG(file,this.chart,800,500);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
