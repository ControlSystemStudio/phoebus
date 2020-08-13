package org.phoebus.applications.email;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javafx.scene.image.Image;

/**
 * An instance of the email entry.
 * 
 * @author Kunal Shroff
 */
public class EmailEntry {

    private String title;
    private String body;
    private List<Image> images = Collections.emptyList();
    private List<File> files = Collections.emptyList();

    public EmailEntry()
    {
    }

    public EmailEntry(String title, String body, List<Image> images)
    {
        super();
        this.title = title;
        this.body = body;
        this.images = images;
    }

    public EmailEntry(String title, String body, List<Image> images, List<File> files)
    {
        super();
        this.title = title;
        this.body = body;
        this.images = images;
        this.files = files;
    }


    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public List<Image> getImages()
    {
        return images;
    }

    public void setImages(List<Image> images)
    {
        this.images = images;
    }

    public List<File> getFiles()
    {
        return files;
    }

    public void setFiles(List<File> files)
    {
        this.files = files;
    }
}
