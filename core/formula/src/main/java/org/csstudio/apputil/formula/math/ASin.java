package org.csstudio.apputil.formula.math;

/**
 * ASin class
 */
public class ASin extends OneArgMathFunction
{
	 /**
     * Constructor
     */
	public ASin()
    {
        super("asin", "Inverse sine", Math::asin);
    }
}