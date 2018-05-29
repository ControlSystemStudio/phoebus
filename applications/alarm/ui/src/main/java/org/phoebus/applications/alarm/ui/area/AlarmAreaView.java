package org.phoebus.applications.alarm.ui.area;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class AlarmAreaView extends GridPane implements AlarmClientListener
{
	@SuppressWarnings("unused")
	private final AlarmClient model;
	private final AreaFilter areaFilter;
	private final int level = 2;
	private final int col_num = 2;

	private final AtomicInteger grid_index = new AtomicInteger(0);

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
        	synchronized (grid_index)
        	{
	        	// 					  Column 			  		Row
	            setConstraints(label, grid_index.get()%col_num, grid_index.get()/col_num);
	            getChildren().add(label);
	            grid_index.getAndIncrement();
        	}
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
		// TODO Auto-generated method stub
	}

	@Override
	public void itemUpdated(AlarmTreeItem<?> item)
	{
		// TODO Auto-generated method stub
	}

}
