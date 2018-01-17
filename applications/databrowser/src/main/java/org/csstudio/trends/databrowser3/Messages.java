/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import org.phoebus.framework.nls.NLS;

/** Eclipse string externalization
 *  @author Kay Kasemir
 */
public class Messages
{
    // ---
    // --- Keep alphabetically sorted and 'in sync' with messages.properties!
    // ---
    public static String AddArchive;
    public static String AddArchiveMsg;
    public static String AddAxis;
    public static String AddFormula;
    public static String AddFormulaMsg;
    public static String AddFormula_NameTT;
    public static String AddItemErrorFmt;
    public static String AddPV;
    public static String AddPV_Axis;
    public static String AddPV_AxisTT;
    public static String AddPVMsg;
    public static String AddPV_NameTT;
    public static String AddPV_NewOrEmptyAxis;
    public static String AddPV_OnChange;
    public static String AddPV_OnChangeTT;
    public static String AddPV_Period;
    public static String AddPV_PeriodTT;
    public static String AppendSearchResults;
    public static String AppendSearchResultsTT;
    public static String ApplyChanges;
    public static String ArchiveAccessMessageFmt;
    public static String ArchiveDataSources;
    public static String ArchiveDescription;
    public static String ArchiveFetchDetailFmt;
    public static String ArchiveFetchJobFmt;
    public static String ArchiveFetchProgressFmt;
    public static String ArchiveFetchStart;
    public static String ArchiveKey;
    public static String ArchiveListGUI_NoArchives;
    public static String ArchiveName;
    public static String ArchiveRescale_Label;
    public static String ArchiveRescale_NONE;
    public static String ArchiveRescale_STAGGER;
    public static String ArchiveServerErrorFmt;
    public static String ArchiveServerInfo;
    public static String ArchiveServerInfoTT;
    public static String ArchiveURLDialogTitle;
    public static String AutoScale;
    public static String AxesFontTT;
    public static String Axis;
    public static String AxisLabelFont;
    public static String AxisMax;
    public static String AxisMin;
    public static String AxisOnRight;
    public static String AxisScaleFont;
    public static String AxisTT;
    public static String AxisVisibility;
    public static String BackgroundColorLbl;
    public static String BackgroundColorTT;
    public static String ChangeLiveCapacityCommandErrorFmt;
    public static String ChangeNameErrorFmt;
    public static String Color;
    public static String ColorTT;
    public static String ConfigFileErrorFmt;
    public static String Connecting;
    public static String CursorTimestamp;
    public static String CursorTimestampTT;
    public static String CursorValue;
    public static String CursorValueTT;
    public static String DeleteArchive;
    public static String DeleteAxis;
    public static String DeleteAxisWarningFmt;
    public static String DeleteItem;
    public static String DuplicateItemFmt;
    public static String EditItems;
    public static String EmptyNameError;
    public static String EndTimeLbl;
    public static String EndTimeTT;
    public static String Error;
    public static String ErrorFmt;
    public static String ExportBrowse;
    public static String ExportBrowseTT;
    public static String ExportDefaultDigits;
    public static String ExportDefaultFilename;
    public static String ExportDefaultLinearInterpolation;
    public static String ExportDefaultLinearInterpolationTT;
    public static String ExportDefaultOptimization;
    public static String Export_Delimiter;
    public static String ExportDigits;
    public static String ExportDigitsError;
    public static String ExportDigitsTT;
    public static String ExportEnterFilenameError;
    public static String ExportFileExists;
    public static String ExportFileExistsFmt;
    public static String ExportFilename;
    public static String ExportFilenameTT;
    public static String ExportFormat_DecimalTT;
    public static String ExportFormat_DefaultTT;
    public static String ExportFormat_ExponentialTT;
    public static String ExportGroupFormat;
    public static String ExportGroupOutput;
    public static String ExportGroupSource;
    public static String ExportLinearIntervalError;
    public static String ExportMatlabFilenameError;
    public static String ExportMinMaxCol;
    public static String ExportMinMaxColTT;
    public static String Export_NoValueMarker;
    public static String ExportOptimizationTT;
    public static String ExportOptimizeCountError;
    public static String ExportPlotStartEnd;
    public static String ExportPlotStartEndTT;
    public static String ExportSource;
    public static String ExportSource_Linear;
    public static String ExportSource_LinearTT;
    public static String ExportSource_OptimizedArchive;
    public static String ExportSource_OptimizedArchiveTT;
    public static String ExportSource_Plot;
    public static String ExportSource_PlotTT;
    public static String ExportSource_RawArchive;
    public static String ExportSource_RawArchiveTT;
    public static String ExportStartExport;
    public static String ExportStartExportTT;
    public static String ExportTabular;
    public static String ExportTabularTT;
    public static String ExportTypeMatlab;
    public static String ExportTypeMatlabTT;
    public static String ExportTypeSpreadsheet;
    public static String ExportTypeSpreadsheetTT;
    public static String ExportValueInfo;
    public static String ExportValueInfoTT;
    public static String Format_Decimal;
    public static String Format_Default;
    public static String Format_Exponential;
    public static String Formula;
    public static String FormulaLabel;
    public static String FormulaLabelEditTT;
    public static String Grid;
    public static String GridLbl;
    public static String GridTT;
    public static String HideTraceWarning;
    public static String HideTraceWarningDetail;
    public static String ImportActionFileSelectorTitleFmt;
    public static String ImportActionLabelFmt;
    public static String ImportDialogMessage;
    public static String ImportedChannelBaseName;
    public static String ImportTitle;
    public static String ImportTypeLbl;
    public static String Information;
    public static String InspectSamples;
    public static String InvalidScanPeriodError;
    public static String InvalidStartEndTimeError;
    public static String ItemName;
    public static String ItemNameTT;
    public static String KeyLbl;
    public static String LabelFontLbl;
    public static String LabelFontTT;
    public static String LegendFontLbl;
    public static String LegendFontTT;
    public static String LinacScale;
    public static String LinLogScaleType;
    public static String LiveBufferSizeInfoFmt;
    public static String LiveData;
    public static String LiveSampleBufferSize;
    public static String LogentryDefaultBody;
    public static String LogentryDefaultTitle;
    public static String LogScale;
    public static String Miscellaneous;
    public static String Model_Disconnected;
    public static String MoveItemDown;
    public static String MoveItemUp;
    public static String Name;
    public static String NameLbl;
    public static String NegErrColumn;
    public static String NotApplicable;
    public static String OpenDataBrowserPerspective;
    public static String OpenExportView;
    public static String OpenPropertiesView;
    public static String OpenSearchView;
    public static String OpenWaveformView;
    public static String Plot_TimeAxisName;
    public static String Plot_ValueAxisName;
    public static String Plot_ValueAxisNameFMT;
    public static String PointSize;
    public static String PointSizeTT;
    public static String PointType;
    public static String PointTypeTT;
    public static String PosErrColumn;
    public static String PrefPage_ArchiveFetchDelay;
    public static String PrefPage_Archives;
    public static String PrefPage_AutomaticHistoryRefresh;
    public static String PrefPage_DataServerURLs;
    public static String PrefPage_LiveBufferSize;
    public static String PrefPage_PlotBins;
    public static String PrefPage_ScanPeriod;
    public static String PrefPage_TimeRange;
    public static String PrefPage_Title;
    public static String PrefPage_TraceLineWidth;
    public static String PrefPage_TraceOpacity;
    public static String PrefPage_UpdatePeriod;
    public static String PrintSnapshot;
    public static String PromptForErrors_Label;
    public static String PVName;
    public static String PVUsedInFormulaFmt;
    public static String Refresh;
    public static String RegularExpression;
    public static String RegularExpressionTT;
    public static String RemoveEmptyAxes;
    public static String ReplaceSearchResults;
    public static String ReplaceSearchResultsTT;
    public static String Request_optimized;
    public static String Request_raw;
    public static String RequestType;
    public static String RequestTypeTT;
    public static String RequestTypeWarning;
    public static String RequestTypeWarningDetail;
    public static String SampleView_Item;
    public static String SampleView_MinMaxValueTT;
    public static String SampleView_NoPlot;
    public static String SampleView_Refresh;
    public static String SampleView_RefreshTT;
    public static String SampleView_SelectItem;
    public static String SampleView_Source;
    public static String Save;
    public static String SaveChangesLbl;
    public static String SaveChangesTT;
    public static String ScaleFontLbl;
    public static String ScanPeriod;
    public static String ScanPeriodChangeErrorFmt;
    public static String ScanPeriodTT;
    public static String ScrollButtonTT;
    public static String ScrollStepLbl;
    public static String ScrollStepTT;
    public static String Search;
    public static String SearchArchiveConfirmFmt;
    public static String SearchChannelFmt;
    public static String SearchPattern;
    public static String SearchPatternTT;
    public static String SearchTT;
    public static String Search_URL;
    public static String Search_URL_TT;
    public static String SendToElog;
    public static String ServerAlias;
    public static String SeverityColumn;
    public static String SeverityStatusFmt;
    public static String StartEndDialogBtn;
    public static String StartEndDialogTT;
    public static String StartTimeLbl;
    public static String StartTimeTT;
    public static String StatusColumn;
    public static String TimeAxis;
    public static String TimeColumn;
    public static String TitleFontLbl;
    public static String TitleFontTT;
    public static String TitleLbl;
    public static String TitleTT;
    public static String TraceDisplayName;
    public static String TraceDisplayNameTT;
    public static String TraceLineWidth;
    public static String TraceLineWidthTT;
    public static String TracesTab;
    public static String TraceType;
    public static String TraceTypes_Label;
    public static String TraceTypeTT;
    public static String TraceVisibility;
    public static String TraceVisibilityTT;
    public static String UpdatePeriodLbl;
    public static String UpdatePeriodTT;
    public static String URL;
    public static String URL_Lbl;
    public static String UseAutoScale_Label;
    public static String UseAxisName;
    public static String UseDefaultArchives;
    public static String UseDefaultArchives_Label;
    public static String UseTraceNames;
    public static String ValueAxes;
    public static String ValueAxisName;
    public static String ValueColumn;
    public static String WaveformAmplitude;
    public static String WaveformIndex;
    public static String WaveformIndexCol;
    public static String WaveformIndexColTT;
    public static String WaveformStatus;
    public static String WaveformTimeSelector;
    public static String WaveformTimestamp;
    // ---
    // --- Keep alphabetically sorted and 'in sync' with messages.properties!
    // ---

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation
    }
}
