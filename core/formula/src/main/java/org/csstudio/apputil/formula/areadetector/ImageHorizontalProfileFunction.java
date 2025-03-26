package org.csstudio.apputil.formula.areadetector;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.VImage;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.Arrays;
import java.util.List;

/** A formula function for fetching the horizontal profile for a VImage.
 *  @author Kunal Shroff
 */
public class ImageHorizontalProfileFunction implements FormulaFunction
{

    @Override
    public String getCategory()
    {
        return "areaDetector";
    }

    @Override
    public String getName()
    {
        return "imageHorizontalProfile";
    }

    @Override
    public String getDescription()
    {
        return "Fetch the horizontal profile data for the given Image and y position.";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("image", "y position");
    }

    protected VType getHorizontalProfile(final VImage image, final int yPosition)
    {
        int start = yPosition * image.getWidth();
        int end = start + image.getWidth();
        return VNumberArray.of(image.getData().subList(start, end), Alarm.none(), image.getTime(), Display.none());
    }

    /**
     * Computes the horizontal profile based on the provided arguments.
     *
     * @param args The arguments, where:
     *             - args[0] must be a VImage
     *             - args[1] must be the y position
     * @return The computed horizontal profile as a VType.
     * @throws Exception If invalid arguments are provided.
     */
    @Override
    public VType compute(final VType... args) throws Exception
    {
        if (args.length != 2) {
            throw new Exception("Function " + getName() +
                    " requires 2 arguments but received " + Arrays.toString(args));
        }
        if (!(args[0] instanceof VImage))
            throw new Exception("Function " + getName() +
                    " takes VImage but received " + Arrays.toString(args));

        int yPosition = (int) Math.round(VTypeHelper.toDouble(args[1]));
        return getHorizontalProfile((VImage) args[0], yPosition);
    }
}