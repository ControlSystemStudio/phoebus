/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.ui;

import org.phoebus.framework.nls.NLS;

/** Externalized strings */
public class Messages
{
    /** Externalized strings */
    public static String Alarm,
                         Description,
                         CheckAll,
                         CheckAll_TT,
                         Completion,
                         Delete,
                         Delete_TT,
                         DisableSaveRestore,
                         EnterPositiveTolerance,
                         EnterTolerance,
                         EmptyTable,
                         EnterNewPV,
                         Error,
                         ExportXLSAction,
                         ExportXLSAction_TT,
                         ExportXLSImpossible,
                         ExportXLSImpossible_NoMeasure,
                         ExportXLSImpossible_NoConf,
                         ImpossibleToDelete,
                         InformationPopup,
                         InformationPopup_ConfAlreadyExist,
                         InformationPopup_DelConfHeader,
                         InformationPopup_NoConfToDel,
                         InformationPopup_NoMeasureToDel,
                         Insert,
                         Insert_TT,
                         InvalidFileExtension,
                         PV,
                         Restore,
                         Restore_TT,
                         RestoreSelection,
                         RestoreSelection_TT,
                         Saved,
                         Saved_Value_TimeStamp,
                         SearchPV,
                         Selected,
                         Snapshot,
                         Snapshot_TT,
                         SnapshotSelection,
                         SnapshotSelection_TT,
                         Time,
                         Timeout,
                         TimestampSave,
                         Tolerance,
                         Tolerance_TT,
                         UncheckAll,
                         UncheckAll_TT,
                         Value;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages() {
        // prevent instantiation
    }
}
