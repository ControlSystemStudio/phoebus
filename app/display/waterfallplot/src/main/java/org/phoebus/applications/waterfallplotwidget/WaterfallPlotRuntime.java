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
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
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
                                        new AtomicReference<>(new TreeMap<>()));
        }
        else {
            pvData = new ScalarPVsData(new AtomicDouble(Double.NaN),
                                       new AtomicDouble(Double.NaN),
                                       new LinkedList<>());
        }
        retrieveHistoricValuesFromTheArchiver = waterfallPlotWidget.propRetrieveHistoricValuesFromTheArchiver().getValue();
        pvNames = waterfallPlotWidget.propInputPVs().getValue().stream().map(widgetProperty -> widgetProperty.getValue()).collect(Collectors.toUnmodifiableList());
    }

    public sealed interface PVData permits WaveformPVData, ScalarPVsData {}
    // In order to be able to garbage collect data points that are no longer needed,
    // the TreeMap<> instantToValue is contained in an AtomicReference<>, so that we
    // can set the map to a new map without values that are no longer needed. This is
    // the case for both 'WaveformPVData' and 'ScalarPVsData'.
    public record WaveformPVData (AtomicDouble minFromPV,
                                  AtomicDouble maxFromPV,
                                  AtomicReference<TreeMap<Instant, LinkedList<Double>>> instantToValue) implements PVData {}
    public record ScalarPVsData (AtomicDouble minFromPV,
                                 AtomicDouble maxFromPV,
                                 LinkedList<Pair<String, AtomicReference<TreeMap<Instant, Double>>>> pvNameToInstantToValue) implements PVData {}

    @Override
    public void start() {

        super.start();

        if (pvData instanceof ScalarPVsData scalarPVsData) {
            for (var pvName : pvNames) {
                try {
                    RuntimePV runtimePV = PVFactory.getPV(pvName);
                    super.addPV(runtimePV, false);
                    AtomicReference<TreeMap<Instant, Double>> instantToValueAtomicReference = new AtomicReference<>(new TreeMap<>());
                    scalarPVsData.pvNameToInstantToValue.add(new Pair(pvName, instantToValueAtomicReference));
                    runtimePV.addListener((pv, vType) -> {
                        if (vType instanceof VNumber vnumber) {
                            synchronized (pvData) {
                                instantToValueAtomicReference.get().put(vnumber.getTime().getTimestamp(), vnumber.getValue().doubleValue());
                                {
                                    Range displayRange = vnumber.getDisplay().getDisplayRange();
                                    double minFromPV = displayRange.getMinimum();
                                    scalarPVsData.minFromPV.set(minFromPV);
                                    double maxFromPV = displayRange.getMaximum();
                                    scalarPVsData.maxFromPV.set(maxFromPV);
                                }
                            }
                        }
                        else if (vType instanceof VEnum vEnum) {
                            synchronized (pvData) {
                                instantToValueAtomicReference.get().put(vEnum.getTime().getTimestamp(), (double) vEnum.getIndex());

                                {
                                    int enumSize = vEnum.getDisplay().getChoices().size();
                                    double minFromPV = 0;
                                    scalarPVsData.minFromPV.set(minFromPV);
                                    double maxFromPV = enumSize - 1;
                                    scalarPVsData.maxFromPV.set(maxFromPV);
                                }
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
                                    synchronized (pvData) {
                                        for (var vtype : values) {
                                            if (vtype instanceof VNumber vnumber) {
                                                instantToValueAtomicReference.get().put(vnumber.getTime().getTimestamp(), vnumber.getValue().doubleValue());
                                            }
                                            else if (vtype instanceof VStatistics vstatistics) {
                                                instantToValueAtomicReference.get().put(vstatistics.getTime().getTimestamp(), vstatistics.getAverage());
                                            }
                                            else if (vtype instanceof VEnum vEnum) {
                                                instantToValueAtomicReference.get().put(vEnum.getTime().getTimestamp(), (double) vEnum.getIndex());
                                            }

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

                        LinkedList<Double> waveform = new LinkedList<>();
                        for (int m = 0; m < vNumberArray.getData().size(); m++) {
                            var value = vNumberArray.getData().getDouble(m);
                            waveform.add(value);
                        }

                        synchronized (pvData) {
                            waveformPVData.instantToValue.get().put(vNumberArray.getTime().getTimestamp(), waveform);

                            {
                                Range displayRange = vNumberArray.getDisplay().getDisplayRange();
                                double minFromPV = displayRange.getMinimum();
                                waveformPVData.minFromPV.set(minFromPV);
                                double maxFromPV = displayRange.getMaximum();
                                waveformPVData.maxFromPV.set(maxFromPV);
                            }
                        }
                    }
                    else if (vType instanceof VEnumArray vEnumArray) {

                        LinkedList<Double> waveform = new LinkedList<>();
                        ListNumber listNumber = vEnumArray.getIndexes();
                        for (int m = 0; m < vEnumArray.getData().size(); m++) {
                            var value = listNumber.getDouble(m);
                            waveform.add(value);
                        }

                        synchronized (pvData) {
                            waveformPVData.instantToValue.get().put(vEnumArray.getTime().getTimestamp(), waveform);

                            {
                                int enumSize = vEnumArray.getDisplay().getChoices().size();
                                double minFromPV = 0;
                                waveformPVData.minFromPV.set(minFromPV);
                                double maxFromPV = enumSize - 1;
                                waveformPVData.maxFromPV.set(maxFromPV);
                            }
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

                                            LinkedList<Double> waveform = new LinkedList<>();
                                            for (int m = 0; m < vNumberArray.getData().size(); m++) {
                                                var value = vNumberArray.getData().getDouble(m);
                                                waveform.add(value);
                                            }

                                            waveformPVData.instantToValue.get().put(vNumberArray.getTime().getTimestamp(), waveform);
                                        }
                                        else if (vtype instanceof VEnumArray vEnumArray) {

                                            LinkedList<Double> waveform = new LinkedList<>();
                                            ListNumber listNumber = vEnumArray.getIndexes();
                                            for (int m = 0; m < vEnumArray.getData().size(); m++) {
                                                var value = listNumber.getDouble(m);
                                                waveform.add(value);
                                            }

                                            waveformPVData.instantToValue.get().put(vEnumArray.getTime().getTimestamp(), waveform);
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
