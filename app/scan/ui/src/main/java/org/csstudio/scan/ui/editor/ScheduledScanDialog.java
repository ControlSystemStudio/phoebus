package org.csstudio.scan.ui.editor;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.csstudio.scan.client.ScanClient;
import org.phoebus.ui.time.DateTimePane;

import java.time.LocalDateTime;
import java.time.ZoneId;


public class ScheduledScanDialog extends Dialog<Long> {
    final String scan_name;
    final ScanClient scan_client;
    final String script_xml;
    final boolean queued;

    public ScheduledScanDialog(String scan_name, ScanClient scan_client, String script_xml) {
        this(scan_name, scan_client, script_xml, true);
    }

    public ScheduledScanDialog(String scan_name, ScanClient scan_client, String script_xml, boolean queued) {
        this.scan_name = scan_name;
        this.scan_client = scan_client;
        this.script_xml = script_xml;
        this.queued = queued;

        setTitle("Schedule " + scan_name);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final DateTimePane datetime = new DateTimePane();
        getDialogPane().setContent(datetime);

        setResultConverter(button ->
        {
            if (button == ButtonType.OK) {
                LocalDateTime scheduled = LocalDateTime.ofInstant(datetime.getInstant(), ZoneId.systemDefault());
                try {
                    return scan_client.submitScan(
                            scan_name,
                            script_xml,
                            queued,
                            scheduled
                    );
                }
                catch (Exception e) {
                    return null;
                }
            } else {
                return null;
            }
        });
    }
}
