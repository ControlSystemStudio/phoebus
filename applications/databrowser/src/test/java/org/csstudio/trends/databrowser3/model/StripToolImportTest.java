package org.csstudio.trends.databrowser3.model;

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Test;
import org.phoebus.util.time.TimeInterval;

public class StripToolImportTest
{
	@Test
	public void testStripToolImport() throws Exception
	{
		final Model model = new Model();
		// Load the stp file into the model using the StripToolImporter
		StripToolImporter.load(model, StripToolImportTest.class.getResourceAsStream("demo.stp"));

		final TimeInterval timeInterval = model.getTimerange().toAbsoluteInterval();
		final long span = Duration.between(timeInterval.getStart(), timeInterval.getEnd()).getSeconds();

		// Test that the timespan is correct and that the start is not absolute.
		assertEquals(333, span);
		assertEquals(false, model.getTimerange().isStartAbsolute());

		// Check the number of model items is correct. This should be the number of PVs.
		assertEquals(2, model.getItems().size());

		// Check the number of axes matches the number of model items. One axis per pv.
		assertEquals(2, model.getAxisCount());

		// For comparing doubles: assertEquals( expected, actual, acceptable delta)

		// Check the name of the PV.
		assertEquals("sim://sine", model.getItems().get(0).getName());

		// Check the range of the PV.
		assertEquals(-10.69, model.getItems().get(0).getAxis().getMin(), 0);
		assertEquals(10.33, model.getItems().get(0).getAxis().getMax(), 0);

		// Color red.
		double red = 1.0;
		double green = 0.0;
		double blue = 0.0;

		// Check the PV's color values.
		assertEquals(red, model.getItems().get(0).getPaintColor().getRed(), 0.0);
		assertEquals(green, model.getItems().get(0).getPaintColor().getGreen(), 0.0);
		assertEquals(blue, model.getItems().get(0).getPaintColor().getBlue(), 0.0);


		// Check the name of the PV.
		assertEquals("sim://cosine", model.getItems().get(1).getName());

		// Check the range of the PV.
		assertEquals(-20.5, model.getItems().get(1).getAxis().getMin(), 0);
		assertEquals(15, model.getItems().get(1).getAxis().getMax(), 0);

		// Color green.
		red = 0.0;
		green = 1.0;
		blue = 0.0;

		// Check the PV's color values.
		assertEquals(red, model.getItems().get(1).getPaintColor().getRed(), 0.0);
		assertEquals(green, model.getItems().get(1).getPaintColor().getGreen(), 0.0);
		assertEquals(blue, model.getItems().get(1).getPaintColor().getBlue(), 0.0);

	}
}
