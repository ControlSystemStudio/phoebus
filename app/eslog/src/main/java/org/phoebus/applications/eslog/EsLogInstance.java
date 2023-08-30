package org.phoebus.applications.eslog;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.phoebus.applications.eslog.archivedjmslog.ElasticsearchModel;
import org.phoebus.applications.eslog.archivedjmslog.JMSReceiverPool;
import org.phoebus.applications.eslog.archivedjmslog.LiveModel;
import org.phoebus.applications.eslog.archivedjmslog.LogArchiveModel;
import org.phoebus.applications.eslog.model.EventLogMessage;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

class EsLogInstance implements AppInstance
{
    /** Singleton instance maintained by {@link EsLogApp} */
    static EsLogInstance INSTANCE = null;

    private final AppDescriptor app;
    // private final EsLog EsLog;
    private final DockItem tab;

    static final String[] PROPERTIES = { EventLogMessage.HOST,
            EventLogMessage.SEVERITY, EventLogMessage.TEXT };

    private LogArchiveModel model;

    class LogCellFactory implements
            Callback<TableColumn<EventLogMessage, String>, TableCell<EventLogMessage, String>>
    {
        @Override
        public TableCell<EventLogMessage, String> call(
                TableColumn<EventLogMessage, String> col)
        {
            final var tableCell = new TableCell<EventLogMessage, String>()
            {
                @Override
                protected void updateItem(String item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if (!empty && (null != this.getTableRow().getItem()))
                    {
                        final var severity = this.getTableRow().getItem()
                                .getPropertyValue(EventLogMessage.SEVERITY);
                        switch (severity)
                        {
                        // TODO: get rid of the constants
                        case "SEVERE":
                            if (getIndex() % 2 == 1)
                            {
                                this.setStyle("-fx-background-color: #FF4444;");
                            }
                            else
                            {
                                this.setStyle("-fx-background-color: #FF7777;");
                            }
                            break;
                        case "WARNING":
                            if (getIndex() % 2 == 1)
                            {
                                this.setStyle("-fx-background-color: #FFFF33;");
                            }
                            else
                            {
                                this.setStyle("-fx-background-color: #FFFF88;");
                            }
                            break;
                        default:
                            this.setStyle("");
                            break;
                        }

                        // TODO: selection, … in other words, make it usable

                        this.setText(item);
                        this.setTooltip(new Tooltip(
                                this.getTableRow().getItem().toString()));
                    }
                    else
                    {
                        this.setText(null);
                        this.setStyle(null);
                        this.setTooltip(null);
                    }
                }
            };
            return tableCell;
        }
    }

    class LogValueFactory implements
            Callback<CellDataFeatures<EventLogMessage, String>, ObservableValue<String>>
    {
        private String property;

        public LogValueFactory(final String property)
        {
            this.property = property;
        }

        @Override
        public ObservableValue<String> call(
                CellDataFeatures<EventLogMessage, String> msg)
        {
            return new SimpleStringProperty(
                    msg.getValue().getPropertyValue(this.property));
        }
    }

    @SuppressWarnings("nls")
    public EsLogInstance(final AppDescriptor app) throws Exception
    {
        this.app = app;
        model = createModel();

        final var timeRange = model.getTimerangeText();
        // controls
        final var startBox = new TextField(timeRange[0]);
        final var startLabel = new Label("_Start:");
        startLabel.setMnemonicParsing(true);
        startLabel.setLabelFor(startBox);

        final var endBox = new TextField(timeRange[1]);
        final var endLabel = new Label("_End:");
        endLabel.setMnemonicParsing(true);
        endLabel.setLabelFor(endBox);

        this.model.addTimeChangeListener(range -> {
            final var t = model.getTimerangeText();
            startBox.setText(t[0]);
            endBox.setText(t[1]);
        });

        startBox.setOnAction(e -> {
            try
            {
                model.setTimerange(startBox.getText(), endBox.getText());
                startBox.setStyle("");
            }
            catch (IllegalArgumentException ex)
            {
                startBox.setStyle("-fx-background-color: #FF7777");
            }
        });
        endBox.setOnAction(e -> {
            try
            {
                model.setTimerange(startBox.getText(), endBox.getText());
                endBox.setStyle("");
            }
            catch (IllegalArgumentException ex)
            {
                endBox.setStyle("-fx-background-color: #FF7777");
            }
        });

        final var timesBtn = new Button("_Times");
        timesBtn.setMnemonicParsing(true);
        timesBtn.setOnAction(this::openTimesDialog);

        final var filterBtn = new Button("_Filter");
        filterBtn.setMnemonicParsing(true);
        filterBtn.setOnAction(this::openFilterDialog);

        final var msgList = new TableView<EventLogMessage>(
                model.getObservable());
        var colDate = new TableColumn<EventLogMessage, String>("Date");
        colDate.setMinWidth(170);
        colDate.setCellValueFactory(new LogValueFactory(EventLogMessage.DATE));
        colDate.setCellFactory(new LogCellFactory());
        msgList.getColumns().add(colDate);

        var colSeverity = new TableColumn<EventLogMessage, String>("Severity");
        colSeverity.setComparator(new SeverityComparator());
        colSeverity.setMinWidth(90);
        colSeverity.setCellValueFactory(
                new LogValueFactory(EventLogMessage.SEVERITY));
        colSeverity.setCellFactory(new LogCellFactory());
        msgList.getColumns().add(colSeverity);

        var colText = new TableColumn<EventLogMessage, String>("Text");
        colText.setMinWidth(600);
        colText.setCellValueFactory(new LogValueFactory(EventLogMessage.TEXT));
        colText.setCellFactory(new LogCellFactory());
        msgList.getColumns().add(colText);

        var colHost = new TableColumn<EventLogMessage, String>("Host");
        colHost.setMinWidth(100);
        colHost.setCellValueFactory(new LogValueFactory(EventLogMessage.HOST));
        colHost.setCellFactory(new LogCellFactory());
        msgList.getColumns().add(colHost);

        var colDelta = new TableColumn<EventLogMessage, String>("Delta");
        colDelta.setMinWidth(100);
        colDelta.setCellValueFactory(
                new LogValueFactory(EventLogMessage.DELTA));
        colDelta.setCellFactory(new LogCellFactory());
        msgList.getColumns().add(colDelta);

        msgList.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        msgList.getSortOrder().add(colDate);

        model.addChangeListener(msgList::sort);
        model.updateFromArchive();

        // geometry
        final var layout = new GridPane();
        layout.add(startLabel, 0, 0);
        layout.add(startBox, 1, 0);
        GridPane.setHgrow(startBox, Priority.SOMETIMES);
        layout.add(endLabel, 2, 0);
        layout.add(endBox, 3, 0);
        GridPane.setHgrow(endBox, Priority.SOMETIMES);
        layout.add(timesBtn, 4, 0);
        layout.add(filterBtn, 5, 0);

        layout.add(msgList, 0, 1, 6, 1);
        GridPane.setVgrow(msgList, Priority.ALWAYS);
        GridPane.setHgrow(msgList, Priority.ALWAYS);

        tab = new DockItem(this, layout);
        tab.addCloseCheck(() -> {
            INSTANCE = null;
            return CompletableFuture.completedFuture(true);
        });
        DockPane.getActiveDockPane().addTab(tab);
    }

    @SuppressWarnings("nls")
    public LogArchiveModel createModel()
    {
        ElasticsearchModel<EventLogMessage> archive = null;
        if (!EsLogPreferences.es_url.isEmpty())
        {
            try
            {
                archive = new ElasticsearchModel<>(EsLogPreferences.es_url,
                        EsLogPreferences.es_index, EventLogMessage.DATE,
                        EventLogMessage::fromElasticsearch)
                {
                    @Override
                    public EventLogMessage[] getMessages()
                    {
                        EventLogMessage[] result = super.getMessages();
                        Instant lastTime = null;
                        for (EventLogMessage msg : result)
                        {
                            var thisTime = msg.getTime();
                            if (null != lastTime)
                            {
                                msg.setDelta(thisTime.toEpochMilli()
                                        - lastTime.toEpochMilli());
                            }
                            lastTime = thisTime;
                        }
                        return result;
                    }
                };
            }
            catch (MalformedURLException e)
            {
                Activator.logger.warning("Invalid archive URL configured."); //$NON-NLS-1$
            }
        }
        else
        {
            Activator.logger.config("No archive URL configured."); //$NON-NLS-1$
        }

        LiveModel<EventLogMessage> live = null;
        if (!EsLogPreferences.jms_url.isEmpty())
        {
            var receiver = JMSReceiverPool.getReceiver(EsLogPreferences.jms_url,
                    EsLogPreferences.jms_user, EsLogPreferences.jms_password,
                    EsLogPreferences.jms_topic);
            live = new LiveModel<>(receiver, EsLogPreferences.jms_topic,
                    EventLogMessage.DATE, EventLogMessage::fromJMS);
        }
        else
        {
            Activator.logger.config("No JMS URL configured."); //$NON-NLS-1$
        }

        return new LogArchiveModel(archive, live);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    private Object openFilterDialog(ActionEvent e)
    {
        var dialog = new FilterDialog(model.getFilters());
        var result = dialog.showAndWait();
        result.ifPresent(model::setFilters);
        return null;
    }

    private Object openTimesDialog(ActionEvent e)
    {
        final var dialog = new TimeSelectDialog(model.getTimerange());
        final var result = dialog.showAndWait();
        result.ifPresent(model::setTimerange);
        return null;
    }

    void raise()
    {
        // tab.select();
    }
}
