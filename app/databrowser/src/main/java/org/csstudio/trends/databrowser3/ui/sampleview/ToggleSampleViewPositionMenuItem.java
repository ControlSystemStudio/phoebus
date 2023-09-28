package org.csstudio.trends.databrowser3.ui.sampleview;


import javafx.scene.control.MenuItem;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.ui.Perspective;
import org.phoebus.ui.javafx.ImageCache;

/** Menu item to toggle position of SampleView between bottom_tabs and main view
 * @author Thomas Lehrach
 */
public class ToggleSampleViewPositionMenuItem extends MenuItem
{
    public ToggleSampleViewPositionMenuItem(final Perspective perspective)
    {
        if (perspective.isSampleViewInBottomTabs()) {
            setText(Messages.SampleView_Move_Up);
            setGraphic(ImageCache.getImageView(SampleView.class, "/icons/up.png"));
            setOnAction(event -> perspective.setSampleviewLocation(false));
        }
        else {
            setGraphic(ImageCache.getImageView(SampleView.class, "/icons/down.png"));
            setText(Messages.SampleView_Move_Down);
            setOnAction(event -> perspective.setSampleviewLocation(true));
        }
    }
}
