package org.csstudio.trends.databrowser3.ui.smoothview.filter;

/**
 * Interface for filter algorithms.
 *
 * @author Mingtao Li
 * @see FilterAlgorithm
 * <p>
 * China Spallation Neutron Sources
 * @since 5.0
 */
public interface FilterAlgorithm {

    double[] applyFilter(double[] signal, int windowSize);
}
