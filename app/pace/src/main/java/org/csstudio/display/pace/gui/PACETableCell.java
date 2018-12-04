package org.csstudio.display.pace.gui;

import org.csstudio.display.pace.model.Cell;
import org.csstudio.display.pace.model.Instance;

import javafx.geometry.Insets;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.converter.DefaultStringConverter;

public class PACETableCell extends TextFieldTableCell<Instance, String>
{
    private static final Background EDITED = new Background(new BackgroundFill(Color.YELLOW, CornerRadii.EMPTY, Insets.EMPTY));
    private static final Background READ_ONLY = new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY));

    public PACETableCell()
    {
        super(new DefaultStringConverter());
    }

    @Override
    public void updateItem(final String item, final boolean empty)
    {
        super.updateItem(item, empty);

        if (empty  ||  item == null)
        {
            setBackground(null);
        }
        else
        {
            final int col = getTableView().getColumns().indexOf(getTableColumn()) - 1;
            final Instance instance = getTableRow().getItem();
            final Cell cell = instance.getCell(col);
            if (cell.isReadOnly())
                setBackground(READ_ONLY);
            else if (cell.isEdited())
                setBackground(EDITED);
            else
                setBackground(null);
        }
    }
}
