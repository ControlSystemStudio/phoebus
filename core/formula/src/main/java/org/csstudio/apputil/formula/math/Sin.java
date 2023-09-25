package org.csstudio.apputil.formula.math;

import java.util.List;

/**
 * Sin class see @Math sin function
 */
public class Sin extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public Sin()
    {
        super("sin", "Sine", Math::sin);
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("angle");
    }
}