package org.phoebus.applications.uxanalytics.monitor;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.ui.docking.DockItemWithInput;

import javafx.scene.input.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ActiveTab {

    private final ConcurrentLinkedDeque<Widget> widgets;
    private final DockItemWithInput parentTab;
    //private final File fileObject;
    //private final String SHA256;
    //private final String hashFilename;
    private final ToolkitListener toolkitListener;
    private final Supplier<Future<Boolean>> ok_to_close = () -> {
        this.close();
        return CompletableFuture.completedFuture(true);
    };
    private Node jfxNode;
    private UXAMouseMonitor mouseMonitor;

    public ActiveTab(DockItemWithInput tab){
        widgets = new ConcurrentLinkedDeque<>();
        parentTab = tab;
        //fileObject = ResourceParser.getFile(parentTab.getInput());
        //SHA256 = getFileSHA256(fileObject);
        //hashFilename = getFileName()+getFirst8CharsSHA256();
        toolkitListener = new UXAToolkitListener();
        ((UXAToolkitListener)toolkitListener).setTabWrapper(this);
        ((DisplayRuntimeInstance)tab.getProperties().get("application")).addListener(toolkitListener);
        jfxNode = tab.getContent();
        mouseMonitor = new UXAMouseMonitor(this);
        jfxNode.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseMonitor);
        parentTab.addCloseCheck(ok_to_close);
    }

    //public File getFileObject(){
    //    return fileObject;
    //}

    //public String getFileName(){
    //    return fileObject.getName();
    //}

    //re-implement here with Java MessageDigest, so we don't need to depend on elog
    private static String getFileSHA256(File fileObject){
        try(InputStream fis = new FileInputStream(fileObject)){
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] byteBuffer = new byte[1024];
            int bytesCount = 0;

            while((bytesCount = fis.read(byteBuffer)) != -1){
                digest.update(byteBuffer, 0, bytesCount);
            }

            byte[] bytes = digest.digest();

            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();

        } catch(IOException | NoSuchAlgorithmException e){;
            throw new RuntimeException(e);
        }
    }

    //public String getSHA256(){
    //    return SHA256;
    //}

    //public String getFirst8CharsSHA256(){
    //    return SHA256.substring(0, 8);
    //}

    public ToolkitListener getToolkitListener(){
        return toolkitListener;
    }

    public synchronized void add(Widget widget){
        widgets.add(widget);
    }

    public synchronized void remove(Widget widget){
        widgets.remove(widget);
    }

    public synchronized void close(){

        DisplayRuntimeInstance instance = (DisplayRuntimeInstance) parentTab.getApplication();
        if(instance != null)
            instance.removeListener(toolkitListener);
        if(jfxNode != null) {
            jfxNode.removeEventFilter(MouseEvent.MOUSE_CLICKED, mouseMonitor);
        }
    }

    public Tab getParentTab() {
        return parentTab;
    }

    //public String getHashFileName() {
    //    return hashFilename;
    //}
}
