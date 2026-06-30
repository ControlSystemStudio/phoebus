package org.phoebus.applications.uxanalytics.monitor.representation;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeApplication;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.epics.vtype.Display;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.phoebus.applications.uxanalytics.monitor.UXAMouseMonitor;
import org.phoebus.applications.uxanalytics.monitor.UXAToolkitListener;
import org.phoebus.applications.viewer3d.ResourceUtil;
import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ActiveTabTest {
    private DisplayRuntimeApplication app;
    private DisplayRuntimeInstance instance;
    private DockItemWithInput tab;
    private static final String WINDOW_ID = "testWindowID";

    @BeforeAll
    public static void startJFX()
    {
        try {
            Platform.startup(() -> {});
        }
        catch (IllegalStateException e) {
            // JFX already started
        }
    }

    @BeforeEach
    public void setUp(){
        app = mock(DisplayRuntimeApplication.class);
        when(app.getDisplayName()).thenReturn("test");
        instance = mock(DisplayRuntimeInstance.class);
        when(instance.getAppDescriptor()).thenReturn(app);
        BorderPane pane = spy(new BorderPane());
        List<UXAToolkitListener> listeners = new ArrayList<>();
        doAnswer(invocation->{
            UXAToolkitListener listener = invocation.getArgument(0);
            listeners.add(listener);
            return null;
        }).when(instance).addListener(any(UXAToolkitListener.class));

        doAnswer(invocation->{
            listeners.remove((UXAToolkitListener) invocation.getArgument(0));
            return null;
        }).when(instance).removeListener(any(UXAToolkitListener.class));


        when(app.getName()).thenReturn("test");
        when(app.getDisplayName()).thenReturn("test");
        when(app.create()).thenReturn(instance);
        try{
            String filename = ActiveTabTest.class.getResource("/test.bob").getPath();
            InputStream test_display = ResourceUtil.openResource(filename);
            ModelReader rdr = new ModelReader(test_display, filename);
            DisplayModel mdl = rdr.readModel();
            DisplayInfo info = new DisplayInfo("test", filename, new Macros(), false);
            tab = new DockItemWithInput(instance, pane, URI.create(filename),null,null);
            when(instance.getDockItem()).thenReturn(tab);
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    @Test
    public void testConstructorAddsListenersExactlyOnce(){
        ActiveWindowsService svc = mock(ActiveWindowsService.class);
        when(svc.isActive()).thenReturn(true);
        ActiveTab activeTab = spy(new ActiveTab(tab,svc,WINDOW_ID));
        Assertions.assertNotNull(activeTab.getMouseMonitor());
        Assertions.assertNotNull(activeTab.getParentTab().getContent());
        activeTab.addListeners();//should do nothing, already done in constructor, for total of one listener-add
        verify(instance, times(1)).addListener(any(UXAToolkitListener.class));
        verify(activeTab.getParentTab().getContent(), times(1)).addEventFilter(eq(MouseEvent.MOUSE_CLICKED), any(UXAMouseMonitor.class));
    }

    @Test
    public void testConstructorPopulatesFields(){
        ActiveWindowsService svc = mock(ActiveWindowsService.class);
        when(svc.isActive()).thenReturn(true);
        ActiveTab activeTab = new ActiveTab(tab, svc, WINDOW_ID);
        Assertions.assertNotNull(activeTab.getMouseMonitor());
        Assertions.assertNotNull(activeTab.getParentTab().getContent());
        Assertions.assertTrue(activeTab.isListening());
    }

    @Test
    public void testDetachListeners(){
        ActiveWindowsService svc = mock(ActiveWindowsService.class);
        when(svc.isActive()).thenReturn(true);
        ActiveTab activeTab = spy(new ActiveTab(tab,svc, WINDOW_ID));
        verify(instance, times(1)).addListener(any(UXAToolkitListener.class));
        verify(activeTab.getParentTab().getContent(), times(1)).addEventFilter(eq(MouseEvent.MOUSE_CLICKED), any(UXAMouseMonitor.class));
        activeTab.detachListeners();
        verify(instance, times(1)).removeListener(any(UXAToolkitListener.class));
        verify(activeTab.getParentTab().getContent(), times(1)).removeEventFilter(eq(MouseEvent.MOUSE_CLICKED), any(UXAMouseMonitor.class));
        Assertions.assertFalse(activeTab.isListening());
    }


}
