package org.csstudio.apputil.formula.math;

import java.util.List;

/**
 * Pow class see @Math pow function
 */
public class Pow extends TwoArgMathFunction
{
	/**
     * Constructor
     */
	public Pow()
    {
        super("pow", "Power (base, exponent)", Math::pow);
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("base", "exponent");
    }
}