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

    private String subject;
    private String body;
    private List<Image> images = Collections.emptyList();
    private List<File> files = Collections.emptyList();

    public EmailEntry()
    {
    }

    public EmailEntry(String subject, String body, List<Image> images)
    {
        super();
        this.subject = subject;
        this.body = body;
        this.images = images;
    }

    public EmailEntry(String subject, String body, List<Image> images, List<File> files)
    {
        super();
        this.subject = subject;
        this.body = body;
        this.images = images;
        this.files = files;
    }


    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
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
