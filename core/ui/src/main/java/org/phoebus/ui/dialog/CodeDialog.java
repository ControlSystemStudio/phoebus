package org.phoebus.ui.dialog;


import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Dialog for entering multi-line code
 *
 *  <p>Can also be used to just display code,
 *  allowing to 'copy' the text, but no changes.
 *
 *  @author Dennis Hilhorst
 */
public class CodeDialog extends MultiLineInputDialog {

    public CodeDialog(String initial_text) {
        super(initial_text);
        setupEditor();
    }

    public CodeDialog(Node parent, String initial_text) {
        super(parent, initial_text);
        setupEditor();
    }

    public CodeDialog(Node parent, String initial_text, boolean editable) {
        super(parent, initial_text, editable);
        setupEditor();
    }

    private void setupEditor() {
        setStyling();
        setTabToSpaces();
    }

    private void setTabToSpaces() {
        text.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume();
                int caretPosition = text.getCaretPosition();

                if (event.isShiftDown()) {
                    if (caretPosition >= 4) {
                        String textBeforeCaret = text.getText(caretPosition - 4, caretPosition);
                        if ("    ".equals(textBeforeCaret)) {
                            // remove last 'tab'
                            text.deleteText(caretPosition - 4, caretPosition);
                            return;
                        }
                    }

                    // Move caret back by the specified number of spaces, but not past the start of the line
                    int lineStart = text.getText(0, caretPosition).lastIndexOf('\n') + 1;
                    text.positionCaret(Math.max(lineStart, caretPosition - 4));
                }
                else {
                    text.insertText(caretPosition, "    ");
                }
            }
        });
    }

    private void setStyling() {
        text.getStyleClass().add("code-dialog");  // in opieditor.css
    }
}
