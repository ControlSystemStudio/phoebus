/*******************************************************************************
 * Copyright (c) 2014-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javafx.util.Pair;
import org.csstudio.javafx.rtplot.internal.util.Log10;

/** Helper for creating tick marks on a logarithmic scale.
 *
 *  <p>Computes tick positions and formats tick labels.
 *  Does not perform actual drawing.
 *
 *  <p>Three layout strategies are applied in order of preference:
 *  <ol>
 *   <li>If there are more decades than fit on screen, the decade list is
 *       thinned geometrically.  All decades still get a prominent tick line;
 *       only the thinned subset receives a text label.</li>
 *   <li>If all decades fit, sub-decade values (2×10^n … 9×10^n) are added
 *       as minor ticks — or promoted to labeled major ticks when the axis
 *       has enough room for all of them.</li>
 *   <li>When the range spans less than one decade a linear tick spacing is
 *       used as a fallback.</li>
 *  </ol>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LogTicks extends LinearTicks
{
    /** Set to {@code true} by {@link #buildThinnedDecadeTicks} to signal that
     *  the labeled subset has already been chosen symmetrically by
     *  {@link #thinDecades}.  When {@code true}, the axis painter must not
     *  apply a second visibility-culling pass, which would destroy the
     *  intentional spacing.
     */
    private boolean thinned = false;

    /** @return {@code true} when the tick-label set was pre-thinned by
     *  {@link #thinDecades} and must not be culled again by the caller. */
    public boolean isThinned() { return thinned; }

    public LogTicks()
    {
        num_fmt          = createExponentialFormat(2);
        detailed_num_fmt = createExponentialFormat(3);
    }

    /** {@inheritDoc}
     *
     *  <p>Clamps non-positive or non-finite bounds to valid values for a
     *  logarithmic scale and ensures {@code low < high}.
     */
    @Override
    public Pair<Double, Double> adjustRange(Double low, Double high)
    {
        if (!Double.isFinite(low) || low <= 0.0)
            low = Math.ulp(0.0);
        if (!Double.isFinite(high))
            high = Double.MAX_VALUE;
        if (Math.abs(high - low) < 3 * Math.ulp(low))
            high = low + 3 * Math.ulp(low);
        if (high < low)
        {
            final double swap = low;
            low  = high;
            high = swap;
        }
        return new Pair<>(low, high);
    }

    /** {@inheritDoc} */
    @Override
    public void compute(Double low, Double high, final Graphics2D gc, final int screen_width)
    {
        final Pair<Double, Double> adjusted = adjustRange(low, high);
        final double adjLow  = adjusted.getKey();
        final double adjHigh = adjusted.getValue();
        if (adjLow != low  ||  adjHigh != high)
            logger.log(Level.WARNING,
                "Invalid value range for a logarithmic scale {0,number,#.###############E0} ... " +
                "{1,number,#.###############E0}. Adjusting the range to " +
                "{2,number,#.###############E0} ... {3,number,#.###############E0}.",
                new Object[] { low, high, adjLow, adjHigh });
        low  = adjLow;
        high = adjHigh;

        logger.log(Level.FINE, "Compute log ticks, width {0}, for {1} - {2}",
                new Object[] { screen_width, low, high });

        final double lowExp  = Log10.log10(low);
        final double highExp = Log10.log10(high);

        num_fmt       = createExponentialFormat(0);
        zero_threshold = 0.0;
        thinned        = false;

        final int lowExpInt  = (int) Math.floor(lowExp);
        final int highExpInt = (int) Math.ceil(highExp);
        final int numFits        = numLabelsThatFit(gc.getFontMetrics(), screen_width, lowExpInt, highExpInt);
        final List<Integer> decadeExps      = collectDecadeExponents(lowExp, highExp, low, high);
        final List<Double>  subDecadeValues = collectSubDecadeValues(lowExp, highExp, low, high);

        final List<MajorTick<Double>> major_ticks = new ArrayList<>();
        final List<MinorTick<Double>> minor_ticks = new ArrayList<>();

        if (decadeExps.size() >= 2  &&  decadeExps.size() > numFits)
            buildThinnedDecadeTicks(decadeExps, subDecadeValues, numFits, major_ticks, minor_ticks);
        else if (decadeExps.size() >= 2)
            buildLabeledDecadeTicks(decadeExps, subDecadeValues, numFits, major_ticks, minor_ticks);
        else
            buildLinearFallbackTicks(low, high, numFits, major_ticks);

        if (major_ticks.size() < 2)
        {   // Fallback: ensure range endpoints are always visible.
            // Do NOT reset num_fmt to precision 17 (as LinearTicks does)
            // because log-scale boundaries are clean powers of ten.
            major_ticks.add(0, new MajorTick<>(low,  format(low)));
            major_ticks.add(   new MajorTick<>(high, format(high)));
        }

        // Apply user-specified label format override if set.
        final NumberFormat override = getLabelFormatOverride();
        if (override != null)
            relabelTicks(major_ticks, override);

        this.major_ticks = major_ticks;
        this.minor_ticks = minor_ticks;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** @return Number of tick labels that fit in {@code screenPixels}.
     *
     *  <p>Labels on a log-scale axis are drawn rotated 90° so their physical
     *  extent along the axis is their string width, not {@link FontMetrics#getHeight()}.
     *  Using height (≈14 px) instead of a typical string width (≈30 px for
     *  {@code "1E10"}) overestimates by 2–3×, causing thinDecades to keep too
     *  many labels and producing visual overlap.
     *
     *  <p>This method measures the widest of the bottom and top decade labels
     *  and uses that as the slot size.
     *
     *  @param metrics       Font metrics for the current scale font
     *  @param screenPixels  Axis length in pixels
     *  @param lowExpInt     Floor of {@code log10(low)}
     *  @param highExpInt    Ceil  of {@code log10(high)}
     */
    private int numLabelsThatFit(final FontMetrics metrics, final int screenPixels,
            final int lowExpInt, final int highExpInt)
    {
        final int labelSize;
        if (isPerpendicularTickLabels())
            // Horizontal labels: each occupies one font-height along the axis.
            labelSize = Math.max(1, metrics.getHeight());
        else
        {
            // Vertical (rotated) labels: each occupies its string width along the axis.
            final NumberFormat fmt = createExponentialFormat(0);
            final int w1 = metrics.stringWidth(fmt.format(Log10.pow10(lowExpInt)));
            final int w2 = metrics.stringWidth(fmt.format(Log10.pow10(highExpInt)));
            labelSize = Math.max(1, Math.max(w1, w2));
        }
        return Math.max(2, screenPixels * FILL_PERCENTAGE / 100 / labelSize);
    }

    /** Collect the integer exponents for all powers of 10 within [low, high].
     *
     *  @param lowExp  {@code log10(low)}
     *  @param highExp {@code log10(high)}
     *  @param low     Range lower bound
     *  @param high    Range upper bound
     *  @return Sorted list of exponents {@code n} such that {@code 10^n} is in [low, high]
     */
    private List<Integer> collectDecadeExponents(final double lowExp, final double highExp,
            final double low, final double high)
    {
        final List<Integer> result = new ArrayList<>();
        for (int exp = (int) Math.floor(lowExp);  exp <= (int) Math.ceil(highExp);  exp++)
        {
            final double val = Log10.pow10(exp);
            if (val >= low  &&  val <= high)
                result.add(exp);
        }
        return result;
    }

    /** Collect sub-decade values {@code i × 10^n} for {@code i = 1…9} within [low, high].
     *
     *  <p>The {@code i = 1} values are exact decades and are included so that
     *  callers receive the complete set of candidate tick positions.  Callers
     *  that must not double-count decades can use {@link #isNotExactDecade}.
     *
     *  @param lowExp  {@code log10(low)}
     *  @param highExp {@code log10(high)}
     *  @param low     Range lower bound
     *  @param high    Range upper bound
     *  @return Values in [low, high] of the form {@code i × 10^n}, {@code i = 1…9}
     */
    private List<Double> collectSubDecadeValues(final double lowExp, final double highExp,
            final double low, final double high)
    {
        final List<Double> result = new LinkedList<>();
        for (int exp = (int) Math.floor(lowExp);  exp <= (int) Math.ceil(highExp);  exp++)
        {
            final double base = Log10.pow10(exp);
            for (int i = 1; i < 10; i++)
            {
                final double val = i * base;
                if (val >= low  &&  val <= high)
                    result.add(val);
            }
        }
        return result;
    }

    /** Select up to {@code maxCount} evenly-spaced entries from {@code decades},
     *  always including the first and last entry.
     *
     *  <p>The previous halving approach could not preserve the true midpoint of
     *  a range: e.g., for exponents [0…10] with maxCount=3 it returned {0,8,10}
     *  (or after the visibility pass: 0, 4, 10) instead of the symmetrical
     *  {0, 5, 10}.  Evenly-spaced index selection avoids that bias.
     *
     *  @param decades  Full sorted list of decade exponents
     *  @param maxCount Maximum number of entries to select
     *  @return Selected list with at most {@code maxCount} entries
     */
    private List<Integer> thinDecades(final List<Integer> decades, final int maxCount)
    {
        final int n = decades.size();
        if (n <= maxCount)
            return new ArrayList<>(decades);

        final List<Integer> result = new ArrayList<>(maxCount);
        for (int i = 0; i < maxCount; i++)
        {
            // Map slot i uniformly across the index range [0, n-1].
            final int idx = (int) Math.round((double) i * (n - 1) / (maxCount - 1));
            result.add(decades.get(idx));
        }
        return result;
    }

    /** @return {@code true} when {@code val} is not an exact power of ten. */
    private static boolean isNotExactDecade(final double val)
    {
        final double log = Log10.log10(val);
        return Math.abs(log - Math.round(log)) > 1e-9;
    }

    /** Build ticks for the case where there are more decades than fit on screen.
     *
     *  <p>The labeled set is thinned to {@code numFits}, but every decade still
     *  receives a prominent (long) tick line.  Decades that lost their label
     *  are emitted as major ticks with an empty label string so they remain
     *  visually distinct from the shorter minor marks.
     *
     *  @param decadeExps      All decade exponents in range
     *  @param subDecadeValues Sub-decade values (2×10^n … 9×10^n) in range
     *  @param numFits         Number of labels that fit
     *  @param major_ticks     Output: major tick list to populate
     *  @param minor_ticks     Output: minor tick list to populate
     */
    private void buildThinnedDecadeTicks(
            final List<Integer> decadeExps, final List<Double> subDecadeValues,
            final int numFits,
            final List<MajorTick<Double>> major_ticks, final List<MinorTick<Double>> minor_ticks)
    {
        num_fmt = createExponentialFormat(0);
        thinned = true;   // caller must not apply a second visibility pass

        final List<Integer> labeled    = thinDecades(decadeExps, numFits);
        final Set<Integer>  labeledSet = new HashSet<>(labeled);

        for (int exp : decadeExps)
        {
            final double val   = Log10.pow10(exp);
            final String label = labeledSet.contains(exp) ? format(val) : "";
            major_ticks.add(new MajorTick<>(val, label));
        }

        // Sub-decade values (2×10^n … 9×10^n) become minor ticks.
        // Exact decades are already major ticks above; skip them here.
        for (double val : subDecadeValues)
            if (isNotExactDecade(val))
                minor_ticks.add(new MinorTick<>(val));
    }

    /** Build ticks for the case where all decades fit within the available space.
     *
     *  <p>When all sub-decade values also fit, they are promoted to labeled
     *  major ticks so a tall tank shows every significant value.  Otherwise
     *  they remain as short minor marks.
     *
     *  @param decadeExps      All decade exponents in range
     *  @param subDecadeValues Sub-decade values (i×10^n, i=1…9) in range
     *  @param numFits         Number of labels that fit
     *  @param major_ticks     Output: major tick list to populate
     *  @param minor_ticks     Output: minor tick list to populate
     */
    private void buildLabeledDecadeTicks(
            final List<Integer> decadeExps, final List<Double> subDecadeValues,
            final int numFits,
            final List<MajorTick<Double>> major_ticks, final List<MinorTick<Double>> minor_ticks)
    {
        num_fmt = createExponentialFormat(0);

        for (int exp : decadeExps)
        {
            final double val = Log10.pow10(exp);
            major_ticks.add(new MajorTick<>(val, format(val)));
        }

        final boolean allFit = decadeExps.size() + subDecadeValues.size() <= numFits;
        if (allFit)
        {
            // Promote sub-decade values to labeled major ticks.
            // Filter exact decades (already added above) to avoid duplicates.
            for (double val : subDecadeValues)
                if (isNotExactDecade(val))
                    major_ticks.add(new MajorTick<>(val, format(val)));
            major_ticks.sort((a, b) -> Double.compare(a.getValue(), b.getValue()));
        }
        else
        {
            // Not enough room to label everything; keep sub-decade values as
            // anonymous minor marks.  Include i=1 values so the minor line
            // appears at every decade position before the labeled major tick.
            for (double val : subDecadeValues)
                minor_ticks.add(new MinorTick<>(val));
        }
    }

    /** Build ticks using a linear spacing for sub-decade ranges.
     *
     *  <p>Used as a fallback when the range spans less than one decade and
     *  the logarithmic decade strategy would produce fewer than two ticks.
     *  The step size is halved iteratively until enough ticks are present.
     *
     *  <p>On arithmetic overflow or insufficient double precision, the method
     *  returns without adding any ticks; the caller will then fall back to
     *  showing just the range endpoints.
     *
     *  @param low         Range lower bound
     *  @param high        Range upper bound
     *  @param numFits     Target number of ticks
     *  @param major_ticks Output: major tick list to populate
     */
    private void buildLinearFallbackTicks(
            final double low, final double high, final int numFits,
            final List<MajorTick<Double>> major_ticks)
    {
        final int    logStep = (int) Math.floor(Math.log10(high - low));
        final double step0   = Log10.pow10(logStep);
        final Set<Double> values = new TreeSet<>();
        double step        = step0;
        int    refinements = 0;

        do
        {
            double v = low - (low % step0) - step;
            if (!Double.isFinite(v)  ||  v + step == v)
                return;  // precision exhausted; let caller add endpoints
            while (v <= high)
            {
                if (v >= low)
                    values.add(v);
                v += step;
            }
            refinements++;
            step /= 2;
        }
        while (values.size() < numFits / 2);

        for (double v : values)
        {
            final int prec       = (int) Math.floor(Math.log10(v)) - logStep + refinements;
            num_fmt              = createExponentialFormat(prec - 1);
            detailed_num_fmt     = createExponentialFormat(prec + 1);
            major_ticks.add(new MajorTick<>(v, format(v)));
        }
    }
}
