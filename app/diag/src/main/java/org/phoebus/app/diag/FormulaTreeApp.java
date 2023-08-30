package org.phoebus.app.diag;

import javafx.scene.image.Image;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.javafx.ImageCache;

public class FormulaTreeApp implements AppDescriptor {
    static final Image icon = ImageCache.getImage(FormulaTreeApp.class, "/icons/tree-property-16.png");
    public static final String NAME = "formula_tree";
    public static final String DISPLAYNAME = "Formula Tree";
    
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
}
