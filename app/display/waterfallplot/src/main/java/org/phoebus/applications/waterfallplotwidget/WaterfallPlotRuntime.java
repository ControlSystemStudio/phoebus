package org.phoebus.applications.waterfallplotwidget;

import javafx.util.Pair;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.trends.databrowser3.archive.ArchiveFetchJob;
import org.csstudio.trends.databrowser3.archive.ArchiveFetchJobListener;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.model.PVSamples;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.RequestType;
import org.epics.util.array.ListNumber;
import org.epics.util.stats.Range;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VType;
import org.python.google.common.util.concurrent.AtomicDouble;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WaterfallPlotRuntime extends WidgetRuntime<WaterfallPlotWidget> {

    private List<String> pvNames = new LinkedList<>();
    private long timeSpanInSeconds = 1800;

    private PVData pvData;
    private boolean retrieveHistoricValuesFromTheArchiver;
    protected PVData getPVData() {
        return pvData;
    }

    @Override
    public void initialize(WaterfallPlotWidget waterfallPlotWidget) {
        super.initialize(waterfallPlotWidget);
        {
            String timespanString = waterfallPlotWidget.propTimespan().getValue();
            var parsedTimespan = WaterfallPlotController.parseTimespanString(timespanString);
            if (parsedTimespan.isPresent()) {
                var parsedTimespanInSeconds = WaterfallPlotController.timespanInSeconds(parsedTimespan.get());
                timeSpanInSeconds = parsedTimespanInSeconds;
            }
        }
        boolean isWaveform = waterfallPlotWidget.propInputIsWaveformPV().getValue();
        if (isWaveform) {
            pvData = new WaveformPVData(new AtomicDouble(Double.NaN),
                                        new AtomicDouble(Double.NaN),
                                        new ConcurrentSkipListMap<>());
        }
        else {
            pvData = new ScalarPVsData(new AtomicDouble(Double.NaN),
                                       new AtomicDouble(Double.NaN),
                                       new ArrayList<>());
        }
        retrieveHistoricValuesFromTheArchiver = waterfallPlotWidget.propRetrieveHistoricValuesFromTheArchiver().getValue();
        pvNames = waterfallPlotWidget.propInputPVs().getValue().stream().map(widgetProperty -> widgetProperty.getValue()).collect(Collectors.toUnmodifiableList());
    }

    public sealed interface PVData permits WaveformPVData, ScalarPVsData {}
    // The type ConcurrentSkipListMap is used for the data points to allow for concurrent insertions and deletions:
    public record WaveformPVData (AtomicDouble minFromPV,
                                  AtomicDouble maxFromPV,
                                  ConcurrentSkipListMap<Instant, ArrayList<Double>> instantToValue) implements PVData {}
    public record ScalarPVsData (AtomicDouble minFromPV,
                                 AtomicDouble maxFromPV,
                                 ArrayList<Pair<String, ConcurrentSkipListMap<Instant, Double>>> pvNameToInstantToValue) implements PVData {}

    @Override
    public void start() {

        super.start();

        if (pvData instanceof ScalarPVsData scalarPVsData) {
            for (var pvName : pvNames) {
                try {
                    RuntimePV runtimePV = PVFactory.getPV(pvName);
                    super.addPV(runtimePV, false);
                    ConcurrentSkipListMap<Instant, Double> instantToValue = new ConcurrentSkipListMap<>();
                    scalarPVsData.pvNameToInstantToValue.add(new Pair(pvName, instantToValue));
                    runtimePV.addListener((pv, vType) -> {
                        if (vType instanceof VNumber vnumber) {
                            instantToValue.put(vnumber.getTime().getTimestamp(), vnumber.getValue().doubleValue());
                            {
                                Range displayRange = vnumber.getDisplay().getDisplayRange();
                                double minFromPV = displayRange.getMinimum();
                                scalarPVsData.minFromPV.set(minFromPV);
                                double maxFromPV = displayRange.getMaximum();
                                scalarPVsData.maxFromPV.set(maxFromPV);
                            }
                        }
                        else if (vType instanceof VEnum vEnum) {
                            instantToValue.put(vEnum.getTime().getTimestamp(), (double) vEnum.getIndex());

                            {
                                int enumSize = vEnum.getDisplay().getChoices().size();
                                double minFromPV = 0;
                                scalarPVsData.minFromPV.set(minFromPV);
                                double maxFromPV = enumSize - 1;
                                scalarPVsData.maxFromPV.set(maxFromPV);
                            }
                        }
                        else {
                            logger.log(Level.WARNING, "Waterfall Plot widget: unsupported VType receoved: " + vType.toString());
                        }
                    });

                    if (retrieveHistoricValuesFromTheArchiver) {
                        retrieveArchivedPVValues(pvName,
                                Instant.now().minusSeconds(timeSpanInSeconds),
                                Instant.now(),
                                values -> {
                                    for (var vtype : values) {
                                        if (vtype instanceof VNumber vnumber) {
                                            instantToValue.put(vnumber.getTime().getTimestamp(), vnumber.getValue().doubleValue());
                                        } else if (vtype instanceof VStatistics vstatistics) {
                                            instantToValue.put(vstatistics.getTime().getTimestamp(), vstatistics.getAverage());
                                        } else if (vtype instanceof VEnum vEnum) {
                                            instantToValue.put(vEnum.getTime().getTimestamp(), (double) vEnum.getIndex());
                                        }
                                    }
                                });
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if (pvData instanceof WaveformPVData waveformPVData && pvNames.size() == 1) {
            String pvName = pvNames.get(0);
            try {
                RuntimePV runtimePV = PVFactory.getPV(pvName);
                super.addPV(runtimePV, false);
                runtimePV.addListener((pv, vType) -> {
                    if (vType instanceof VNumberArray vNumberArray) {

                        int size = vNumberArray.getData().size();
                        ArrayList<Double> waveform = new ArrayList<>(size);
                        for (int m = 0; m < vNumberArray.getData().size(); m++) {
                            var value = vNumberArray.getData().getDouble(m);
                            waveform.add(value);
                        }
                        waveformPVData.instantToValue.put(vNumberArray.getTime().getTimestamp(), waveform);

                        {
                            Range displayRange = vNumberArray.getDisplay().getDisplayRange();
                            double minFromPV = displayRange.getMinimum();
                            waveformPVData.minFromPV.set(minFromPV);
                            double maxFromPV = displayRange.getMaximum();
                            waveformPVData.maxFromPV.set(maxFromPV);
                        }
                    } else if (vType instanceof VEnumArray vEnumArray) {

                        int size = vEnumArray.getData().size();
                        ArrayList<Double> waveform = new ArrayList<>(size);
                        ListNumber listNumber = vEnumArray.getIndexes();
                        for (int m = 0; m < vEnumArray.getData().size(); m++) {
                            var value = listNumber.getDouble(m);
                            waveform.add(value);
                        }

                        waveformPVData.instantToValue.put(vEnumArray.getTime().getTimestamp(), waveform);

                        {
                            int enumSize = vEnumArray.getDisplay().getChoices().size();
                            double minFromPV = 0;
                            waveformPVData.minFromPV.set(minFromPV);
                            double maxFromPV = enumSize - 1;
                            waveformPVData.maxFromPV.set(maxFromPV);
                        }
                    }
                });

                if (retrieveHistoricValuesFromTheArchiver) {
                    retrieveArchivedPVValues(pvName,
                            Instant.now().minusSeconds(timeSpanInSeconds),
                            Instant.now(),
                            values -> {
                                synchronized (pvData) {
                                    for (var vtype : values) {
                                        if (vtype instanceof VNumberArray vNumberArray) {

                                            int size = vNumberArray.getData().size();
                                            ArrayList<Double> waveform = new ArrayList<>(size);
                                            for (int m = 0; m < vNumberArray.getData().size(); m++) {
                                                var value = vNumberArray.getData().getDouble(m);
                                                waveform.add(value);
                                            }

                                            waveformPVData.instantToValue.put(vNumberArray.getTime().getTimestamp(), waveform);
                                        }
                                        else if (vtype instanceof VEnumArray vEnumArray) {

                                            int size = vEnumArray.getData().size();
                                            ArrayList<Double> waveform = new ArrayList<>(size);
                                            ListNumber listNumber = vEnumArray.getIndexes();
                                            for (int m = 0; m < vEnumArray.getData().size(); m++) {
                                                var value = listNumber.getDouble(m);
                                                waveform.add(value);
                                            }

                                            waveformPVData.instantToValue.put(vEnumArray.getTime().getTimestamp(), waveform);
                                        }
                                    }
                                }
                            });
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else {
            // This should not occur.
        }
    }

    @Override
    public void stop() {
        for (var pv : super.getPVs()) {
            super.removePV(pv);
            PVFactory.releasePV(pv);
        }
        super.stop();
    }

    private void retrieveArchivedPVValues(String pvName,
                                          Instant start,
                                          Instant end,
                                          Consumer<List<VType>> vTypeConsumer) {
        Object lock = new Object();
        PVItem pvItem = new PVItem(pvName, 0.0);

        if (pvName.isEmpty()) {
            return;
        }

        pvItem.setWaveformIndex(0);
        pvItem.setRequestType(RequestType.OPTIMIZED);

        try {
            pvItem.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        pvItem.useDefaultArchiveDataSources();

        Runnable closePVItem = () -> {
            pvItem.stop();
            pvItem.dispose();
        };

        ArchiveFetchJobListener archiveFetchJobListener = new ArchiveFetchJobListener() {
            
            @Override
            public void fetchCompleted(ArchiveFetchJob archiveFetchJob) {
                synchronized (lock) {
                    if (true) {
                        PVSamples samples = pvItem.getSamples();
                        Lock lock = samples.getLock();
                        lock.lock();

                        try {
                            int i = 0;
                            LinkedList<VType> values = new LinkedList<>();
                            while (i < samples.size()) {
                                PlotSample sample = samples.get(i);
                                VType vtype = sample.getVType();
                                values.add(vtype);
                                i += 1;
                            }
                            vTypeConsumer.accept(values);
                        } finally {
                            lock.unlock();
                        }
                        closePVItem.run();
                    }
                }
            }

            @Override
            public void archiveFetchFailed(ArchiveFetchJob archiveFetchJob, ArchiveDataSource archiveDataSource, Exception e) {
                closePVItem.run();
            }

            @Override
            public void channelNotFound(ArchiveFetchJob archiveFetchJob,
                                        boolean channelFoundAtLeastOnce,
                                        List<ArchiveDataSource> list) {
                if (!channelFoundAtLeastOnce) {
                    // If the channel was found at least once, closePVItem()
                    // will be called by fetchCompleted() instead.
                    closePVItem.run();
                }
            }
        };

        synchronized (lock) {

            ArchiveFetchJob archiveFetchJob = new ArchiveFetchJob(pvItem,
                                                                  start,
                                                                  end,
                                                                  archiveFetchJobListener); // Note: The archive fetch job is automatically scheduled by the constructor!
        }

        return;
    }
}
