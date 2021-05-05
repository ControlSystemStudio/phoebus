package org.phoebus.ui.selection;

import javafx.scene.Node;
import javafx.scene.image.Image;

import java.util.function.Supplier;

/**
 * A simple selection holder which can be used by most applications. This avoids the need for applications to create
 * their own selection object.
 */
public class AppSelection
{
    private final Node parent;
    private final String title;
    private final String body;
    private final Supplier<Image> image;

    private AppSelection(Node parent, String title, String body, Supplier<Image> image)
    {
        this.parent = parent;
        this.title = title;
        this.body = body;
        this.image = image;
    }

    public static AppSelection of(Node parent, String title, String body, Supplier<Image> image)
    {
        return new AppSelection(parent, title, body, image);
    }

    public Node getParent()
    {
        return parent;
    }

    public String getTitle()
    {
        return title;
    }

    public String getBody()
    {
        return body;
    }

    public Supplier<Image> getImage()
    {
        return image;
    }
}
