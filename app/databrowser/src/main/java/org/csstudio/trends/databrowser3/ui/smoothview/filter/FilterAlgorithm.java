package org.csstudio.trends.databrowser3.ui.smoothview.filter;

/**
 * Interface for filter algorithms.
 *
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public interface FilterAlgorithm {

    double[] applyFilter(double[] signal, int windowSize);
}
