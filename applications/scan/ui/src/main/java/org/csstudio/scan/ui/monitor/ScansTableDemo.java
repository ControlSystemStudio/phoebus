package org.csstudio.scan.ui.monitor;

import java.util.Collections;
import java.util.List;

import org.csstudio.scan.client.ScanInfoModel;
import org.csstudio.scan.client.ScanInfoModelListener;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ScansTableDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final ScansTable scans = new ScansTable();

        final Scene scene = new Scene(scans, 600, 300);
        stage.setScene(scene);
        stage.show();

        ScanInfoModel model = ScanInfoModel.getInstance();
        model.addListener(new ScanInfoModelListener()
        {
            @Override
            public void scanServerUpdate(ScanServerInfo server_info)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void scanUpdate(List<ScanInfo> infos)
            {
                scans.update(infos);
            }

            @Override
            public void connectionError()
            {
                scans.update(Collections.emptyList());
            }
        });

        // TODO model.removeListener(listener);
        // TODO model.release();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
