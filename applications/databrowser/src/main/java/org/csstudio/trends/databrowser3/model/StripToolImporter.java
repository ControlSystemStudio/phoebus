package org.csstudio.trends.databrowser3.model;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.phoebus.util.time.TimeRelativeInterval;

import javafx.scene.paint.Color;

public class StripToolImporter
{

	public static void load(final Model model, final InputStream stream) throws Exception
	{
		// TODO Auto-generated method stub
		final BufferedReader buffReader = new BufferedReader(new InputStreamReader(stream));
		String line = null;
		final ArrayList<String> lines = new ArrayList<>();
		while (null != (line = buffReader.readLine()))
		{
			lines.add(line);
		}

		final ArrayList<PVItem> pvItems = new ArrayList<>();
		final ArrayList<String[]> colors = new ArrayList<>();
		for (final String str : lines)
		{
			//TODO Handle lines with no data following ID strings.

			// Split the line by spaces. This will separate the identifier from the following data.
			final String[] tokens = str.split("\\s+");
			if (0 != tokens.length)
			{
				final String id = tokens[0];

				// Consume empty lines
				if (0 == id.compareTo(""))
				{
					continue;
				}

				// Split the string by periods. Regex uses "." so use quote method to escape the regex and get the desired full stop.
				final String[] idTok = id.split(Pattern.quote("."));

				// Skip lines with an improperly formatted identifier string.
				final String strip = idTok[0];
				if (1 == strip.compareTo("Strip"))
				{
					continue;
				}

				//Identify the 'type'.
				final String type = idTok[1].trim();

				if (0 == type.compareTo("Color"))
				{

					final String colorID = idTok[2].trim();

					if (colorID.matches("Color\\d(.*)?"))
					{
						// Add the color values to an array list for later processing.
						colors.add(Arrays.copyOfRange(tokens, 1, tokens.length));
					}
				}
				else if (0 == type.compareTo("Curve"))
				{
					// If the the type is a Curve check if it is in the pvItems array list.
					final String indexStr = idTok[2].trim();

					final Integer index = Integer.parseInt(indexStr);
					if (pvItems.size() < index + 1)
					{
						// If it is not in the array list, add a PVItem to the pvItems array list.
						pvItems.add(new PVItem("unknown", 0));
						model.addAxis();
						final double red = Double.parseDouble(colors.get(index)[0]);
						final double green = Double.parseDouble(colors.get(index)[1]);
						final double blue = Double.parseDouble(colors.get(index)[2]);

						// Colors are stored as unsigned shorts with max values of 65535.
						// Convert to doubles in range [0.0, 1.0] by dividing by max value.
						final double new_red = red/65535;
						final double new_green = green/65535;
						final double new_blue = blue/65535;

						// Instantiate a new color with opacity as 1.0.
						final Color new_color = new Color(new_red, new_green, new_blue, 1.0);
						final PVItem newPV = pvItems.get(index);
						newPV.setColor(new_color);
					}

					final PVItem pvItem = pvItems.get(index);

					// Check the subtype of the ID.
					final String subtype = idTok[3].trim();
					if (0 == subtype.compareTo("Name"))
					{
						// Add the name to the corresponding PVItem.
						final String name = tokens[1].trim();
						pvItem.setName(name);
					}
					else if (0 == subtype.compareTo("Min"))
					{
						// Retrieve the axis that corresponds to the current PVItem.
						final AxisConfig axis = model.getAxes().get(index);

						final double min = Double.parseDouble(tokens[1].trim());
						final double max = axis.getMax();

						// Set the range and set the axis.
						axis.setRange(min, max);
						pvItem.setAxis(axis);
					}
					else if (0 == subtype.compareTo("Max"))
					{
						// Retrieve the axis that corresponds to the current PVItem.
						final AxisConfig axis = model.getAxes().get(index);

						final double max = Double.parseDouble(tokens[1].trim());
						final double min = axis.getMin();

						// Set the range and set the axis.
						axis.setRange(min, max);
						pvItem.setAxis(axis);
					}
				}
				else if (0 == type.compareTo("Time"))
				{
					// Process time data.
					final String timeType = idTok[2].trim();
					if (0 == timeType.compareTo("Timespan"))
					{
						// Parse the timespan.
						final long timespan = Integer.parseInt(tokens[1].trim());

						// Save the timespan as a relative start in a relative interval.
						final TimeRelativeInterval timerange = TimeRelativeInterval.of(Duration.ofSeconds(timespan), Duration.ZERO);
						model.setTimerange(timerange);
					}
				}
			}
		}

		for (final PVItem item : pvItems)
		{
			model.addItem(item);
		}
	}

}
