/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.apputil.formula.node.AddNode;
import org.csstudio.apputil.formula.node.AndNode;
import org.csstudio.apputil.formula.node.ConstantNode;
import org.csstudio.apputil.formula.node.DivNode;
import org.csstudio.apputil.formula.node.EqualNode;
import org.csstudio.apputil.formula.node.GreaterEqualNode;
import org.csstudio.apputil.formula.node.GreaterThanNode;
import org.csstudio.apputil.formula.node.IfNode;
import org.csstudio.apputil.formula.node.LessEqualNode;
import org.csstudio.apputil.formula.node.LessThanNode;
import org.csstudio.apputil.formula.node.MaxNode;
import org.csstudio.apputil.formula.node.MinNode;
import org.csstudio.apputil.formula.node.MulNode;
import org.csstudio.apputil.formula.node.NotEqualNode;
import org.csstudio.apputil.formula.node.NotNode;
import org.csstudio.apputil.formula.node.OrNode;
import org.csstudio.apputil.formula.node.PwrNode;
import org.csstudio.apputil.formula.node.RndNode;
import org.csstudio.apputil.formula.node.SPIFuncNode;
import org.csstudio.apputil.formula.node.SubNode;
import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.VType;

/** A formula interpreter.
 *
 *  <p>Supported, in descending order of precedence:
 *  <ul>
 *  <li>Numeric constant 3.14, -47; "Text Constants"; named variables
 *  <li>(sub-formula in braces), sqrt(x), ln(x), exp(x),
 *       min(a, b, ...), max(a, b, ...).
 *  <li>*, /, ^
 *  <li>+, -
 *  <li>comparisons <, >, >=, <=, ==, !=
 *  <li>boolean logic !, &, |,  .. ? .. : ..
 *  </ul>
 *
 *
 *  <p>The parser operates in three different modes:
 *
 *  <ul>
 *  <li><code>new Formula("2+6")</code> parses a formula without variables.
 *      The parsed expression may only contain constants.
 *      If the parsed expression contained a variable,
 *      the parser throws an exception.
 *  <li><code>new Formula("2+A+B", new VariableNode("A", 1.0), new VariableNode("B", 2.0))</code> parses a formula with predefined variables.
 *      The parsed expression may contain the provided variables.
 *      If the parsed expression contained an unknown variable,
 *      the parser throws an exception.
 *      Before evaluating the expression, the values of the variables may be changed.
 *  <li><code>new Formula("2+A+B", true)</code> parses a formula with arbitrary variables.
 *      The parser automatically creates variables as they are encountered in the expression.
 *      Before evaluating the formula,
 *      caller needs to query the formula for its automatically determined variables
 *      and set their values.
 *  </ul>
 *
 *  <p>The formula string is parsed into a tree, so that subsequent
 *  evaluations, possibly with modified values for input variables,
 *  are reasonably fast.
 *
 *  <p>Functions can be provided via the {@link FormulaFunction} SPI.
 *
 *  @author Kay Kasemir
 *  @author Xiaosong Geng
 */
@SuppressWarnings("nls")
public class Formula implements Node
{
    /** Logger for formula messages */
    public static final Logger logger = Logger.getLogger(Formula.class.getPackageName());

    /** The original formula that we parsed */
    final private String formula;

    final private Node tree;

    private static final VariableNode constants[] = new VariableNode[]
    {
        new VariableNode("E", Math.E),
        new VariableNode("PI", Math.PI)
    };

    /** SPI-provided functions mapped by name */
    private static final Map<String, FormulaFunction> spi_functions = new HashMap<>();

    static
    {
        // Locate SPI-provided functions
        for (FormulaFunction func : ServiceLoader.load(FormulaFunction.class))
        {
            logger.log(Level.FINE, () -> "SPI FormulaFunction " + func.getSignature());
            spi_functions.put(func.getName(), func);
        }
    }

    /** Determine variables from formula? */
    final private boolean determine_variables;

    /** Variables that can be used in the formula */
    final private ArrayList<VariableNode> variables;

    /** Create formula from string.
     *  @param formula The formula to parse
     *  @throws Exception on parse error
     */
    public Formula(final String formula)  throws Exception
    {
        this(formula, null);
    }

    /** Create formula from string with variables.
     *  @param formula The formula to parse
     *  @param variables Array of variables. Formula can only use these variables.
     *  @throws Exception on parse error
     */
    public Formula(final String formula,
                   final VariableNode[] variables)  throws Exception
    {
        this.formula = formula;
        if (variables == null)
            this.variables = null;
        else
        {
            this.variables = new ArrayList<>();
            for (VariableNode var : variables)
                this.variables.add(var);
        }
        this.determine_variables = false;
        tree = parse();
    }

    /** Create formula from string.
     *  @param formula The formula to parse
     *  @param determine_variables Determine variables from formula?
     *  @throws Exception on parse error
     */
    public Formula(final String formula, final boolean determine_variables)
        throws Exception
    {
        this.formula = formula;
        this.variables = new ArrayList<>();
        this.determine_variables = determine_variables;
        tree = parse();
    }

    /** @return Original formula that got parsed. */
    public String getFormula()
    {   return formula;    }

    /** @return Array of variables or <code>null</code> if none are used. */
    public VariableNode[] getVariables()
    {
        if (variables == null)
            return null;
        final VariableNode result[] = new VariableNode[variables.size()];
        return variables.toArray(result);
    }

    /** {@inheritDoc} */
    @Override
    public VType eval()
    {
        return tree.eval();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final Node node)
    {
        return tree == node  ||  tree.hasSubnode(node);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final String name)
    {
        return tree.hasSubnode(name);
    }

    /** Parse -0.1234 or variable or sub-expression in braces. */
    private Node parseConstant(final Scanner s) throws Exception
    {
        final String digits = "0123456789.";
        StringBuffer buf = new StringBuffer();
        boolean negative = false;

        if (s.isDone())
            throw new Exception("Unexpected end of formula.");
        // Possible leading '-'
        if (s.get() == '-')
        {
            negative = true;
            s.next();
        }
        if (s.isDone())
            throw new Exception("Unexpected end of formula.");
        Node result = null;
        // Sub-formula in '( ... )' ?
        if (s.get() == '(')
            result = parseBracedExpression(s);
        else if (s.get() == '\''  ||  s.get() == '`')
        {   // 'VariableName' or `VariableName`
            final char match = s.get();
            s.next(false);
            while (!s.isDone()  &&   s.get() != match)
            {
                buf.append(s.get());
                s.next(false);
            }
            if (s.isDone())
                throw new Exception("Unexpected end of quoted variable name.");
            // Skip final quote
            s.next();
            final String name = buf.toString();
            result = findVariable(name);
        }
        else if (s.get() == '"')
        {
            // "Text Constant"
            char last = s.get();
            s.next(false);
            // Copy until next un-escaped '"'
            while (!s.isDone()  &&   (s.get() != '"'  || last == '\\'))
            {
                last = s.get();
                if (last != '\\')
                    buf.append(last);
                s.next(false);
            }
            if (s.isDone())
                throw new Exception("Unexpected end of quoted string");
            // Skip final quote
            s.next();
            return new ConstantNode(buf.toString());
        }
        else
        {   // Digits?
            if (digits.indexOf(s.get()) >= 0)
            {
                boolean last_was_e = false;
                do
                {
                    buf.append(s.get());
                    last_was_e = s.get()=='e' || s.get()=='E';
                    s.next();
                }
                while (!s.isDone()
                       && (// Digits are OK
                           digits.indexOf(s.get()) >= 0
                           // So is e or E
                           || s.get()=='e'
                           || s.get()=='E'
                           // which might be in the form of "e+-34"
                           || (s.get()=='+' && last_was_e)
                           || (s.get()=='-' && last_was_e)));
                // Details of number format left to parseDouble()
                double value = Double.parseDouble(buf.toString());
                return new ConstantNode(negative ? -value : value);
            }
            // Else: assume variable or function.
            while (!s.isDone()  &&  isFunctionOrVariableChar(s.get()))
            {
                buf.append(s.get());
                s.next();
            }
            String name = buf.toString();
            if (s.get() == '(')
                result = findFunction(s, name);
            else
                result = findVariable(name);
        }
        if (negative)
            return new SubNode(new ConstantNode(0), result);
        return result;
    }

    /** @return <code>true</code> if given char is allowed inside a
     *          function or variable name.
     */
    private boolean isFunctionOrVariableChar(final char c)
    {
        final String other_allowed_stuff = "_:";
        return Character.isLetterOrDigit(c)
               || other_allowed_stuff.indexOf(c) >= 0;
    }

    /** @param name Function name
     *  @return Returns Node that evaluates the function.
     *  @throws Exception
     */
    private Node findFunction(final Scanner s, final String name) throws Exception
    {
        final Node [] args = parseArgExpressions(s);

        // Check SPI-provided functions.
        // Handled first so SPI could replace built-in functions
        final FormulaFunction function = spi_functions.get(name);
        if (function != null)
        {
            if (args.length != function.getArguments().size() &&
                !function.isVarArgs())
                throw new Exception("Function " + function.getSignature() + " takes " +
                                    function.getArguments().size() + " arguments but received " + Arrays.toString(args));
            return new SPIFuncNode(function, args);
        }
        // ... oddballs
        if (name.equalsIgnoreCase("rnd"))
        {
            if (args.length < 1)
                throw new Exception("Expected 1 arg, got " + args.length);
            return new RndNode(args[0]);
        }
        if (name.equalsIgnoreCase("min"))
        {
            if (args.length < 2)
                throw new Exception("Expected >=2 arg, got " + args.length);
            return new MinNode(args);
        }
        if (name.equalsIgnoreCase("max"))
        {
            if (args.length < 2)
                throw new Exception("Expected >=2 arg, got " + args.length);
            return new MaxNode(args);
        }
        throw new Exception("Unknown function '" + name +"'");
    }

    /** @return node for sub-expression arguments in (a1, a2, .. ) braces.
     *  @throws Exception when no closing ')' is found.
     */
    private Node[] parseArgExpressions(final Scanner s) throws Exception
    {
        Vector<Node> args = new Vector<>();
        if (s.get() != '(')
            throw new Exception("Expected '(', found '" + s.get() + "'");
        s.next();
        while (!s.isDone())
        {
            Node arg = parseBool(s);
            args.add(arg);
            // Expect ',' and another arg or ')'
            if (s.get() != ',')
                break;
            s.next();
        }
        if (s.get() != ')')
            throw new Exception("Expected closing ')'");
        s.next(); // ')'
        // Convert to array
        Node [] arg_nodes = new Node[args.size()];
        args.toArray(arg_nodes);
        return arg_nodes;
    }

    /** @param name Variable name.
     *  @return Returns VariableNode
     *  @throws Exception when not found.
     */
    private Node findVariable(final String name) throws Exception
    {
        if (variables != null)
        {   // Find the variable.
            for (VariableNode var : variables)
                if (var.getName().equals(name))
                    return var;
        }
        // No user variable. Try constants
        for (VariableNode var : constants)
            if (var.getName().equals(name))
                return var;

        if (!determine_variables)
           throw new Exception("Unknown variable '" + name + "'");
        // else: Automatically generate the unknown variable
        final VariableNode var = new VariableNode(name);
        variables.add(var);
        return var;
    }

    /** @return node for sub-expression in ( .. ) braces.
     *  @throws Exception when no closing ')' is found.
     */
    private Node parseBracedExpression(final Scanner s) throws Exception
    {
        Node result;
        if (s.get() != '(')
            throw new Exception("Expected '(', found '" + s.get() + "'");
        s.next();
        result = parseBool(s);
        if (s.get() != ')')
            throw new Exception("Expected closing ')'");
        s.next();
        return result;
    }

    /** Parse multiplication, division, ... */
    private Node parseMulDiv(final Scanner s) throws Exception
    {
        // Expect a ...
        Node n = parseUnary(s);
        // possibly followed by  * b / c ....
        while (! s.isDone())
        {
            if (s.get() == '^')
            {
                s.next();
                n = new PwrNode(n, parseUnary(s));
            }
            else if (s.get() == '*')
            {
                s.next();
                n = new MulNode(n, parseUnary(s));
            }
            else if (s.get() == '/')
            {
                s.next();
                n = new DivNode(n, parseUnary(s));
            }
            else break;
        }
        return n;
    }

    private Node parseUnary(final Scanner s) throws Exception
    {
        if (s.get() == '!')
        {
            s.next();
            return new NotNode(parseConstant(s));
        }
        else
            return parseConstant(s);
    }

    /** Parse addition, subtraction, ... */
    private Node parseAddSub(final Scanner s) throws Exception
    {
        // Expect a ...
        Node n = parseMulDiv(s);
        // possibly followed by  + b - c ....
        while (! s.isDone())
        {
            if (s.get() == '+')
            {
                s.next();
                n = new AddNode(n, parseMulDiv(s));
            }
            else if (s.get() == '-')
            {
                s.next();
                n = new SubNode(n, parseMulDiv(s));
            }
            else break;
        }
        return n;
    }

    /** Comparisons */
    private Node parseCompare(final Scanner s) throws Exception
    {
        // Expect a ...
        Node n = parseAddSub(s);
        // possibly followed by  > b >= c ....
        while (! s.isDone())
        {
            if (s.get() == '!')
            {
                s.next();
                if (s.get() == '=')
                {
                    s.next();
                    n = new NotEqualNode(n, parseAddSub(s));
                }
                else
                    throw new Exception("Expected '!=', found '!"
                            + s.get() + "'");
            }
            else if (s.get() == '=')
            {
                s.next();
                if (s.get() == '=')
                {
                    s.next();
                    n = new EqualNode(n, parseAddSub(s));
                }
                else
                    throw new Exception("Expected '==', found '="
                            + s.get() + "'");
            }
            else if (s.get() == '>')
            {
                s.next();
                if (s.get() == '=')
                {
                    s.next();
                    n = new GreaterEqualNode(n, parseAddSub(s));
                }
                else
                    n = new GreaterThanNode(n, parseAddSub(s));
            }
            else if (s.get() == '<')
            {
                s.next();
                if (s.get() == '=')
                {
                    s.next();
                    n = new LessEqualNode(n, parseAddSub(s));
                }
                else
                    n = new LessThanNode(n, parseAddSub(s));
            }
            else break;
        }
        return n;
    }

    /** Boolean &, | */
    private Node parseBool(final Scanner s) throws Exception
    {
        // Expect a ...
        Node n = parseCompare(s);
        // possibly followed by  & b | c ....
        while (! s.isDone())
        {
            if (s.get() == '&')
            {
                s.next();
                // Allow '&' as well as '&&'
                if (s.get() == '&')
                    s.next();
                n = new AndNode(n, parseCompare(s));
            }
            else if (s.get() == '|')
            {
                s.next();
                // Allow '|' as well as '||'
                if (s.get() == '|')
                    s.next();
                n = new OrNode(n, parseCompare(s));
            }
            else if (s.get() == '?')
            {
                s.next();
                Node yes = parseCompare(s);
                if (s.get() != ':')
                    throw new Exception("Expected ':' to follow the (cond) ? ...");
                s.next();
                n = new IfNode(n, yes, parseBool(s));
            }
            else break;
        }
        return n;
    }

    /** Parse formula.
     */
    private Node parse() throws Exception
    {
        final Scanner scanner = new Scanner(formula);
        final Node tree = parseBool(scanner);
        if (! scanner.isDone())
            throw new Exception("Parse error at '" + scanner.rest() + "'");
        return tree;
    }

    @Override
    public String toString()
    {
        return (tree != null) ? tree.toString() : "<empty formula>";
    }
}
