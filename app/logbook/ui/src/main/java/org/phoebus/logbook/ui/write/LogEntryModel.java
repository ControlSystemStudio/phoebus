/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.LogFactory;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookImpl;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;
import org.phoebus.security.tokens.SimpleAuthenticationToken;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;

/**
 * Purveyor of log entry application state.
 * <p> Provides methods to set log entry data and to submit log entries.
 * @author Evan Smith
 */
public class LogEntryModel
{
    private final LogService logService;
    private final LogFactory logFactory;
    
    private Node   node;
    private String username, password;
    private String date, level;
    private String title, text;
    
    private final ObservableList<String> logbooks, tags, selectedLogbooks, selectedTags;
    private final ObservableList<Image>  images;
    private final ObservableList<File>   files;
    
    /** onSubmitAction runnable - runnable to executed after the submit action completes.*/
    private Runnable onSubmitAction;
    
    public LogEntryModel(final Node callingNode)
    {   
        logService = LogService.getInstance();
        logFactory = logService.getLogFactories().get("org.phoebus.sns.logbook");
        tags     = FXCollections.observableArrayList();
        logbooks = FXCollections.observableArrayList();
 
        selectedLogbooks = FXCollections.observableArrayList();
        selectedTags     = FXCollections.observableArrayList();

        images = FXCollections.observableArrayList();
        files  = FXCollections.observableArrayList();
        
        node = callingNode;
    }

    /**
     * Gets the JavaFX Scene graph.
     * @return Scene
     */
    public Scene getScene()
    {
        return node.getScene();
    }

    /**
     * Set the user name.
     * @param username
     */
    public void setUser(final String username)
    {
        this.username = username;
    }
    
    /**
     * Set the password.
     * @param password
     */
    public void setPassword(final String password)
    {
        this.password = password;
    }
    
    /**
     * Set the date.
     * @param date
     */
    public void setDate(final String date)
    {
        this.date = date;
    }
    
    /**
     * Set the level.
     * @param level
     */
    public void setLevel(final String level)
    {
        this.level = level;
    }
   
    /**
     * Get the text.
     * @param text
     */
    public String getTitle()
    {
        return title;
    }
    
    /**
     * Set the title.
     * @param title
     */
    public void setTitle(final String title)
    {
        this.title = title;
    }
    
    /**
     * Get the text.
     * @param text
     */
    public String getText()
    {
        return text;
    }
    
    /**
     * Set the text.
     * @param text
     */
    public void setText(final String text)
    {
        this.text = text;
    }
    
    /**
     * Get an unmodifiable list of the log books.
     * @return
     */
    public ObservableList<String> getLogbooks()
    {
        return FXCollections.unmodifiableObservableList(logbooks);
    }
    
    /**
     * Get an unmodifiable list of the selected log books.
     * @return
     */
    public ObservableList<String> getSelectedLogbooks()
    {
        return FXCollections.unmodifiableObservableList(selectedLogbooks);
    }
    
    /**
     * Tests whether the model's log book list contains the passed log book name.
     * @param logbook
     * @return
     */
    public boolean hasLogbook (final String logbook)
    {
        return logbooks.contains(logbook);
    }
    
    /**
     * Tests whether the model's selected log book list contains the passed log book name.
     * @param logbook
     * @return
     */
    public boolean hasSelectedLogbook (final String logbook)
    {
        return selectedLogbooks.contains(logbook);
    }
    
    /**
     * Add a log book to the model's selected log books list.
     * @param logbook
     * @return
     * @throws Exception If the selected log book does not exist.
     */
    public boolean addSelectedLogbook(final String logbook) throws Exception
    {
        if (! logbooks.contains(logbook))
            throw new Exception("The selected logbook '" + logbook + "' does not exist.");
        boolean result = selectedLogbooks.add(logbook);
        selectedLogbooks.sort(Comparator.naturalOrder());
        return result;
    }
    
    /**
     * Remove a log book from the model's selected log book list.
     * @param logbook
     * @return
     * @throws Exception If the selected log book does not exist.
     */
    public boolean removeSelectedLogbook(final String logbook) throws Exception
    {
        if (! logbooks.contains(logbook))
            throw new Exception("The selected logbook '" + logbook + "' does not exist.");
        boolean result = selectedLogbooks.remove(logbook);
        return result;    
    }
    
    /**
     * Get an unmodifiable list of the tags.
     * @return
     */
    public ObservableList<String> getTags()
    {
        return FXCollections.unmodifiableObservableList(tags);
    }
    
    /**
     * Get an unmodifiable list of the selected tags.
     * @return
     */
    public ObservableList<String> getSelectedTags()
    {
        return FXCollections.unmodifiableObservableList(selectedTags);
    }
    
    /**
     * Tests whether the model's tag list contains the passed tag name.
     * @param tag
     * @return
     */
    public boolean hasTag (final String tag) 
    {
        return tags.contains(tag);
    }
    
    /**
     * Tests whether the model's selected tag list contains the passed tag name.
     * @param tag
     * @return
     */
    public boolean hasSelectedTag (final String tag) 
    {
        return selectedTags.contains(tag);
    }
    
    /**
     * Adds the passed tag name to the model's selected tag list.
     * @param tag
     * @return
     * @throws Exception If the selected tag does not exist.
     */
    public boolean addSelectedTag(final String tag) throws Exception
    {
        if (! tags.contains(tag))
            throw new Exception("The selected tag '" + tag + "' does not exist.");
        boolean result = selectedTags.add(tag);
        selectedTags.sort(Comparator.naturalOrder());
        return result;    
    }
    
    /**
     * Removes the passed tag name from the model's selected tag list.
     * @param tag
     * @return
     * @throws Exception Exception If the selected tag does not exist.
     */
    public boolean removeSelectedTag(final String tag) throws Exception
    {
        if (! tags.contains(tag))
            throw new Exception("The selected tag '" + tag + "' does not exist.");
        boolean result = selectedTags.remove(tag);
        return result;        
    }
    
    /**
     * Return an unmodifiable list of the model's images.
     * @return
     */
    public ObservableList<Image> getImages()
    {
        return FXCollections.unmodifiableObservableList(images);
    }
    
    /**
     * Add an image to the model's list of images.
     * @param image
     * @return
     */
    public boolean addImage(final Image image)
    {
        if (null != image)
            return images.add(image);
        return false;    
    }
    
    /**
     * Remove an image from the model's list of images.
     * @param image
     * @return
     */
    public boolean removeImage(final Image image)
    {
        if (null != image)
            return images.remove(image);
        return false;
    }
    
    /**
     * Add a listener to the images list.
     * @param listChangeListener
     */
    public void addImagesListener(ListChangeListener<Image> listChangeListener)
    {
        images.addListener(listChangeListener);
    }
    
    /**
     * Return an unmodifiable list of the model's files.
     * @return
     */
    public ObservableList<File> getFiles()
    {
        return FXCollections.unmodifiableObservableList(files);
    }
    
    /**
     * Add a file to the model's list of files.
     * @param file
     * @return
     */
    public boolean addFile(final File file)
    {
        return files.add(file);
    }
    
    /** 
     * Remove a file form the model's list of files.
     * @param file
     * @return
     */
    public boolean removeFile(final File file)
    {
        return files.remove(file);
    }
    
    /**
     * Create and return a log entry with the current data in the log entry form.
     * @throws IOException 
     */
    public LogEntry submitEntry() throws IOException
    {    
        // Create a log entry with the form data.
        LogEntryBuilder logEntryBuilder = new LogEntryBuilder();
        logEntryBuilder.title(title)
            .description(text)
            .createdDate(Instant.parse(date))
            .modifiedDate(Instant.parse(date))
            .level(level);
        
        for (String selectedLogbook : selectedLogbooks)
            logEntryBuilder.appendToLogbook(LogbookImpl.of(selectedLogbook));
        for (String selectedTag : selectedTags)
            logEntryBuilder.appendTag(TagImpl.of(selectedTag));
        
        // List of temporary image files to delete.
        List<File> toDelete = new ArrayList<File>();
        
        // Add Images
        for (Image image : images)
        {
            File imageFile = File.createTempFile("log_entry_image", null);
            toDelete.add(imageFile);
            imageFile.deleteOnExit();
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", imageFile);
            logEntryBuilder.attach(AttachmentImpl.of(imageFile));
        }
        
        // Add Files
        for (File file : files)
        {
            logEntryBuilder.attach(AttachmentImpl.of(file));
        }
        
        LogEntry logEntry = logEntryBuilder.build();

        JobManager.schedule("Submit Log Entry", monitor ->
        {
            LogClient client = logFactory.getLogClient(new SimpleAuthenticationToken(username, password));
            client.set(logEntry);
            
            // Delete the temporary files.
            for (File file : toDelete)
                file.delete();
            // Run the onSubmitAction runnable
            if (null != onSubmitAction)
                onSubmitAction.run();
        });
        
        return logEntry;
    }

    /** Fetch the log book and tag lists on a separate thread. */
    public void fetchLists()
    {
        JobManager.schedule("Fetch Logbooks and Tags", monitor ->
        {
            LogClient initClient = logFactory.getLogClient();
            Collection<Logbook> logList = initClient.listLogbooks();
            Collection<Tag> tagList = initClient.listTags();
            Platform.runLater(() ->
            {
                logList.forEach(logbook -> logbooks.add(logbook.getName()));
                tagList.forEach(tag -> tags.add(tag.getName()));
            });
        });
    }

    public void addTagListener(ListChangeListener<String> changeListener)
    {
        tags.addListener(changeListener);
    }

    public void addLogbookListener(ListChangeListener<String> changeListener)
    {
        logbooks.addListener(changeListener);
    }

    public void setOnSubmitAction(Runnable runnable)
    {
        onSubmitAction = runnable;
    }
}
