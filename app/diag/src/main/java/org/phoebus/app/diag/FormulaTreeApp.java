package org.phoebus.app.diag;

import javafx.scene.image.Image;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.javafx.ImageCache;

import java.net.URI;
import java.util.logging.Logger;

public class FormulaTreeApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(FormulaTreeApp.class.getName());
    static final Image icon = ImageCache.getImage(FormulaTreeApp.class, "/icons/tree-property-16.png");
    public static final String NAME = "formula_tree";
    public static final String DISPLAYNAME = "Formula Tree";


    private static final String SUPPORTED_SCHEMA = "cf";
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAYNAME;
    }

    @Override
    public AppInstance create() {
        return new FormulaTree(this);
    }

    @Override
    public boolean canOpenResource(String resource) {
        return URI.create(resource).getScheme().equals(SUPPORTED_SCHEMA);
    }

    /**
     * Support the launching of channeltable using resource cf://?<search_string>
     * e.g.
     * -resource cf://?query=SR*
     */
    @Override
    public AppInstance create(URI resource) {
        return new FormulaTree(this);
    }
}
