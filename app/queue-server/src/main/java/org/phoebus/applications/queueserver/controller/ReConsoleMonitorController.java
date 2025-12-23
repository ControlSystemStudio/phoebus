package org.phoebus.applications.queueserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.queueserver.Preferences;
import org.phoebus.applications.queueserver.api.ConsoleOutputUpdate;
import org.phoebus.applications.queueserver.api.ConsoleOutputWsMessage;
import org.phoebus.applications.queueserver.client.QueueServerWebSocket;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.PollCenter;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public final class ReConsoleMonitorController implements Initializable {

    @FXML private CheckBox  autoscrollChk;
    @FXML private Button    clearBtn;
    @FXML private TextField maxLinesField;
    @FXML private TextArea  textArea;

    private static final double EPS = 0.5;
    private ScrollBar vBar;

    private static final int MIN_LINES = 10, MAX_LINES = 10_000,
            BACKLOG   = 1_000, CHUNK    = 32_768;

    private int maxLines  = 1_000;
    private final ConsoleTextBuffer textBuf = new ConsoleTextBuffer(MAX_LINES);

    private final RunEngineService svc = new RunEngineService();
    private final ExecutorService  io  =
            Executors.newSingleThreadExecutor(r -> new Thread(r,"console-reader"));
    private static final ObjectMapper JSON = new ObjectMapper();

    private volatile boolean stop       = true;
    private volatile String  lastUid    = "ALL";
    private Instant lastLine  = Instant.EPOCH;

    private ScheduledFuture<?> pollTask;
    private volatile long lastProgrammaticScroll = 0;
    private final StringBuilder wsTextBuffer = new StringBuilder();
    private final Object wsTextLock = new Object();

    private QueueServerWebSocket<ConsoleOutputWsMessage> consoleWs;
    private volatile boolean isRunning = false;

    @Override public void initialize(URL url, ResourceBundle rb) {

        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: monospace");

        // Add visual indicator when autoscroll is unchecked
        autoscrollChk.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                // Checked - normal appearance
                autoscrollChk.setStyle("");
            } else {
                // Unchecked - red bold text to make it obvious
                autoscrollChk.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        });
        // Set initial style based on current state
        if (!autoscrollChk.isSelected()) {
            autoscrollChk.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }

        // Detect ANY user scroll interaction - mouse wheel, touchpad, scrollbar drag
        textArea.setOnScroll(event -> {
            // Any scroll event disables autoscroll
            if (autoscrollChk.isSelected()) {
                autoscrollChk.setSelected(false);
            }
        });

        textArea.sceneProperty().addListener((o,ov,nv)->{
            if(nv==null)return;
            Platform.runLater(()->{
                vBar =(ScrollBar)textArea.lookup(".scroll-bar:vertical");
                if(vBar!=null){
                    vBar.valueProperty().addListener(this::scrollbarChanged);

                    // Detect when user clicks anywhere on scrollbar
                    vBar.setOnMousePressed(event -> {
                        if (autoscrollChk.isSelected()) {
                            autoscrollChk.setSelected(false);
                        }
                    });

                    // Detect when user drags the scrollbar
                    vBar.setOnMouseDragged(event -> {
                        if (autoscrollChk.isSelected()) {
                            autoscrollChk.setSelected(false);
                        }
                    });
                }
            });
        });

        StringConverter<Integer> cv = new IntegerStringConverter();
        TextFormatter<Integer> tf = new TextFormatter<>(cv, maxLines,
                c->c.getControlNewText().matches("\\d*")?c:null);
        maxLinesField.setTextFormatter(tf);

        tf.valueProperty().addListener((o,ov,nv)->{
            maxLines = clamp(nv==null?MIN_LINES:nv);
            maxLinesField.setText(String.valueOf(maxLines));
            render();
        });
        maxLinesField.focusedProperty().addListener((o,ov,nv)->{
            if(!nv) maxLinesField.setText(String.valueOf(maxLines));
        });

        clearBtn.setOnAction(e->{ textBuf.clear(); render(); });

        // Only start/stop on null <-> non-null transitions, not on every status change
        StatusBus.latest().addListener((o,oldS,newS)-> {
            boolean wasConnected = oldS != null;
            boolean isConnected = newS != null;

            // Only act on state transitions
            if (wasConnected != isConnected) {
                java.util.logging.Logger.getLogger(getClass().getPackageName())
                    .log(java.util.logging.Level.FINE,
                         "Console monitor state transition: wasConnected=" + wasConnected +
                         ", isConnected=" + isConnected);
                Platform.runLater(()-> {
                    if (isConnected) start();
                    else stop();
                });
            }
        });
    }

    public void shutdown(){
        stop();
        if (consoleWs != null) {
            consoleWs.close();
        }
        io.shutdownNow();
    }

    private void start(){
        // Prevent starting if already running
        if(isRunning) {
            java.util.logging.Logger.getLogger(getClass().getPackageName())
                .log(java.util.logging.Level.FINE, "Console monitor start() called but already running, ignoring");
            return;
        }

        if(pollTask!=null || (consoleWs != null && consoleWs.isConnected())) {
            java.util.logging.Logger.getLogger(getClass().getPackageName())
                .log(java.util.logging.Level.FINE, "Console monitor start() called but already have active connections, ignoring");
            return;
        }

        java.util.logging.Logger.getLogger(getClass().getPackageName())
            .log(java.util.logging.Level.FINE, "Console monitor starting");
        stop=false; textBuf.clear(); lastUid="ALL";
        synchronized (wsTextLock) {
            wsTextBuffer.setLength(0);
        }

        isRunning = true;

        // Load backlog in background thread to avoid blocking JavaFX thread
        io.submit(this::loadBacklog);

        if (Preferences.use_websockets) {
            startWebSocket();
        } else {
            startStream();
            pollTask = PollCenter.everyMs(Preferences.update_interval_ms, this::poll);
        }
    }

    private void stop(){
        if(!isRunning) {
            java.util.logging.Logger.getLogger(getClass().getPackageName())
                .log(java.util.logging.Level.FINE, "Console monitor stop() called but not running, ignoring");
            return; // Already stopped
        }

        java.util.logging.Logger.getLogger(getClass().getPackageName())
            .log(java.util.logging.Level.FINE, "Console monitor stopping");

        if(pollTask!=null){ pollTask.cancel(true); pollTask=null; }
        if(consoleWs != null) { consoleWs.disconnect(); }
        synchronized (wsTextLock) {
            wsTextBuffer.setLength(0);
        }
        stop=true;
        isRunning = false;
        // Don't clear textBuf - keep last console output visible for users
    }

    private void loadBacklog(){
        try{
            // These HTTP calls run in background thread (io executor)
            String t = svc.consoleOutput(BACKLOG).text();
            String uid = svc.consoleOutputUid().uid();

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                if(!t.isEmpty()) textBuf.addMessage(t);
                lastUid = uid;
                render();

                // Force scroll to bottom on initial load if autoscroll is enabled
                // Multiple runLaters to ensure UI is fully laid out and text is rendered
                if (autoscrollChk.isSelected()) {
                    Platform.runLater(() -> {
                        Platform.runLater(() -> {
                            Platform.runLater(() -> {
                                lastProgrammaticScroll = System.currentTimeMillis();
                                textArea.positionCaret(textArea.getLength());
                                textArea.setScrollTop(Double.MAX_VALUE);
                                if (vBar != null) {
                                    vBar.setValue(vBar.getMax());
                                }
                            });
                        });
                    });
                }
            });
        }catch(Exception ignore){}
    }

    private void startWebSocket(){
        consoleWs = svc.createConsoleOutputWebSocket();

        // Buffer incoming WebSocket messages without immediately updating UI
        consoleWs.addListener(msg -> {
            String text = msg.msg();
            if (text != null && !text.isEmpty()) {
                synchronized (wsTextLock) {
                    wsTextBuffer.append(text);
                }
            }
        });

        consoleWs.connect();

        // Schedule throttled UI updates at the configured interval
        pollTask = PollCenter.everyMs(Preferences.update_interval_ms, () -> {
            String bufferedText;
            synchronized (wsTextLock) {
                if (wsTextBuffer.length() == 0) {
                    return; // Nothing to render
                }
                bufferedText = wsTextBuffer.toString();
                wsTextBuffer.setLength(0);
            }

            Platform.runLater(() -> {
                textBuf.addMessage(bufferedText);
                render();
                lastLine = Instant.now();
            });
        });
    }

    private void startStream(){
        Task<Void> job = new Task<>(){
            @Override protected Void call(){
                try(var br = new BufferedReader(
                        new InputStreamReader(svc.streamConsoleOutput(),
                                StandardCharsets.UTF_8))){
                    StringBuilder chunk = new StringBuilder(CHUNK);
                    for(String ln; !stop && (ln=br.readLine())!=null; ){
                        if(!ln.isBlank())
                            chunk.append(JSON.readTree(ln).path("msg").asText());
                        if(chunk.length()>CHUNK){
                            String out=chunk.toString(); chunk.setLength(0);
                            Platform.runLater(()->{ textBuf.addMessage(out); render(); });
                        }
                    }
                    if(chunk.length()>0)
                        Platform.runLater(()->{ textBuf.addMessage(chunk.toString()); render(); });
                }catch(Exception ignore){}
                return null;
            }
        };
        io.submit(job);
    }

    private void poll(){
        if(stop||Instant.now().minusMillis(Preferences.update_interval_ms).isBefore(lastLine))return;
        try{
            ConsoleOutputUpdate u = svc.consoleOutputUpdate(lastUid);
            if(u.consoleOutputMsgs()!=null){
                StringBuilder sb=new StringBuilder();
                for(Map<String,Object> m:u.consoleOutputMsgs())
                    sb.append((String)m.get("msg"));
                if(sb.length()!=0){ textBuf.addMessage(sb.toString()); render(); }
            }
            lastUid=u.lastMsgUid();
        }catch(Exception ignore){}
    }

    private void render() {

        final boolean wantBottom = autoscrollChk.isSelected();

        int    keepCaret   = textArea.getCaretPosition();
        double keepScrollY = textArea.getScrollTop();

        textArea.replaceText(0, textArea.getLength(), textBuf.tail(maxLines));

        if (wantBottom) {
            lastProgrammaticScroll = System.currentTimeMillis();
            Platform.runLater(() -> {
                lastProgrammaticScroll = System.currentTimeMillis();
                textArea.positionCaret(textArea.getLength());
                textArea.setScrollTop(Double.MAX_VALUE);
                if (vBar != null) vBar.setValue(vBar.getMax());
            });
        } else {
            lastProgrammaticScroll = System.currentTimeMillis();
            textArea.positionCaret(Math.min(keepCaret, textArea.getLength()));
            double y = keepScrollY;
            Platform.runLater(() -> {
                lastProgrammaticScroll = System.currentTimeMillis();
                textArea.setScrollTop(y);
            });
        }

        lastLine = Instant.now();
    }

    private void scrollbarChanged(ObservableValue<? extends Number> obs,
                                  Number oldVal, Number newVal) {

        if (vBar == null) return;

        // If autoscroll is on, check if user scrolled away from bottom
        if (autoscrollChk.isSelected()) {
            // Ignore scroll events that happen within 100ms of programmatic scrolling
            long timeSinceProgrammaticScroll = System.currentTimeMillis() - lastProgrammaticScroll;
            if (timeSinceProgrammaticScroll < 100) {
                return; // Too soon after programmatic scroll, ignore
            }

            // Check if we're not at the bottom anymore (with small tolerance)
            boolean atBottom = (vBar.getMax() - newVal.doubleValue()) < 2.0;

            // If not at bottom, user must have scrolled up - uncheck autoscroll
            if (!atBottom) {
                autoscrollChk.setSelected(false);
            }
        }
    }

    private static int clamp(int v){ return Math.max(MIN_LINES,Math.min(MAX_LINES,v)); }

    private static final class ConsoleTextBuffer {

        private final int hardLimit;
        private final List<String> buf = new ArrayList<>();

        private int line = 0;
        private int pos  = 0;

        private static final String NL  = "\n";
        private static final String CR  = "\r";
        private static final String UP_ONE_LINE = "\u001b[A";  // ESC[A

        ConsoleTextBuffer(int hardLimit) {
            this.hardLimit = Math.max(hardLimit, 0);
        }

        void clear() {
            buf.clear();
            line = pos = 0;
        }

        void addMessage(String msg) {
            while (!msg.isEmpty()) {
                // Find next control sequence
                int nlIdx = msg.indexOf(NL);
                int crIdx = msg.indexOf(CR);
                int upIdx = msg.indexOf(UP_ONE_LINE);

                int next = minPos(nlIdx, crIdx, upIdx);
                if (next < 0) next = msg.length();

                // ----------------------------------------------------
                //  FRAGMENT: plain text until control char
                // ----------------------------------------------------
                if (next != 0) {
                    String frag = msg.substring(0, next);
                    msg = msg.substring(next);

                    // Ensure we have at least one line
                    if (buf.isEmpty()) {
                        buf.add("");
                    }

                    ensureLineExists(line);

                    // Extend current line with spaces if cursor is past EOL
                    int lineLen = buf.get(line).length();
                    if (lineLen < pos) {
                        buf.set(line, buf.get(line) + " ".repeat(pos - lineLen));
                    }

                    // Insert/overwrite fragment at current position
                    String currentLine = buf.get(line);
                    String before = currentLine.substring(0, Math.min(pos, currentLine.length()));
                    String after = currentLine.substring(Math.min(pos + frag.length(), currentLine.length()));
                    buf.set(line, before + frag + after);

                    pos += frag.length();
                    continue;
                }

                // ----------------------------------------------------
                //  CONTROL SEQUENCES
                // ----------------------------------------------------

                if (nlIdx == 0) {
                    // Newline: move to next line
                    line++;
                    if (line >= buf.size()) {
                        buf.add("");
                    }
                    pos = 0;
                    msg = msg.substring(NL.length());
                }
                else if (crIdx == 0) {
                    // Carriage return: move to beginning of current line
                    pos = 0;
                    msg = msg.substring(CR.length());
                }
                else if (upIdx == 0) {
                    // Move up one line
                    if (line > 0) {
                        line--;
                    }
                    pos = 0;
                    msg = msg.substring(UP_ONE_LINE.length());
                }
                else {
                    // Shouldn't happen, but handle gracefully
                    if (!msg.isEmpty()) {
                        ensureLineExists(line);
                        buf.set(line, buf.get(line) + msg.charAt(0));
                        pos++;
                        msg = msg.substring(1);
                    }
                }
            }
            trim();
        }

        String tail(int n) {
            if (n <= 0) return "";

            // Remove trailing empty line if present
            int visible = buf.size();
            if (visible > 0 && buf.get(visible - 1).isEmpty()) {
                visible--;
            }

            int start = Math.max(0, visible - n);
            StringBuilder out = new StringBuilder();
            for (int i = start; i < visible; i++) {
                out.append(buf.get(i));
                if (i + 1 < visible) out.append('\n');
            }
            return out.toString();
        }

        private void ensureLineExists(int idx) {
            while (buf.size() <= idx) {
                buf.add("");
            }
        }

        private void trim() {
            // Keep some buffer beyond hardLimit to avoid constant trimming
            int maxAllowed = hardLimit + 100;
            while (buf.size() > maxAllowed) {
                buf.remove(0);
                line = Math.max(0, line - 1);
            }
        }

        private static int minPos(int... p) {
            int m = Integer.MAX_VALUE;
            for (int v : p) {
                if (v >= 0 && v < m) m = v;
            }
            return m == Integer.MAX_VALUE ? -1 : m;
        }
    }
}
