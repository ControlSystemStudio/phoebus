package org.csstudio.apputil.formula.areadetector;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.Arrays;
import java.util.List;

/**
 * A formula function for fetching the horizontal profile from image data.
 * This function extracts the horizontal profile along a given y position.
 *
 * @author Kunal Shroff
 */
public class ImageDataHorizontalProfileFunction implements FormulaFunction {

    @Override
    public String getCategory() {
        return "areaDetector";
    }

    @Override
    public String getName() {
        return "imageDataHorizontalProfile";
    }

    @Override
    public String getDescription() {
        return "Fetch the horizontal profile data for the given Image data at a specific y position.";
    }

    @Override
    public List<String> getArguments() {
        return List.of("image", "image width", "y position");
    }

    /**
     * Computes the horizontal profile of the given image data at the specified y position.
     *
     * @param imageData  The image data.
     * @param imageWidth The width of the image.
     * @param yPosition  The y position in the image data from which to extract the horizontal profile.
     * @return A VNumberArray representing the extracted horizontal profile.
     */
    protected VType getHorizontalProfile(final VNumberArray imageData, final int imageWidth, final int yPosition) {
        int start = yPosition * imageWidth;
        int end = start + imageWidth;
        return VNumberArray.of(imageData.getData().subList(start, end),
                Alarm.none(),
                imageData.getTime(),
                Display.none());
    }

    /**
     * Computes the horizontal profile based on the provided arguments.
     *
     * @param args The arguments, where:
     *             - args[0] must be a numeric array
     *             - args[1] must be the image width
     *             - args[2] must be the y position
     * @return The computed horizontal profile as a VType.
     * @throws Exception If invalid arguments are provided.
     */
    @Override
    public VType compute(final VType... args) throws Exception {
        if (args.length != 3) {
            throw new Exception("Function " + getName() +
                    " requires 3 arguments but received " + Arrays.toString(args));
        }
        if (!(VTypeHelper.isNumericArray(args[0])))
            throw new Exception("Function " + getName() +
                    " takes Numeric Array but received " + Arrays.toString(args));

        int width = (int) Math.round(VTypeHelper.toDouble(args[1]));
        int yPosition = (int) Math.round(VTypeHelper.toDouble(args[2]));
        return getHorizontalProfile((VNumberArray) args[0], width, yPosition);
    }
}