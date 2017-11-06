package org.csstudio.display.builder.representation.javafx;

/**
 * Generic updater interface for (JavaFX-based) {@link AutocompleteMenu}.
 * 
 * @author Amanda Carpenter
 *
 */
public interface AutocompleteMenuUpdater
{
    /**
     * Request autocomplete entries for the given content, and handle updating
     * the menu's content. The method AutocompleteMenu#setResults is intended
     * for handling these updates.
     * 
     * @param content Content for which to request autocomplete entries
     */
    public void requestEntries(String content);

    /**
     * Add the given entry into autocomplete history.
     * 
     * @param entry Entry to be added to history
     */
    public void updateHistory(String entry);
}
