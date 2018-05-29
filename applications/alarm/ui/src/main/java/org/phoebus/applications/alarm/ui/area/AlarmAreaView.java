package org.phoebus.applications.alarm.ui.area;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Paint;

public class AlarmAreaView extends GridPane implements AlarmClientListener
{

	@SuppressWarnings("unused")
	private final AlarmClient model;
	private final AreaFilter areaFilter;

	private final int level = 2;
	private final int col_num = 2;

	private final ConcurrentHashMap<AlarmTreeItem<?>, /* View Item */Label> itemViewMap = new ConcurrentHashMap<>();
	/** Items to update, ordered by time of original update request
    *
    *  SYNC on access
    */
    private final Set<AlarmTreeItem<?>> items_to_update = new LinkedHashSet<>();

    /** Throttle [5Hz] used for updates of existing items */
    private final UpdateThrottle throttle = new UpdateThrottle(200, TimeUnit.MILLISECONDS, this::performUpdates);

	public AlarmAreaView(AlarmClient model)
	{
		if (model.isRunning())
            throw new IllegalStateException();

        this.model = model;
        areaFilter = new AreaFilter(level);
        model.addListener(this);
	}

	@Override
	public void itemAdded(AlarmTreeItem<?> item)
	{
		if (! areaFilter.filter(item))
			return;

		final CountDownLatch done = new CountDownLatch(1);

		Platform.runLater(() ->
        {
        	final Label label = new Label(item.getName());
        	itemViewMap.put(item, label);
	        getChildren().add(label);
	        resetGridConstraints();
            done.countDown();
        });

        try
        {
            done.await();
        }
        catch (final InterruptedException ex)
        {
            logger.log(Level.WARNING, "Alarm area update error for added item " + item.getPathName(), ex);
        }
	}

	@Override
	public void itemRemoved(AlarmTreeItem<?> item)
	{
		if (! areaFilter.filter(item))
			return;

		final CountDownLatch done = new CountDownLatch(1);

		Platform.runLater(() ->
        {
        	final Label label = itemViewMap.get(item);
        	getChildren().remove(label);
        	itemViewMap.remove(item);
        	resetGridConstraints();
        	done.countDown();
	    });

		try
        {
            done.await();
        }
        catch (final InterruptedException ex)
        {
            logger.log(Level.WARNING, "Alarm area update error for removed item " + item.getPathName(), ex);
        }

	}

	private void resetGridConstraints()
	{
		int index = 0;
		for (final Node child : getChildren())
		{
			final int columnIndex = index%col_num;
			final int rowIndex = index/col_num;

			setConstraints(child, columnIndex, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(10, 10, 10, 10));
			index++;
		}
	}

	@Override
	public void itemUpdated(AlarmTreeItem<?> item)
	{
		// TODO Auto-generated method stub
	}

	private void performUpdates()
	{
		 final AlarmTreeItem<?>[] items;
        synchronized (items_to_update)
        {

            items = items_to_update.toArray(new AlarmTreeItem<?>[items_to_update.size()]);
            items_to_update.clear();
        }

        for (final AlarmTreeItem<?> item : items)
            updateLabel(item, itemViewMap.get(item));
	}

	private void updateLabel(AlarmTreeItem<?> item, Label label)
	{
		final BasicState state = item.getState();
		final Paint color = AlarmUI.getColor(state.getSeverity());
		label.setTextFill(color);
	}

}
