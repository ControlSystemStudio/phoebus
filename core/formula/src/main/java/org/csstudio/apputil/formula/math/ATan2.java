package org.csstudio.apputil.formula.math;

import java.util.List;

/**
 * ATan2 class see @Math atan2 function
 */
public class ATan2 extends TwoArgMathFunction
{
    /**
     * Constructor
     */
	public ATan2()
    {
        super("atan2", "Inverse tangent (y, x)", Math::atan2);
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("y", "x");
    }
}