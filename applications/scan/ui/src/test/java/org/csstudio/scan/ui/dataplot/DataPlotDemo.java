package org.csstudio.scan.ui.dataplot;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DataPlotDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final DataPlot plot = new DataPlot();
        final Scene scene = new Scene(plot, 800, 600);
        stage.setScene(scene);
        stage.show();

        plot.selectScan(71);
        plot.selectXDevice("xpos");
        plot.addYDevice("ypos");
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}
