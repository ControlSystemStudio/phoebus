package org.csstudio.display.extra.widgets.linearmeter;

import java.awt.*;
import java.awt.geom.AffineTransform;

import org.csstudio.javafx.rtplot.internal.*;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

import static org.csstudio.javafx.rtplot.internal.util.Log10.log10;


public class LinearMeterScale extends NumericAxis
{
    private int p1x, p1y;
    private double scale;
    private double offset;

    public boolean isHorizontal() {
        return isHorizontal;
    }

    private boolean isHorizontal = true;

    public synchronized void setHorizontal(boolean v){
        this.isHorizontal = v;
    }

    public void computeTicks(Graphics2D gc) { super.computeTicks(gc);}

    public Ticks<Double> getTicks() {
        return ticks;
    }

    public int getTickLength() {
        return TICK_LENGTH;
    }

    /** Create scale with label and listener. */
    public LinearMeterScale(PlotPartListener listener,
                            int width,
                            int height,
                            boolean horizontal,
                            double min,
                            double max,
                            boolean isLogarithmic)
    {
        super("", listener, horizontal, min, max);
        super.setBounds(0, 0, width, height);
        super.setLogarithmic(isLogarithmic);
        isHorizontal = horizontal;
    }

    /** Configure scale layout
     *  @param p1x
     *  @param p1y
     *  @param s
     */
    public void configure(int p1x, int p1y, double s)
    {
        this.p1x = p1x;
        this.p1y = p1y;
        this.scale = s;
        this.offset = this.range.getLow();

        dirty_ticks = true;
        requestLayout();
    }

    @Override
    public int getDesiredPixelSize(Rectangle region, Graphics2D gc)
    {
        // Not used
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void paint(Graphics2D gc, Rectangle plot_bounds)
    {

        if (!isVisible()){
            return;
        }

        Stroke old_width = gc.getStroke();
        Color old_fg = gc.getColor();
        Color foreground = GraphicsUtils.convert(getColor());
        gc.setFont(scale_font);

        super.paint(gc);

        computeTicks(gc);
        
        gc.setColor(foreground);

        //Draw axis
        gc.setColor(foreground);

        // Major tick marks
        gc.setStroke(TICK_STROKE);
        int start_x = this.p1x;
        int start_y = this.p1y;

        if (isHorizontal)
        {
            FontMetrics scale_font_fontMetrics = gc.getFontMetrics(scale_font);

            for (MajorTick<Double> tick : ticks.getMajorTicks()) {
                if (isLogarithmic()) {
                    gc.drawLine((int) ((start_x + this.scale * (log10(tick.getValue()) - log10(offset)) )),
                            (int) ((start_y - 0.5 * TICK_LENGTH)),
                            (int) ((start_x + this.scale * (log10(tick.getValue()) - log10(offset)) )),
                            (int) ((start_y + 0.5 * TICK_LENGTH)));
                    drawTickLabel(gc,
                            (int) ((start_x + this.scale * (log10(tick.getValue()) - log10(offset)) - scale_font_fontMetrics.stringWidth(tick.getLabel())/2)),
                            (int) (start_y + 0.5 * TICK_LENGTH + 2 + Math.round(Math.ceil((72.0 * (scale_font_fontMetrics.getAscent())) / 96.0))),
                            tick.getLabel());
                }
                else {
                    gc.drawLine((int) ((start_x + this.scale * (tick.getValue() - offset) )),
                            (int) ((start_y - 0.5 * TICK_LENGTH)),
                            (int) ((start_x + this.scale * (tick.getValue() - offset) )),
                            (int) ((start_y + 0.5 * TICK_LENGTH)));
                    drawTickLabel(gc,
                            (int) ((start_x + this.scale * (tick.getValue() - offset) - scale_font_fontMetrics.stringWidth(tick.getLabel())/2)),
                            (int) (start_y + 0.5 * TICK_LENGTH + 2 + Math.round(Math.ceil((72.0 * (scale_font_fontMetrics.getAscent())) / 96.0))),
                            tick.getLabel());
                }
            }
        } else {
            for (MajorTick<Double> tick : ticks.getMajorTicks()) {
                if (isLogarithmic()) {
                    gc.drawLine((int) (start_x - 0.5 * TICK_LENGTH),
                                (int) (start_y - this.scale * (log10(tick.getValue()) - log10(offset))),
                                (int) (start_x + 0.5 * TICK_LENGTH),
                                (int) (start_y - this.scale * (log10(tick.getValue()) - log10(offset))));
                    drawTickLabel(gc,
                                  (int) (start_x + 4),
                                  (int) (start_y - this.scale * (log10(tick.getValue()) - log10(offset)) + Math.round((72.0 * scale_font.getSize()) / (96.0 * 2.0))),
                                  tick.getLabel());
                }
                else {
                    gc.drawLine((int) (start_x - 0.5 * TICK_LENGTH),
                            (int) (start_y - this.scale * (tick.getValue() - offset)),
                            (int) (start_x + 0.5 * TICK_LENGTH),
                            (int) (start_y - this.scale * (tick.getValue() - offset)));
                    drawTickLabel(gc,
                            (int) (start_x + 4),
                            (int) (start_y - this.scale * (tick.getValue() - offset) + Math.round((72.0 * scale_font.getSize()) / (96.0 * 2.0))),
                            tick.getLabel());
                }
            }
        }

        gc.setStroke(old_width);

        // Minor tick marks
        if(isHorizontal) {
            for (MinorTick<Double> tick : ticks.getMinorTicks()) {
                gc.drawLine((int) ((start_x + this.scale * (tick.getValue() - offset) )),
                    (int) (start_y - 0.5 * TICK_LENGTH),
                    (int) ((start_x + this.scale * (tick.getValue() - offset) )),
                    (int) (start_y + 0.5 * TICK_LENGTH));
            }
        } else {
            for (MinorTick<Double> tick : ticks.getMinorTicks()) {
                gc.drawLine((int) (start_x - 0.5 * TICK_LENGTH),
                    (int) (start_y - this.scale * (tick.getValue() - offset) ),
                    (int) (start_x + 0.5 * TICK_LENGTH),
                    (int) (start_y - this.scale * (tick.getValue() - offset) ));
            }
        }

        gc.setColor(old_fg);

    }

    private void drawTickLabel(Graphics2D gc,
                               int cx,
                               int cy,
                               String mark)
    {
        gc.setFont(scale_font);

        if(!isHorizontal){
            AffineTransform orig = gc.getTransform();
            gc.translate(cx, cy);
            gc.drawString(mark, 6, 0);
            gc.setTransform(orig);
        } else {
            AffineTransform orig = gc.getTransform();
            gc.translate(cx, cy);
            gc.drawString(mark, 0, 0);
            gc.setTransform(orig);
        }
    }

    @Override
    public void drawTickLabel(Graphics2D gc, Double tick)
    {
        // NOP
    }
}
