/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import java.util.ArrayList;
import java.util.Vector;

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
import org.csstudio.apputil.formula.node.MathFuncNode;
import org.csstudio.apputil.formula.node.MaxNode;
import org.csstudio.apputil.formula.node.MinNode;
import org.csstudio.apputil.formula.node.MulNode;
import org.csstudio.apputil.formula.node.NotEqualNode;
import org.csstudio.apputil.formula.node.NotNode;
import org.csstudio.apputil.formula.node.OrNode;
import org.csstudio.apputil.formula.node.PwrNode;
import org.csstudio.apputil.formula.node.RndNode;
import org.csstudio.apputil.formula.node.SubNode;

/** A formula interpreter.
 *  <p>
 *  While I found some on the Internet,
 *  I didn't see an open one that included the if-then-else
 *  operation nor one that I understood within 15 minutes
 *  so that I could add that operation.
 *  <p>
 *  Supported, in descending order of precedence:
 *  <ul>
 *  <li>Numeric constant 3.14, -47; named variables
 *  <li> (sub-formula in braces), sqrt(x), ln(x), exp(x),
 *       min(a, b, ...), max(a, b, ...).
 *  <li>*, /, ^
 *  <li>+, -
 *  <li>comparisons <, >, >=, <=, ==, !=
 *  <li>boolean logic !, &, |,  .. ? .. : ..
 *  </ul>
 *  <p>
 *  The formula string is parsed into a tree, so that subsequent
 *  evaluations, possibly with modified values for input variables,
 *  are reasonably fast.
 *  <p>
 *  See FormulaDialog in org.csstudio.apputil.ui plugin.
 *  That plugin also contains a class diagram.
 *
 *  @author Kay Kasemir
 *  @author Xiaosong Geng
 */
@SuppressWarnings("nls")
public class Formula implements Node
{
    /** The original formula that we parsed */
    final private String formula;

    final private Node tree;

    final private static VariableNode constants[] = new VariableNode[]
    {
        new VariableNode("E", Math.E),
        new VariableNode("PI", Math.PI)
    };

    /** Names of functions that take one argument. */
    final private static String one_arg_funcs[] = new String[]
    {
        "abs",
        "acos",
        "asin",
        "atan",
        "ceil",
        "cos",
        "cosh",
        "exp",
        "expm1",
        "floor",
        "log",
        "log10",
        "round",
        "sin",
        "sinh",
        "sqrt",
        "tan",
        "tanh",
        "toDegrees",
        "toRadians"
    };

    /** Names of functions that take two arguments, */
    final private static String two_arg_funcs[] = new String[]
    {
        "atan2",
        "hypot",
        "pow"
    };

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
            this.variables = new ArrayList<VariableNode>();
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
        this.variables = new ArrayList<VariableNode>();
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
    public double eval()
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
        else if (s.get() == '\'')
        {   // 'VariableName'
            s.next();
            while (!s.isDone()  &&   s.get() != '\'')
            {
                buf.append(s.get());
                s.next();
            }
            if (s.isDone())
                throw new Exception("Unexpected end of quoted variable name.");
            // Skip final quote
            s.next();
            final String name = buf.toString();
            result = findVariable(name);
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
        // Check functions with one arg
        for (int i=0; i<one_arg_funcs.length; ++i)
            if (name.equalsIgnoreCase(one_arg_funcs[i]))
            {
                if (args.length != 1)
                    throw new Exception("Expected 1 arg, got " + args.length);
                return new MathFuncNode(name, args);
            }
        // ... two args...
        for (int i=0; i<two_arg_funcs.length; ++i)
            if (name.equalsIgnoreCase(two_arg_funcs[i]))
            {
                if (args.length != 2)
                    throw new Exception("Expected 2 arg, got " + args.length);
                return new MathFuncNode(name, args);
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
        Vector<Node> args = new Vector<Node>();
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
        Node n = parseConstant(s);
        // possibly followed by  * b / c ....
        while (! s.isDone())
        {
            if (s.get() == '^')
            {
                s.next();
                n = new PwrNode(n, parseConstant(s));
            }
            else if (s.get() == '*')
            {
                s.next();
                n = new MulNode(n, parseConstant(s));
            }
            else if (s.get() == '/')
            {
                s.next();
                n = new DivNode(n, parseConstant(s));
            }
            else break;
        }
        return n;
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
        if (s.get() == '!')
        {
            s.next();
            return new NotNode(parseBool(s));
        }
        // Expect a ...
        Node n = parseCompare(s);
        // possibly followed by  & b | c ....
        while (! s.isDone())
        {
            if (s.get() == '&')
            {
                s.next();
                n = new AndNode(n, parseCompare(s));
            }
            else if (s.get() == '|')
            {
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
