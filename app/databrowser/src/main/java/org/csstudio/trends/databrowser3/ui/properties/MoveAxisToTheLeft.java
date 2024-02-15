package org.csstudio.trends.databrowser3.ui.properties;

import javafx.scene.control.MenuItem;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import java.util.stream.Collectors;

/** Action to move an axis to the left
 */
@SuppressWarnings("nls")
public class MoveAxisToTheLeft extends MenuItem
{
    /** @param model Model
     *  @param undoableActionManager Undo manager
     *  @param axisToMoveToTheLeft_index Index of axis to move to the left
     */
    public MoveAxisToTheLeft(Model model,
                             UndoableActionManager undoableActionManager,
                             int axisToMoveToTheLeft_index)
    {
        super("Move Axis to the Left", Activator.getIcon("left"));

        var axisToMoveToTheLeft = model.getAxes().get(axisToMoveToTheLeft_index);

        int axisIndex1, axisIndex2;

        var allAxes = model.getAxes();

        if (!axisToMoveToTheLeft.isOnRight()) {
            // Axis to move to the left is located on the left side of the graph
            var allAxesOnTheLeft = allAxes.stream().filter(axis -> !axis.isOnRight()).collect(Collectors.toList());
            int axisToMoveToTheLeft_index_allAxesOnTheLeft = allAxesOnTheLeft.indexOf(axisToMoveToTheLeft);
            if (axisToMoveToTheLeft_index_allAxesOnTheLeft > 0) {
                var axisOnTheLeft = allAxesOnTheLeft.get(axisToMoveToTheLeft_index_allAxesOnTheLeft - 1);
                axisIndex1 = allAxes.indexOf(axisOnTheLeft);
                axisIndex2 = axisToMoveToTheLeft_index;
                UndoableAction moveAxisToTheLeft = new ExchangeAxesUndoableAction("Move Axis to the Left",
                                                                                  model,
                                                                                  axisIndex1,
                                                                                  axisIndex2);
                setOnAction(actionEvent -> undoableActionManager.execute(moveAxisToTheLeft));
            }
        }
        else {
            // Axis to move to the left is located on the right side of the graph
            var allAxesOnTheRight = allAxes.stream().filter(axis -> axis.isOnRight()).collect(Collectors.toList());
            int axisToMoveToTheLeft_index_allAxesOnTheRight = allAxesOnTheRight.indexOf(axisToMoveToTheLeft);
            if (axisToMoveToTheLeft_index_allAxesOnTheRight < allAxesOnTheRight.size() - 1) {
                var axisOnTheLeft = allAxesOnTheRight.get(axisToMoveToTheLeft_index_allAxesOnTheRight + 1); // On the right side of the graph, axes are stored in "opposite" relative ordering
                axisIndex1 = axisToMoveToTheLeft_index;
                axisIndex2 = allAxes.indexOf(axisOnTheLeft);
                UndoableAction moveAxisToTheLeft = new ExchangeAxesUndoableAction("Move Axis to the Left",
                                                                                  model,
                                                                                  axisIndex1,
                                                                                  axisIndex2);
                setOnAction(actionEvent -> undoableActionManager.execute(moveAxisToTheLeft));
            }
        }
    }
}
