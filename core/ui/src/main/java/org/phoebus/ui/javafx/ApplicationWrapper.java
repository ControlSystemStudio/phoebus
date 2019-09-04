package org.phoebus.ui.javafx;

import javafx.application.Application;
import javafx.stage.Stage;

/** Horrible no-good {@link Application} wrapper
 * 
 *  <p>As described on http://mail.openjdk.java.net/pipermail/openjfx-dev/2018-June/021977.html,
 *  when the Main class extends Application,
 *  the sun.launcher.LauncherHelper expects to find JFX on the module path.
 *  Having it on the classpath is not sufficient and aborts with 
 *  "Error: JavaFX runtime components are missing..".
 *  
 *  <p>This wrapper detaches the {@link Application} from the Main class,
 *  so no matter if JFX is on the classpath or modulepath,
 *  it's resolved and the application runs.
 *    
 *  @author Kay Kasemir
 */
abstract public class ApplicationWrapper
{
    private static Class<? extends ApplicationWrapper> clazz;

    public static class Wrapper extends Application
    {
        private ApplicationWrapper actual;
        
        public void start(final Stage stage) throws Exception
        {
            actual = clazz.getConstructor().newInstance();
            actual.start(stage);
        }
    }
    
    // Same as Application.start(Stage)...
    abstract public void start(final Stage stage) throws Exception;
    
    // Same as Application.launch(clazz, args)
    public static void launch(Class<? extends ApplicationWrapper> clazz, final String[] args)
    {
        ApplicationWrapper.clazz = clazz;
        Application.launch(Wrapper.class, args);
    }
}
