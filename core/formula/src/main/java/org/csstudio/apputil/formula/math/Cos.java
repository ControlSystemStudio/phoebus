package org.csstudio.apputil.formula.math;

import java.util.List;

/**
 * Cos class see @Math cos function
 */
public class Cos extends OneArgMathFunction
{
	 /**
     * Constructor
     */
	public Cos()
    {
        super("cos", "Cosine", Math::cos);
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("angle");
    }
}