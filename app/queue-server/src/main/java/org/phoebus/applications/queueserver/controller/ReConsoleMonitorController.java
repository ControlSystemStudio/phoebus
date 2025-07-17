package org.phoebus.applications.queueserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.applications.queueserver.api.ConsoleOutputUpdate;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

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
    private boolean ignoreScroll = false;

    @Override public void initialize(URL url, ResourceBundle rb) {

        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: monospace");

        textArea.sceneProperty().addListener((o,ov,nv)->{
            if(nv==null)return;
            Platform.runLater(()->{
                vBar =(ScrollBar)textArea.lookup(".scroll-bar:vertical");
                if(vBar!=null)vBar.valueProperty().addListener(this::scrollbarChanged);
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

        StatusBus.latest().addListener((o,oldS,newS)->
                Platform.runLater(()-> { if(newS!=null) start(); else stop(); }));
    }

    public void shutdown(){
        stop();
        io.shutdownNow();
    }

    private void start(){
        if(pollTask!=null) return;
        stop=false; textBuf.clear(); lastUid="ALL";
        loadBacklog(); startStream();
        pollTask = PollCenter.every(1,this::poll);
    }

    private void stop(){
        if(pollTask!=null){ pollTask.cancel(true); pollTask=null; }
        stop=true;
    }

    private void loadBacklog(){
        try{
            String t = svc.consoleOutput(BACKLOG).text();
            if(!t.isEmpty()) textBuf.addMessage(t);
            lastUid = svc.consoleOutputUid().uid();
            render();
        }catch(Exception ignore){}
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
        if(stop||Instant.now().minusMillis(1000).isBefore(lastLine))return;
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

        ignoreScroll = true;
        int    keepCaret   = textArea.getCaretPosition();
        double keepScrollY = textArea.getScrollTop();

        textArea.replaceText(0, textArea.getLength(), textBuf.tail(maxLines));

        if (wantBottom) {
            Platform.runLater(() -> {
                textArea.positionCaret(textArea.getLength());
                textArea.setScrollTop(Double.MAX_VALUE);
                if (vBar != null) vBar.setValue(vBar.getMax());
            });
        } else {
            textArea.positionCaret(Math.min(keepCaret, textArea.getLength()));
            double y = keepScrollY;
            Platform.runLater(() -> textArea.setScrollTop(y));
        }


        ignoreScroll = false;
        lastLine = Instant.now();
    }

    private void scrollbarChanged(ObservableValue<? extends Number> obs,
                                  Number oldVal, Number newVal) {

        if (ignoreScroll) return;

        if (vBar == null) return;

        boolean atBottom = (vBar.getMax() - newVal.doubleValue()) < 1.0;

        if (!atBottom && autoscrollChk.isSelected()) {
            autoscrollChk.setSelected(false);
        }
    }

    private static int clamp(int v){ return Math.max(MIN_LINES,Math.min(MAX_LINES,v)); }

    private static final class ConsoleTextBuffer {

        private final int hardLimit;
        private final List<String> buf = new ArrayList<>();

        private int line = 0;
        private int pos  = 0;

        private boolean progressActive = false;   // overwriting a tqdm bar
        private String  lastVisualLine = "";      // for duplicate-collapse

        private static final String NL  = "\n";
        private static final String CR  = "\r";
        private static final char   ESC = 0x1B;
        private static final String LOG_PREFIXES = "IWEDE";   // info/warn/error/debug/extra

        ConsoleTextBuffer(int hardLimit) {
            this.hardLimit = Math.max(hardLimit, 0);
        }

        void clear() {
            buf.clear();
            line = pos = 0;
            progressActive = false;
            lastVisualLine = "";
        }

        void addMessage(String msg) {
            while (!msg.isEmpty()) {

                /* next control-char position */
                int next = minPos(msg.indexOf(NL), msg.indexOf(CR), msg.indexOf(ESC));
                if (next < 0) next = msg.length();

                /* ---------------------------------------------------- *
                 *  FRAGMENT: plain text until control char             *
                 * ---------------------------------------------------- */
                if (next != 0) {
                    String frag = msg.substring(0, next);
                    msg = msg.substring(next);

                    /* are we switching from progress bar → normal log? */
                    if (progressActive &&
                            (frag.startsWith("[") || frag.startsWith("TEST") ||
                                    frag.startsWith("Returning") || frag.startsWith("history")))
                    {
                        newline();               // finish bar line
                        progressActive = false;
                    }

                    ensureLineExists(line);

                    /* pad if cursor past EOL */
                    if (pos > buf.get(line).length()) {
                        buf.set(line, buf.get(line) + " ".repeat(pos - buf.get(line).length()));
                    }

                    /* overwrite / extend */
                    StringBuilder sb = new StringBuilder(buf.get(line));
                    if (pos + frag.length() > sb.length()) {
                        sb.setLength(pos);
                        sb.append(frag);
                    } else {
                        sb.replace(pos, pos + frag.length(), frag);
                    }
                    buf.set(line, sb.toString());
                    pos += frag.length();

                    /* flag if this fragment looks like a tqdm bar        *
                     * (contains “%|” and the typical frame pattern)      */
                    if (frag.contains("%|")) progressActive = true;

                    continue;
                }

                /* ---------------------------------------------------- *
                 *  CONTROL CHAR                                         *
                 * ---------------------------------------------------- */
                char c = msg.charAt(0);

                if (c == '\n') {                // LF: finish logical line
                    newline();
                    progressActive = false;     // tqdm ends with a real LF
                    msg = msg.substring(1);
                }
                else if (c == '\r') {           // CR: return to start of SAME line
                    pos = 0;
                    msg = msg.substring(1);
                }
                else if (c == ESC) {            // ESC [ n A   (cursor-up)
                    int idx = 1;
                    if (idx < msg.length() && msg.charAt(idx) == '[') {
                        idx++;
                        int startDigits = idx;
                        while (idx < msg.length() && Character.isDigit(msg.charAt(idx))) idx++;
                        if (idx < msg.length() && msg.charAt(idx) == 'A') {
                            int nUp = (idx == startDigits) ? 1
                                    : Math.max(1, Integer.parseInt(msg.substring(startDigits, idx)));
                            line = Math.max(0, line - nUp);
                            pos  = 0;
                            ensureLineExists(line);
                            msg = msg.substring(idx + 1);
                            continue;
                        }
                    }
                    /* unknown sequence → treat ESC literally */
                    ensureLineExists(line);
                    buf.set(line, buf.get(line) + c);
                    pos++;
                    msg = msg.substring(1);
                }
            }
            trim();
        }

        String tail(int n) {
            if (n <= 0) return "";
            boolean sentinel = !buf.isEmpty() && buf.get(buf.size() - 1).isEmpty();
            int visible = buf.size() - (sentinel ? 1 : 0);

            int start = Math.max(0, visible - n);
            StringBuilder out = new StringBuilder();
            for (int i = start; i < visible; i++) {
                out.append(buf.get(i));
                if (i + 1 < visible) out.append('\n');
            }
            return out.toString();
        }

        private void ensureLineExists(int idx) {
            while (buf.size() <= idx) buf.add("");
        }

        private void newline() {
            /* avoid storing duplicate visual lines (stream + poll) */
            String justFinished = buf.get(line);
            if (!justFinished.equals(lastVisualLine)) {
                lastVisualLine = justFinished;
                line++;
                pos = 0;
                ensureLineExists(line);
            } else {
                /* discard duplicate; stay on existing last line        */
                pos = buf.get(line).length();
            }
        }

        private void trim() {
            boolean sentinel = !buf.isEmpty() && buf.get(buf.size() - 1).isEmpty();
            int quota = hardLimit + (sentinel ? 1 : 0);
            while (buf.size() > quota) buf.remove(0);
            if (line >= buf.size()) line = buf.size() - 1;
        }

        private static int minPos(int... p) {
            int m = Integer.MAX_VALUE;
            for (int v : p) if (v >= 0 && v < m) m = v;
            return m == Integer.MAX_VALUE ? -1 : m;
        }
    }
}
