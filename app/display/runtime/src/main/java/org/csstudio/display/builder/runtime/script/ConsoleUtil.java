package org.csstudio.display.builder.runtime.script;

import org.csstudio.display.builder.runtime.script.ScriptUtil;
import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.logging.Level;

public class ConsoleUtil {

    static
    {
	logger.log(Level.INFO, "Script accessed ConsoleUtil. Update to use print or org.csstudio.display.builder.runtime.script.ScriptUtil.getLogger()");
    }
    
    /**Write information to CSS console.
     * @param message the output string.
     */
    public static void writeInfo(String message){
        ScriptUtil.getLogger().info(message);
    }

    /**Write Error information to CSS console.
     * @param message the output string.
     */
    public static void writeError(String message){
        ScriptUtil.getLogger().severe(message);
    }

    /**Write Warning information to CSS console.
     * @param message the output string.
     */
    public static void writeWarning(String message){
        ScriptUtil.getLogger().warning(message);
    }

    /**Write pure string to CSS console without any extra headers in black color.
     * @param string the output string.
     */
    public static void writeString(String string){
        ScriptUtil.getLogger().info(string);
    }

}
