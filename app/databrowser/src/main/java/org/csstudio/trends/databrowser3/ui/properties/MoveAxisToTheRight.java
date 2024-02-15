package org.csstudio.trends.databrowser3.ui.properties;

import javafx.scene.control.MenuItem;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import java.util.stream.Collectors;

/** Action to move an axis to the right
 */
@SuppressWarnings("nls")
public class MoveAxisToTheRight extends MenuItem
{
    /** @param model Model
     *  @param undoableActionManager Undo manager
     *  @param axisToMoveToTheRight_index Index of axis to move to the right
     */
    public MoveAxisToTheRight(Model model,
                             UndoableActionManager undoableActionManager,
                             int axisToMoveToTheRight_index)
    {
        super("Move Axis to the Right", Activator.getIcon("right"));

        var axisToMoveToTheRight = model.getAxes().get(axisToMoveToTheRight_index);

        int axisIndex1, axisIndex2;

        var allAxes = model.getAxes();

        if (!axisToMoveToTheRight.isOnRight()) {
            // Axis to move to the right is located on the left side of the graph
            var allAxesOnTheLeft = allAxes.stream().filter(axis -> !axis.isOnRight()).collect(Collectors.toList());
            int axisToMoveToTheRight_index_allAxesOnTheLeft = allAxesOnTheLeft.indexOf(axisToMoveToTheRight);
            if (axisToMoveToTheRight_index_allAxesOnTheLeft < allAxesOnTheLeft.size() - 1) {
                var axisOnTheRight = allAxesOnTheLeft.get(axisToMoveToTheRight_index_allAxesOnTheLeft + 1);
                axisIndex1 = axisToMoveToTheRight_index;
                axisIndex2 = allAxes.indexOf(axisOnTheRight);
                UndoableAction moveAxisToTheRight = new ExchangeAxesUndoableAction("Move Axis to the Right",
                                                                                   model,
                                                                                   axisIndex1,
                                                                                   axisIndex2);
                setOnAction(actionEvent -> undoableActionManager.execute(moveAxisToTheRight));
            }
        }
        else {
            // Axis to move to the right is located on the right side of the graph
            var allAxesOnTheRight = allAxes.stream().filter(axis -> axis.isOnRight()).collect(Collectors.toList());
            int axisToMoveToTheRight_index_allAxesOnTheRight = allAxesOnTheRight.indexOf(axisToMoveToTheRight);
            if (axisToMoveToTheRight_index_allAxesOnTheRight > 0) {
                var axisOnTheRight = allAxesOnTheRight.get(axisToMoveToTheRight_index_allAxesOnTheRight - 1); // On the right side of the graph, axes are stored in "opposite" relative ordering
                axisIndex1 = allAxes.indexOf(axisOnTheRight);
                axisIndex2 = axisToMoveToTheRight_index;
                UndoableAction moveAxisToTheRight = new ExchangeAxesUndoableAction("Move Axis to the Right",
                                                                                   model,
                                                                                   axisIndex1,
                                                                                   axisIndex2);
                setOnAction(actionEvent -> undoableActionManager.execute(moveAxisToTheRight));
            }
        }
    }
}
