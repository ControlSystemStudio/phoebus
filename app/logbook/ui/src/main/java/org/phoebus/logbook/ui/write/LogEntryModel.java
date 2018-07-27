/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

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
import org.phoebus.logbook.ui.LogbookUiPreferences;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.SimpleAuthenticationToken;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
    public static final String USERNAME_TAG = "username";
    public static final String PASSWORD_TAG = "password";

    private final LogService logService;
    private final LogFactory logFactory;

    private Node    node;
    private String  username, password;
    private Instant date;
    private String  level;
    private String  title, text;

    private final ObservableList<String> logbooks, tags, selectedLogbooks, selectedTags;
    private final ObservableList<Image>  images;
    private final ObservableList<File>   files;

    /** Property that allows the model to define when the application is in an appropriate state to submit a log entry. */
    private final ReadOnlyBooleanProperty readyToSubmitProperty; // To be broadcast through getReadyToSubmitProperty method.
    private final SimpleBooleanProperty   readyToSubmit;         // Used internally. Backs read only property above.

    /** Property that allows the model to define when the application needs to update the username and password text fields. Only used if save_credentials=true */
    private final ReadOnlyBooleanProperty updateCredentialsProperty; // To be broadcast through getUpdateCredentialsProperty.
    private final SimpleBooleanProperty   updateCredentials;         // Used internally. Backs read only property above.

    /** onSubmitAction runnable - runnable to be executed after the submit action completes. */
    private Runnable onSubmitAction;

    public LogEntryModel(final Node callingNode)
    {
        username = "";
        password = "";
        level    = "";
        title    = "";
        text     = "";

        updateCredentials = new SimpleBooleanProperty();
        updateCredentialsProperty = updateCredentials;

        logService = LogService.getInstance();

        logFactory = logService.getLogFactories().get(LogbookUiPreferences.logbook_factory);

        tags     = FXCollections.observableArrayList();
        logbooks = FXCollections.observableArrayList();

        selectedLogbooks = FXCollections.observableArrayList();
        selectedTags     = FXCollections.observableArrayList();

        images = FXCollections.observableArrayList();
        files  = FXCollections.observableArrayList();

        node = callingNode;

        readyToSubmit = new SimpleBooleanProperty(false);
        readyToSubmitProperty = readyToSubmit;

        // Set default logbooks
        // Get rid of leading and trailing whitespace and add the default to the selected list.
        for (String logbook : LogbookUiPreferences.default_logbooks)
            addSelectedLogbook(logbook.trim());
    }

    public void fetchStoredUserCredentials()
    {
        // Perform file IO on background thread.
        JobManager.schedule("Access Secure Store", monitor ->
        {
            // Get the SecureStore. Retrieve username and password.
            try
            {
                SecureStore store = new SecureStore();
                // Could be accessed from JavaFX Application Thread when updating, so synchronize.
                synchronized(username)
                {
                    String result = store.get(LogEntryModel.USERNAME_TAG);
                    username = (null == result) ? "" : result;
                }
                synchronized(password)
                {
                    String result = store.get(LogEntryModel.PASSWORD_TAG);
                    password = (null == result) ? "" : result;
                }
                // Let anyone listening know that their credentials are now out of date.
                updateCredentials.set(true);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Secure Store file not found.", ex);
            }
        });
    }

    public ReadOnlyBooleanProperty getUpdateCredentialsProperty()
    {
        return updateCredentialsProperty;
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
        // Could be accessed from background thread when updating, so synchronize.
        synchronized(this.username)
        {
            this.username = username;
        }
        checkIfReadyToSubmit();
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    /**
     * Set the password.
     * @param password
     */
    public void setPassword(final String password)
    {
        // Could be accessed from background thread when updating, so synchronize.
        synchronized(this.password)
        {
            this.password = password;
        }
        checkIfReadyToSubmit();
    }

    /**
     * Set the date.
     * @param date
     */
    public void setDate(final Instant date)
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
        checkIfReadyToSubmit();
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
     */
    public boolean addSelectedLogbook(final String logbook)
    {
        boolean result = selectedLogbooks.add(logbook);
        selectedLogbooks.sort(Comparator.naturalOrder());
        checkIfReadyToSubmit();
        return result;
    }

    /**
     * Remove a log book from the model's selected log book list.
     * @param logbook
     * @return
     */
    public boolean removeSelectedLogbook(final String logbook)
    {
        boolean result = selectedLogbooks.remove(logbook);
        checkIfReadyToSubmit();
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
     */
    public boolean addSelectedTag(final String tag)
    {
        boolean result = selectedTags.add(tag);
        selectedTags.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Removes the passed tag name from the model's selected tag list.
     * @param tag
     * @return
     */
    public boolean removeSelectedTag(final String tag)
    {
        return selectedTags.remove(tag);
    }

    public void setImages(final List<Image> images)
    {
        this.images.setAll(images);
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

    /** Check if ready to submit and update readyToSubmitProperty appropriately. */
    private void checkIfReadyToSubmit()
    {
        if (
            username.trim().isEmpty() ||
            password.trim().isEmpty() ||
            title.trim().isEmpty()    ||
            selectedLogbooks.isEmpty()
           )
        {
            readyToSubmit.set(false);
        }
        else
        {
            readyToSubmit.set(true);
        }
    }

    /** Get the ready to submit property. True when all required fields have been filled. */
    public ReadOnlyBooleanProperty getReadyToSubmitProperty()
    {
        return readyToSubmitProperty;
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
            .createdDate(date)
            .modifiedDate(date)
            .level(level);

        for (String selectedLogbook : selectedLogbooks)
            logEntryBuilder.appendToLogbook(LogbookImpl.of(selectedLogbook));
        for (String selectedTag : selectedTags)
            logEntryBuilder.appendTag(TagImpl.of(selectedTag));

        // List of temporary image files to delete.
        List<File> toDelete = new ArrayList<>();

        // Add Images
        for (Image image : images)
        {
            File imageFile = File.createTempFile("log_entry_image_", ".png");
            imageFile.deleteOnExit();
            toDelete.add(imageFile);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", imageFile);
            logEntryBuilder.attach(AttachmentImpl.of(imageFile, "image", false));
        }

        // Add Files
        for (File file : files)
        {
            logEntryBuilder.attach(AttachmentImpl.of(file, "file", false));
        }

        LogEntry logEntry = logEntryBuilder.build();

        // Submit the entry on a separate thread.
        JobManager.schedule("Submit Log Entry", monitor ->
        {
            if (LogbookUiPreferences.save_credentials)
            {
                // Get the SecureStore. Store username and password.
                try
                {
                    SecureStore store = new SecureStore();
                    store.set(USERNAME_TAG, username);
                    store.set(PASSWORD_TAG, password);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Secure Store file not found.", ex);
                }
            }

            if (null == logFactory)
            {
                logger.log(Level.WARNING, "Logbook Factory Undefined.");
            }
            else
                logFactory.getLogClient(new SimpleAuthenticationToken(username, password)).set(logEntry);

            // Delete the temporary files.
            for (File file : toDelete)
                file.delete();
            // Run the onSubmitAction runnable
            if (null != onSubmitAction)
                onSubmitAction.run();
        });

        return logEntry;
    }

    /** Fetch the available log book and tag lists on a separate thread.*/
    public void fetchLists()
    {
        JobManager.schedule("Fetch Logbooks and Tags", monitor ->
        {
            if (null == logFactory)
            {
                logger.log(Level.WARNING, "Logbook Factory Undefined.");
            }
            else
            {
                LogClient logClient = logFactory.getLogClient();

                List<Logbook> logList = new ArrayList<>();
                List<Tag> tagList = new ArrayList<>();

                logClient.listLogbooks().forEach(logbook -> logList.add(logbook));
                logClient.listTags().forEach(tag -> tagList.add(tag));

                // Certain views have listeners to these observable lists. So, when they change, the call backs need to execute on the FX Application thread.
                Platform.runLater(() ->
                {
                    logList.forEach(logbook -> logbooks.add(logbook.getName()));
                    tagList.forEach(tag -> tags.add(tag.getName()));

                    Collections.sort(logbooks);
                    Collections.sort(tags);
                });
            }
        });
    }

    /**
     * Add a change listener to the available tags list.
     * @param changeListener
     */
    public void addTagListener(ListChangeListener<String> changeListener)
    {
        tags.addListener(changeListener);
    }

    /**
     * Add a change listener to the available log books list.
     * @param changeListener
     */
    public void addLogbookListener(ListChangeListener<String> changeListener)
    {
        logbooks.addListener(changeListener);
    }

    /**
     * Set the runnable to be executed after the submit action completes.
     * <p>This runnable will be executed on another thread so everything it does should be thread safe.
     * @param runnable
     */
    public void setOnSubmitAction(Runnable runnable)
    {
        onSubmitAction = runnable;
    }
}
