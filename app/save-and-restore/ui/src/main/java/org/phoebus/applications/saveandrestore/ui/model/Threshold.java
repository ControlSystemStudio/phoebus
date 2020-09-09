/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.model;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 *
 * <code>Threshold</code> represents threshold values for a pv. It provides two values, one for positive threshold and
 * one for negative. When comparing two values using this threshold the values are equal if the difference between the
 * first and the second value is less than positive threshold and more than negative threshold</code>. The generic
 * parameter of this class has to be one of the primitive types wrappers: {@link Byte}, {@link Short}, {@link Integer},
 * {@link Long}, {@link Float}, {@link Double}.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 * @param <T> the type of the number that this threshold contains
 */
public class Threshold<T extends Number> implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(Threshold.class.getName());
    private static final long serialVersionUID = 7839497629386640415L;

    // private static final String[] FUNCTIONS = new String[] { "PI", "E", "abs(", "cos(", "acos(", "sin(", "asin(",
    // "tan(", "atan(", "exp(", "log(", "max(", "min(", "pow(", "sqrt(" };
    private static final String BASE = "base";
    private static final String VALUE = "value";
    private static final ScriptEngine EVALUATOR = new ScriptEngineManager().getEngineByName("JavaScript");

    @SuppressWarnings("unchecked")
    private static <U extends Number> U toNegativeValue(U n) {
        if (n instanceof Byte) {
            return (U) Byte.valueOf((byte) -n.byteValue());
        } else if (n instanceof Short) {
            return (U) Short.valueOf((short) -n.shortValue());
        } else if (n instanceof Integer) {
            return (U) Integer.valueOf(-n.intValue());
        } else if (n instanceof Long) {
            return (U) Long.valueOf(-n.longValue());
        } else if (n instanceof Float) {
            return (U) Float.valueOf(-n.floatValue());
        } else if (n instanceof Double) {
            return (U) Double.valueOf(-n.doubleValue());
        }
        throw new IllegalArgumentException(String.format("Cannot negate the value %s.", String.valueOf(n)));
    }

    private static void checkValue(Number n, boolean positive) {
        if (!(n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long || n instanceof Float
            || n instanceof Double)) {
            throw new IllegalArgumentException(
                String.format("The value %s is not one of the primitive type wrappers.", String.valueOf(n)));
        }
        if (positive && n.doubleValue() < 0.) {
            throw new IllegalArgumentException(
                String.format("The value %s should be non negative.", String.valueOf(n)));
        } else if (!positive && n.doubleValue() > 0.) {
            throw new IllegalArgumentException(
                String.format("The value %s should be non positive.", String.valueOf(n)));
        }
    }

    private final T positiveThreshold;
    private final T negativeThreshold;
    private final String function;
    private final boolean isBooleanFunction;
    private boolean logError = true;
    private boolean isMalformed = false;

    /**
     * Construct a new threshold, where the positive and negative threshold value have the same absolute value.
     *
     * @param absoluteThresholdValue the positive threshold value
     */
    public Threshold(T absoluteThresholdValue) {
        checkValue(absoluteThresholdValue, true);
        this.positiveThreshold = absoluteThresholdValue;
        this.negativeThreshold = toNegativeValue(absoluteThresholdValue);
        this.function = null;
        this.isBooleanFunction = false;
    }

    /**
     * Constructs a new threshold.
     *
     * @param positiveThreshold the positive threshold value (always equal or greater than 0)
     * @param negativeThreshold the negative threshold value (always equal or smaller than 0)
     */
    public Threshold(T positiveThreshold, T negativeThreshold) {
        checkValue(positiveThreshold, true);
        checkValue(negativeThreshold, false);
        this.positiveThreshold = positiveThreshold;
        this.negativeThreshold = negativeThreshold;
        this.function = null;
        this.isBooleanFunction = false;
    }

    /**
     * Constructs a new Threshold object from the given input parameter. If the input parameter can be parsed to long,
     * the threshold will be of long type, if it can be parsed to double it will be of double type, otherwise it will be
     * a function type threshold.
     * <p>
     * The function can either be a function that returns double or boolean. In case of a double type function, the
     * function must accept a value <code>x</code> (or <code>y</code>) and calculate the threshold at this value (base).
     * The difference between value and base, will be compared to the threshold. For example: <code>4*x+12</code>. If
     * the function is boolean type it must accept <code>value</code> and <code>base</code>. The function should
     * evaluate if the difference between value and base is acceptable and return true if yes, or false if not.
     * </p>
     *
     * @param thresholdDefinition the definition of the threshold (number of function)
     */
    @SuppressWarnings("unchecked")
    public Threshold(String thresholdDefinition) {
        if (thresholdDefinition == null || thresholdDefinition.isEmpty()) {
            this.positiveThreshold = (T) Double.valueOf(0);
            this.negativeThreshold = positiveThreshold;
            this.function = null;
            this.isBooleanFunction = false;
        } else {
            Long l = null;
            Double d = null;
            try {
                l = Long.parseLong(thresholdDefinition);
            } catch (NumberFormatException e) {
                // ignore
                try {
                    d = Double.parseDouble(thresholdDefinition);
                } catch (NumberFormatException f) {
                    // ignore
                }
            }
            if (l != null) {
                this.positiveThreshold = (T) l;
                this.negativeThreshold = toNegativeValue(this.positiveThreshold);
            } else if (d != null) {
                this.positiveThreshold = (T) d;
                this.negativeThreshold = toNegativeValue(this.positiveThreshold);
            } else {
                this.positiveThreshold = null;
                this.negativeThreshold = null;
            }
            this.function = thresholdDefinition;
            this.isBooleanFunction = this.function.indexOf("base") > -1 && this.function.indexOf("value") > -1;
        }
    }

    /**
     * Returns the negative threshold value. The evaluated value has the be greater or equal than this to be accepted.
     *
     * @return the negative threshold value
     */
    public T getNegativeThreshold() {
        return negativeThreshold;
    }

    /**
     * Returns the positive threshold value. The evaluated value has the be smaller or equal than this to be accepted.
     *
     * @return the positive threshold value
     */
    public T getPositiveThreshold() {
        return positiveThreshold;
    }

    /**
     * Returns the function that is used to calculate if the value is within threshold. This function is only used if
     * the positive and negative thresholds are not defined.
     *
     * @return the function
     */
    public String getFunction() {
        return function;
    }

    /**
     * Checks if the given value is within the threshold limits. The value is within limits if the difference between
     * value and base is greater than or equal to {@link #getNegativeThreshold()} and smaller than or equal to
     * {@link #getPositiveThreshold()}. In case that the threshold values are defined by a function (@see
     * {@link #getFunction()}), the function is used to calculate whether the value is within limits.
     *
     * @param value the value to compare to the thresholds
     * @return true if the value is within threshold limits or false otherwise
     */
    public boolean isWithinThreshold(T value, T base) {
        if (isMalformed) {
            return false;
        }
        if (positiveThreshold != null && negativeThreshold != null) {
            if (value instanceof Long) {
                long l = value.longValue() - base.longValue();
                return l >= negativeThreshold.longValue() && l <= positiveThreshold.longValue();
            } else {
                double v = value.doubleValue() - base.doubleValue();
                return v >= negativeThreshold.doubleValue() && v <= positiveThreshold.doubleValue();
            }
        } else if (function != null && !function.isEmpty()) {
            try {
                if (isBooleanFunction) {
                    Bindings b = new SimpleBindings();
                    b.put(BASE, base);
                    b.put(VALUE, value);
                    return (Boolean) EVALUATOR.eval(function, b);
                } else {
                    // value to string is good enough here
                    Bindings b = new SimpleBindings();
                    b.put(BASE, base);
                    b.put("x", base);
                    double threshold = Math.abs(((Number) EVALUATOR.eval(function, b)).doubleValue());
                    double v = Math.abs(value.doubleValue() - base.doubleValue());
                    return v <= threshold;
                }
            } catch (ScriptException | ClassCastException | NullPointerException e) {
                isMalformed = true;
                if (logError) {
                    LOGGER.log(Level.WARNING, String.format("Threshold function %s cannot be evaluated.", function),
                        (Throwable) e);
                    logError = false;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * Test if this threshold is defined in a way that it can be evaluated. If it is, the method returns true, otherwise
     * it returns false. Note that if the method returns false it does not necessary means that the definition is not
     * correct, but is only an indication that it should be double checked.
     *
     * @return true if the definition is acceptable or false if there is a potential error
     */
    @SuppressWarnings("unchecked")
    public boolean test() {
        boolean log = logError;
        logError = false;
        isWithinThreshold((T) Long.valueOf(1), (T) Long.valueOf(0));
        logError = log;
        return !isMalformed;
    }
}
