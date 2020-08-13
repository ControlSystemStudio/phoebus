/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.PictureWidget;
import org.csstudio.display.builder.representation.javafx.SVGHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Creates JavaFX item for model widget.
 *
 * SVG resources are supported, but in contrast to formats like png, jpg and gif, SVGs are
 * @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class PictureRepresentation extends JFXBaseRepresentation<ImageView, PictureWidget>
{
    /** Change the image file */
    private final DirtyFlag dirty_content = new DirtyFlag();
    /** Change the image size, rotation or preserve_ratio */
    private final DirtyFlag dirty_style = new DirtyFlag();
    private final UntypedWidgetPropertyListener styleChangedListener = this::styleChanged;
    private final WidgetPropertyListener<String> contentChangedListener = this::contentChanged;

    private volatile Image img_loaded;
    private volatile String img_path;
    private volatile double native_ratio = 1.0;

    private volatile Rotate rotation = new Rotate(0);
    private volatile Translate translate = new Translate(0,0);

    @Override
    public ImageView createJFXNode() throws Exception
    {
        final ImageView iv = new ImageView();
        iv.setSmooth(true);
        iv.getTransforms().addAll(translate, rotation);
        return iv;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(styleChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(styleChangedListener);

        model_widget.propStretch().addUntypedPropertyListener(styleChangedListener);
        model_widget.propRotation().addUntypedPropertyListener(styleChangedListener);
//      styleChanged() will be called by contentChanged()

        // This is one of those weird cases where getValue calls setValue and fires the listener.
        // So register listener after getValue called
        final String img_name = model_widget.propFile().getValue();
        model_widget.propFile().addPropertyListener(contentChangedListener);
        /*
         * Must clear the flags, JFXBaseRepresentation will call updateChanges() but we are not initialized yet
         * (contentChanged() will set the flags after it initializes img_path and img_loaded)
         */
        dirty_style.checkAndClear();
        dirty_content.checkAndClear();
        ModelThreadPool.getExecutor().execute(() -> contentChanged(null, null, img_name));
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(styleChangedListener);
        model_widget.propHeight().removePropertyListener(styleChangedListener);
        model_widget.propStretch().removePropertyListener(styleChangedListener);
        model_widget.propRotation().removePropertyListener(styleChangedListener);
        model_widget.propFile().removePropertyListener(contentChangedListener);
        super.unregisterListeners();
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    private void contentChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
    {
        // Imagine if updateChanges executes here. Mark is cleared and image updated before new image loaded.
        // Subsequent Scheduled image update would not happen.

        boolean load_failed = false;

        try
        {
            img_path = resolveImageFile(new_value);
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Failure resolving image path from base path: " + new_value, e);
            load_failed = true;
        }

        if (!load_failed)
        {
            if (toolkit.isEditMode()){
                ImageCache.remove(img_path);
            }

            // Load the SVG without specifying a size so that img_loaded will have the original aspect ratio that we need
            if (img_path.toLowerCase().endsWith("svg"))
                img_loaded = SVGHelper.loadSVG(img_path, 0.0, 0.0);
            else
            {
                img_loaded = ImageCache.cache(img_path, () ->
                {
                    try
                    {
                        // Open the image from the stream created from the resource file
                        return new Image(ModelResourceUtil.openResourceStream(img_path));
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Failure loading image file:" + img_path, ex);
                    }
                    return null;
                });
            }
            if (img_loaded == null) {
                load_failed = !loadDefaultImage();
            }
        }

        if (!load_failed)
            native_ratio = img_loaded.getWidth() / img_loaded.getHeight();

        // Resize/reorient in case we are preserving aspect ratio and changed native_ratio
        dirty_style.mark();
        // Switch to the new image
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_content.checkAndClear())
        {
            if (img_loaded != null)
            {
                jfx_node.setImage(img_loaded);
                // We handle ratio internally, do not let ImageView do that
                jfx_node.setPreserveRatio(false);
            }
            jfx_node.setCache(true);
        }
        if (dirty_style.checkAndClear())
        {
            double widg_w = model_widget.propWidth().getValue().doubleValue();
            double widg_h = model_widget.propHeight().getValue().doubleValue();
            double pic_w = widg_w;
            double pic_h = widg_h;

            // preserve aspect ratio
            if (!model_widget.propStretch().getValue())
            {
                double w_prime = pic_h * native_ratio;
                double h_prime = pic_w / native_ratio;
                if (w_prime < pic_w)
                    pic_h = h_prime;
                else if (h_prime < pic_h)
                    pic_w = w_prime;
            }

            double final_pic_w = pic_w;
            double final_pic_h = pic_h;
            double cos_a = Math.cos(Math.toRadians(model_widget.propRotation().getValue()));
            double sin_a = Math.sin(Math.toRadians(model_widget.propRotation().getValue()));
            double pic_bb_w = pic_w * Math.abs(cos_a) + pic_h * Math.abs(sin_a);
            double pic_bb_h = pic_w * Math.abs(sin_a) + pic_h * Math.abs(cos_a);
            double scale_fac = Math.min(widg_w / pic_bb_w, widg_h / pic_bb_h);

            if (scale_fac < 1.0)
            {
                final_pic_w = (int) Math.floor(scale_fac * pic_w);
                final_pic_h = (int) Math.floor(scale_fac * pic_h);
            }

            // This actually loads SVG images.
            if(img_path != null && img_path.toLowerCase().endsWith("svg")) {
                loadSVG(img_path, final_pic_w, final_pic_h);
                jfx_node.setImage(img_loaded);
            }

            jfx_node.setFitHeight(final_pic_h);
            jfx_node.setFitWidth(final_pic_w);

            // Rotate around the center of the resized image
            rotation.setAngle(model_widget.propRotation().getValue());
            rotation.setPivotX(final_pic_w / 2.0);
            rotation.setPivotY(final_pic_h / 2.0);

            // translate to the center of the widget
            translate.setX((widg_w - final_pic_w) / 2.0);
            translate.setY((widg_h - final_pic_h) / 2.0);
        }
    }

    /**
     * Loads a SVG resource. The image cache is used, but the key to the SVG resource depends
     * on the width and height of the image. Reason is that when resizing a image the underlying
     * SVG must be transcoded again with the new size.
     * If the wanted SVG image is not found, the default (PNG) image is loaded instead.
     * @param width
     * @param height
     * @return An {@link Image} or <code>null</code>.
     */
    private void loadSVG(String fileName, double width, double height){

        String imageFileName = resolveImageFile(fileName);
        img_loaded = SVGHelper.loadSVG(imageFileName, width, height);
        if(img_loaded == null){
            loadDefaultImage();
        }
    }

    private String resolveImageFile (String imageFileName ) {

        try {

            String expandedFileName = MacroHandler.replace(model_widget.getMacrosOrProperties(), imageFileName);

            // Resolve new image file relative to the source widget model (not 'top'!).
            // Get the display model from the widget tied to this representation.
            final DisplayModel widgetModel = model_widget.getDisplayModel();

            // Resolve the image path using the parent model file path.
            return ModelResourceUtil.resolveResource(widgetModel, expandedFileName);

        } catch ( Exception ex ) {

            logger.log(Level.WARNING, String.format("Failure resolving image path: %s", imageFileName), ex);

            return null;
        }
    }

    /**
     * Convenience method to load default image if the wanted image resource cannot be found.
     * Note that this method sets the instance variable <code>img_loaded</code>.
     * @return <code>true</code> if default image loaded successfully, otherwise <code>false</code>
     */
    private boolean loadDefaultImage()
    {
        final String dflt_img = PictureWidget.default_pic;
        img_loaded = ImageCache.cache(dflt_img, () ->
        {
            try
            {
                // Open the image from the stream created from the resource file
                return new Image(ModelResourceUtil.openResourceStream(dflt_img));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failure loading default image file:" + dflt_img, ex);
            }
            return null;
            });

        return img_loaded != null;
    }
}
