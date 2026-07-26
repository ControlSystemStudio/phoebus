package org.phoebus.applications.uxanalytics.monitor.backend.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import org.csstudio.display.actions.OpenDisplayAction;
import org.csstudio.display.actions.WritePVAction;
import org.csstudio.display.builder.model.Widget;

import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeApplication;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.phoebus.applications.uxanalytics.monitor.representation.ActiveTab;
import org.phoebus.applications.uxanalytics.monitor.util.FileUtils;
import org.phoebus.applications.uxanalytics.monitor.util.ResourceOpenSources;
import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.docking.DockItemWithInput;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.stream.Stream;

import static org.csstudio.display.actions.OpenDisplayAction.OPEN_DISPLAY;
import static org.mockito.Mockito.*;
import static org.phoebus.applications.uxanalytics.monitor.backend.database.ServiceLayerConnection.*;

public class ServiceLayerConnectionTest {

    private DisplayRuntimeApplication app;
    private DisplayRuntimeInstance instance;
    private DisplayRuntimeInstance other_instance;
    private final String display_path = ServiceLayerConnectionTest.class.getResource("/test.bob").getPath().replace("file:", "");;
    private final String other_display_path = ServiceLayerConnectionTest.class.getResource("/test2.bob").getPath().replace("file:", "");

    @BeforeAll
    public static void initJFX(){
        try {
            Platform.startup(() -> {});
        }
        catch (IllegalStateException e) {
            // JFX already started
        }
    }

    public void setupUI(){
        app = mock(DisplayRuntimeApplication.class);
        instance = mock(DisplayRuntimeInstance.class);
        other_instance = mock(DisplayRuntimeInstance.class);
        String dummyPath = "";
        try{
            //this probably lives in a git repository if you're running this test
            //ignore result, just make sure it can be done
            Assertions.assertNotNull(FileUtils.getAnalyticsPathFor(display_path));
            Assertions.assertNotNull(FileUtils.getAnalyticsPathFor(other_display_path));
        }
        catch(NullPointerException e){
            //and if it isn't, temporarily create a dummy .git directory adjacent to the test.bob file
            File gitdir = new File(".git");
            gitdir.mkdir();
            //It doesn't matter what the path is, as long as there's a .git directory somewhere up the hierarchy.
        }

        when(app.create()).thenReturn(instance);
        when(app.create(URI.create(display_path))).thenReturn(instance);
        when(app.create(URI.create(other_display_path))).thenReturn(other_instance);
        when(instance.getRepresentation()).thenReturn(mock(JFXRepresentation.class));
        when(instance.getAppDescriptor()).thenReturn(app);
        when(instance.getDisplayInfo()).thenReturn(mock(DisplayInfo.class));
        when(instance.getDisplayInfo().getName()).thenReturn("test");
        when(instance.getDisplayInfo().getPath()).thenReturn(display_path);
        when(other_instance.getRepresentation()).thenReturn(mock(JFXRepresentation.class));
        when(other_instance.getAppDescriptor()).thenReturn(app);
        when(other_instance.getDisplayInfo()).thenReturn(mock(DisplayInfo.class));
        when(other_instance.getDisplayInfo().getName()).thenReturn("test2");
        when(other_instance.getDisplayInfo().getPath()).thenReturn(other_display_path);
    }


    @Test
    public void testMockServerWorks(){
        MockCheckConnectionHandler handler = new MockCheckConnectionHandler(true, true, true);
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setCheckConnectionHandler(handler);
        endpoint.start();
        try {
            HttpClient bareClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11111/analytics/checkConnection"))
                    .GET()
                    .build();
            HttpResponse<String> response = bareClient.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(handler.generateConnectionStatus(true, true, true), response.body());
        }
        catch (Exception e){
            Assertions.fail("Failed to send bare request to localhost:11111/analytics/checkConnection");
        }
        endpoint.stop();
    }

    @Test
    public void testConnectionAllOK(){
        MockCheckConnectionHandler handler = new MockCheckConnectionHandler(true, true, true);
        ServiceLayerConnection serviceLayerConnection = ServiceLayerConnection.getInstance();
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setCheckConnectionHandler(handler);
        endpoint.start();
        try {
            Assertions.assertTrue(serviceLayerConnection.connect("localhost", 11111, null, null));
        }
        catch (Exception e){
            Assertions.fail("Failed to get proper response from localhost:11111/analytics/checkConnection");
        }
        finally{
            endpoint.stop();
        }
    }

    static Stream<boolean[]> generateConnectionTestCases() {
        return Stream.of(
                new boolean[]{false, false, false},
                new boolean[]{false, false, true},
                new boolean[]{false, true, false},
                new boolean[]{false, true, true},
                new boolean[]{true, false, false},
                new boolean[]{true, false, true},
                new boolean[]{true, true, false}
        );
    }

    @ParameterizedTest
    @MethodSource("generateConnectionTestCases")
    void testConnectionBadCases(boolean[] args){
        MockCheckConnectionHandler handler = new MockCheckConnectionHandler(args[0], args[1], args[2]);
        ServiceLayerConnection serviceLayerConnection = ServiceLayerConnection.getInstance();
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setCheckConnectionHandler(handler);
        endpoint.start();
        try {
            Assertions.assertFalse(serviceLayerConnection.connect("localhost", 11111, null, null));
        }
        catch (Exception e){
            Assertions.fail("Failed to get proper response from localhost:11111/analytics/checkConnection");
        }
        finally{
            endpoint.stop();
        }
    }


    static Stream<Boolean> trueThenFalse() {
        return Stream.of(
                Boolean.TRUE,
                Boolean.FALSE
        );
    }

    @ParameterizedTest
    @MethodSource("trueThenFalse")
    public void testHandleClick(Boolean handlerStatus){
        MockClickHandler handler = new MockClickHandler();
        handler.good=handlerStatus;
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setClickHandler(handler);
        endpoint.start();
        setupUI();
        BorderPane content = mock(BorderPane.class);
        DockItemWithInput dockItem = new DockItemWithInput(instance, content,URI.create(display_path),null,null);
        ActiveTab tab = new ActiveTab(dockItem, null);
        String filename = FileUtils.getAnalyticsPathFor(display_path);
        ServiceLayerConnection serviceLayerConnection = ServiceLayerConnection.getInstance();
        serviceLayerConnection.connect("localhost", 11111, null, null);
        serviceLayerConnection.handleClick(tab,42,24);
        try{
            handler.received.get(1000, TimeUnit.MILLISECONDS);
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, Object> map = mapper.readValue(handler.received_body, HashMap.class);
            Assertions.assertEquals("42", map.get("x"));
            Assertions.assertEquals("24", map.get("y"));
            Assertions.assertEquals(filename, map.get("filename"));
        }
        catch(Exception failure){
            if(handlerStatus)
                Assertions.fail("Failed to get proper response from localhost:11111/analytics/click");
            else
                Assertions.assertEquals("{\"error\": \"Internal Server Error\"}", handler.received_body);
        }
        finally {
            serviceLayerConnection.resetLogging();
            endpoint.stop();
        }

    }

    @Test
    public void testWritePV(){
        MockActionHandler handler = new MockActionHandler();
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setActionHandler(handler);
        endpoint.start();
        setupUI();
        BorderPane content = mock(BorderPane.class);
        DockItemWithInput dockItem = new DockItemWithInput(instance, content,URI.create(display_path),null,null);
        ActiveTab tab = new ActiveTab(dockItem,null);
        String filename = FileUtils.getAnalyticsPathFor(display_path);
        ServiceLayerConnection serviceLayerConnection = ServiceLayerConnection.getInstance();
        serviceLayerConnection.resetLogging();
        serviceLayerConnection.connect("localhost", 11111, null, null);
        String pvName = "testPV";
        WritePVAction action = new WritePVAction(null, pvName, "123");
        Widget widget = new ActionButtonWidget();
        try {
            widget.setPropertyValue("name", "testWidget");
            serviceLayerConnection.handleAction(tab, widget, action);
            handler.received.get(1000, TimeUnit.MILLISECONDS);
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, Object> map = mapper.readValue(handler.received_body, HashMap.class);
            Assertions.assertEquals(filename, map.get("srcName"));
            Assertions.assertEquals(TYPE_DISPLAY, map.get("srcType"));
            Assertions.assertEquals(ACTION_WROTE, map.get("action"));
            Assertions.assertEquals("testPV", map.get("dstName"));
            Assertions.assertEquals(TYPE_PV, map.get("dstType"));
            Assertions.assertEquals("testWidget", map.get("via"));
        }
        catch(Exception e){
            Assertions.fail("Failed to set property value");
        }
    }


    @Test
    public void testHandleDisplayOpenViaActionButton(){
        MockActionHandler handler = new MockActionHandler();
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setActionHandler(handler);
        endpoint.start();
        setupUI();
        BorderPane content = mock(BorderPane.class);
        DockItemWithInput dockItem = new DockItemWithInput(instance, content,URI.create(display_path),null,null);
        ActiveTab tab = new ActiveTab(dockItem,null);
        String filename = FileUtils.getAnalyticsPathFor(display_path);
        String other_filename = FileUtils.getAnalyticsPathFor(other_display_path);
        DockItemWithInput other_dockItem = new DockItemWithInput(other_instance, content,URI.create(other_display_path),null,null);
        ActiveTab other_tab = new ActiveTab(other_dockItem,null);
        ServiceLayerConnection serviceLayerConnection = ServiceLayerConnection.getInstance();
        serviceLayerConnection.resetLogging();
        serviceLayerConnection.connect("localhost", 11111, null, null);
        OpenDisplayAction action = mock(OpenDisplayAction.class);
        when(action.getType()).thenReturn(OPEN_DISPLAY);
        DisplayInfo displayInfo = new DisplayInfo(display_path, null, new Macros(), true );
        when(action.getFile()).thenReturn("test2.bob");
        ActionButtonWidget widget = new ActionButtonWidget();
        try {
            widget.setPropertyValue("name", "testWidget");
            serviceLayerConnection.handleAction(tab, widget, action);
            handler.received.get(1000, TimeUnit.MILLISECONDS);
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, Object> map = mapper.readValue(handler.received_body, HashMap.class);
            Assertions.assertEquals(other_filename, map.get("dstName"));
            Assertions.assertEquals(filename, map.get("srcName"));
            Assertions.assertEquals("testWidget", map.get("via"));
            Assertions.assertEquals(ACTION_OPENED, map.get("action"));
            Assertions.assertEquals(TYPE_DISPLAY, map.get("dstType"));
            Assertions.assertEquals(TYPE_DISPLAY, map.get("srcType"));

        }
        catch (Exception e) {
            Assertions.fail("Failed to properly record action");
        }
        finally{
            endpoint.stop();
            serviceLayerConnection.resetLogging();
        };
    }

    static Stream<ResourceOpenSources> otherSources() {
        return Stream.of(
                ResourceOpenSources.FILE_BROWSER,
                ResourceOpenSources.TOP_RESOURCES,
                ResourceOpenSources.RESTORED,
                ResourceOpenSources.UNKNOWN
        );

    }

    @ParameterizedTest
    @MethodSource("otherSources")
    public void testHandleDisplayOpenViaApplication(ResourceOpenSources source){
        MockActionHandler handler = new MockActionHandler();
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setActionHandler(handler);
        endpoint.start();
        setupUI();

        DisplayInfo info = new DisplayInfo(display_path, null, new Macros(), true );
        String filename = FileUtils.getAnalyticsPathFor(display_path);
        ServiceLayerConnection serviceLayerConnection = ServiceLayerConnection.getInstance();
        serviceLayerConnection.resetLogging();
        serviceLayerConnection.connect("localhost", 11111, null, null);
        try {
            serviceLayerConnection.handleDisplayOpen(info, null, source);
            handler.received.get(1000, TimeUnit.MILLISECONDS);
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, Object> map = mapper.readValue(handler.received_body, HashMap.class);
            Assertions.assertEquals(source.name().toLowerCase(), map.get("srcName"));
            Assertions.assertEquals("origin_source", map.get("srcType"));
            Assertions.assertEquals(filename, map.get("dstName"));
            Assertions.assertEquals(TYPE_DISPLAY, map.get("dstType"));
            Assertions.assertEquals(ACTION_OPENED, map.get("action"));
            Assertions.assertNull(map.get("via"));
        }
        catch (Exception e) {
            Assertions.fail("Failed to properly record action");
        }
        finally{
            endpoint.stop();
            serviceLayerConnection.resetLogging();
        };
    }

    static Stream<ResourceOpenSources> navigationSources() {
        return Stream.of(
                ResourceOpenSources.NAVIGATION_BUTTON,
                ResourceOpenSources.RELOAD
        );
    }

    @ParameterizedTest
    @MethodSource("navigationSources")
    public void testHandleDisplayOpenViaNavigation(ResourceOpenSources source){
        MockActionHandler handler = new MockActionHandler();
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setActionHandler(handler);
        endpoint.start();
        setupUI();

        DisplayInfo info = new DisplayInfo(display_path, null, new Macros(), true );
        String filename = FileUtils.getAnalyticsPathFor(display_path);
        DisplayInfo other_info = (source==ResourceOpenSources.RELOAD)?info:new DisplayInfo(other_display_path, null, new Macros(), true );
        String other_filename = (source==ResourceOpenSources.RELOAD)?filename:FileUtils.getAnalyticsPathFor(other_display_path);
        String expected_action = (source==ResourceOpenSources.RELOAD)?ACTION_RELOADED:ACTION_NAVIGATED;


        ServiceLayerConnection serviceLayerConnection = ServiceLayerConnection.getInstance();
        serviceLayerConnection.resetLogging();
        serviceLayerConnection.connect("localhost", 11111, null, null);
        try {
            serviceLayerConnection.handleDisplayOpen(other_info, info, source);
            handler.received.get(1000, TimeUnit.MILLISECONDS);
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, Object> map = mapper.readValue(handler.received_body, HashMap.class);
            Assertions.assertEquals(filename, map.get("srcName"));
            Assertions.assertEquals(TYPE_DISPLAY, map.get("srcType"));
            Assertions.assertEquals(other_filename, map.get("dstName"));
            Assertions.assertEquals(TYPE_DISPLAY, map.get("dstType"));
            Assertions.assertEquals(expected_action, map.get("action"));
            Assertions.assertNull(map.get("via"));
        }
        catch (Exception e) {
            Assertions.fail("Failed to properly record action");
        }
        finally{
            endpoint.stop();
            serviceLayerConnection.resetLogging();
        };
    }

    @Test
    public void testLoggingInhibitedForConnectionCheck(){
        ServiceLayerConnection connection = ServiceLayerConnection.getInstance();
        connection.resetLogging();
        ByteArrayOutputStream logCaptureStream = new ByteArrayOutputStream();
        StreamHandler testHandler = new StreamHandler(new PrintStream(logCaptureStream), new java.util.logging.SimpleFormatter()) {
            @Override
            public synchronized void publish(LogRecord record) {
               super.publish(record);
               flush();
            }
        };
        Logger logger = Logger.getLogger(ServiceLayerConnection.class.getName());
        logger.addHandler(testHandler);
        logger.setLevel(Level.INFO);
        connection.connect("bogus", 12345, null, null);
        Assertions.assertTrue(logCaptureStream.size()>0);
        logCaptureStream.reset();
        connection.connect("bogus", 12345, null, null);
        Assertions.assertEquals(0, logCaptureStream.size());
        logCaptureStream.reset();
        logger.setLevel(Level.FINE);
        testHandler.setLevel(Level.FINE);
        connection.connect("bogus", 12345, null, null);
        Assertions.assertTrue(logCaptureStream.size()>0);
        connection.resetLogging();
        logger.removeHandler(testHandler);
    }

    @Test
    public void testLoggingInhibitedForClickHandle(){
        ServiceLayerConnection connection = ServiceLayerConnection.getInstance();
        connection.connect("bogus", 12345,null,null);
        connection.resetLogging();
        ByteArrayOutputStream logCaptureStream = new ByteArrayOutputStream();
        StreamHandler testHandler = new StreamHandler(new PrintStream(logCaptureStream), new java.util.logging.SimpleFormatter()) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        Logger logger = Logger.getLogger(ServiceLayerConnection.class.getName());
        logger.addHandler(testHandler);
        logger.setLevel(Level.INFO);
        connection.handleClick(null, 0, 0);
        Assertions.assertTrue(logCaptureStream.size()>0);
        logCaptureStream.reset();
        connection.handleClick(null, 0, 0);
        Assertions.assertEquals(0, logCaptureStream.size());
        logCaptureStream.reset();
        logger.setLevel(Level.FINE);
        testHandler.setLevel(Level.FINE);
        connection.handleClick(null, 0, 0);
        Assertions.assertTrue(logCaptureStream.size()>0);
        connection.resetLogging();
        logger.removeHandler(testHandler);
    }

    @Test
    public void testLoggingInhibitedForActionHandle(){
        ServiceLayerConnection connection = ServiceLayerConnection.getInstance();
        connection.connect("bogus", 12345,null,null);
        connection.resetLogging();
        ByteArrayOutputStream logCaptureStream = new ByteArrayOutputStream();
        OpenDisplayAction info = mock(OpenDisplayAction.class);
        when(info.getType()).thenReturn(OPEN_DISPLAY);
        StreamHandler testHandler = new StreamHandler(new PrintStream(logCaptureStream), new java.util.logging.SimpleFormatter()) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        Logger logger = Logger.getLogger(ServiceLayerConnection.class.getName());
        logger.addHandler(testHandler);
        logger.setLevel(Level.INFO);
        connection.handleAction(null, null, info);
        Assertions.assertTrue(logCaptureStream.size()>0);
        logCaptureStream.reset();
        connection.handleAction(null, null, info);
        Assertions.assertEquals(0, logCaptureStream.size());
        logCaptureStream.reset();
        logger.setLevel(Level.FINE);
        testHandler.setLevel(Level.FINE);
        connection.handleAction(null, null, info);
        Assertions.assertTrue(logCaptureStream.size()>0);
        connection.resetLogging();
        logger.removeHandler(testHandler);
    }

    @Test
    public void testLoggingInhibitedForOtherSourceDisplayOpened(){
        ServiceLayerConnection connection = ServiceLayerConnection.getInstance();
        connection.connect("bogus", 12345,null,null);
        connection.resetLogging();
        ByteArrayOutputStream logCaptureStream = new ByteArrayOutputStream();
        ResourceOpenSources src = ResourceOpenSources.FILE_BROWSER;
        StreamHandler testHandler = new StreamHandler(new PrintStream(logCaptureStream), new java.util.logging.SimpleFormatter()) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        Logger logger = Logger.getLogger(ServiceLayerConnection.class.getName());
        logger.addHandler(testHandler);
        logger.setLevel(Level.INFO);
        connection.handleDisplayOpen(null, null, src);
        Assertions.assertTrue(logCaptureStream.size()>0);
        logCaptureStream.reset();
        connection.handleDisplayOpen(null, null, src);
        Assertions.assertEquals(0, logCaptureStream.size());
        logCaptureStream.reset();
        logger.setLevel(Level.FINE);
        testHandler.setLevel(Level.FINE);
        connection.handleDisplayOpen(null, null, src);
        Assertions.assertTrue(logCaptureStream.size()>0);
        connection.resetLogging();
        logger.removeHandler(testHandler);
    }

}
