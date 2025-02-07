package org.csstudio.apputil.formula.areadetector;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.VImage;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.Arrays;
import java.util.List;

/**
 * A formula function for fetching the vertical profile for a VImage.
 * The vertical profile is extracted along a given x position.
 * This is useful for analyzing the intensity distribution along a column in the image.
 *
 *  @author Kunal Shroff
 */
public class ImageVerticalProfileFunction implements FormulaFunction
{

    @Override
    public String getCategory()
    {
        return "areaDetector";
    }

    @Override
    public String getName()
    {
        return "imageVerticalProfile";
    }

    @Override
    public String getDescription()
    {
        return "Fetch the vertical profile data for the given Image and x position.";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("image", "x position");
    }

    /**
     * Computes the vertical profile of the given VImage at the specified x position.
     *
     * @param image     The input image.
     * @param xPosition The x position in the image from which to extract the vertical profile.
     * @return A VNumberArray representing the extracted vertical profile.
     */
    protected VType getVerticalProfile(final VImage image, final int xPosition) {
        return VNumberArray.of(sampleWithStride(image.getData(), image.getWidth(), xPosition), Alarm.none(), image.getTime(), Display.none());
    }

    private static ListDouble sampleWithStride(final ListNumber data, final double stride, final double offset) {

        return new ListDouble() {

            @Override
            public double getDouble(int index) {
                int computedIndex = (int) Math.round(offset + index * stride);
                if (computedIndex < 0 || computedIndex >= data.size()) {
                    throw new IndexOutOfBoundsException("Computed index " + computedIndex + " is out of range.");
                }
                return data.getDouble(computedIndex);
            }

            @Override
            public int size() {
                int size = (int) Math.ceil((data.size() - offset) / stride);
                return Math.max(size, 0);
            }
        };
    }

    /**
     * Computes the vertical profile based on the provided arguments.
     *
     * @param args The arguments, where:
     *             - args[0] must be a VImage
     *             - args[1] must be the x position
     * @return The computed vertical profile as a VType.
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

        int xPosition = (int) Math.round(VTypeHelper.toDouble(args[1]));
        return getVerticalProfile((VImage) args[0], xPosition);
    }
}