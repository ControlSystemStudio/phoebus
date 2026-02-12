/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.acf;

import java.io.FileReader;
import java.io.Reader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.epics.pva.acf.AccessConfigTokenizer.Token;

import static org.epics.pva.PVASettings.logger;

/** Access security configuration file parser
 *
 *  @author Kay Kasemir
 */
public class AccessConfigParser
{
    private final Map<String, UserAccessGroup> user_groups = new HashMap<>();
    private final Map<String, HostAccessGroup> host_groups = new HashMap<>();
    private final Map<String, AccessSecurityGroup> access_groups = new HashMap<>();

    /** @param filename '*.acf' file to parse
     *  @return {@link AccessConfig}
     *  @throws Exception on error
     */
    public AccessConfig parse(final String filename) throws Exception
    {
        return parse(filename, new FileReader(filename));
    }

    /** @param filename '*.acf' file name
     *  @param file_reader {@link Reader} for filename
     *  @return {@link AccessConfig}
     *  @throws Exception on error
     */
    public AccessConfig parse(final String filename, final Reader file_reader) throws Exception
    {
        try (AccessConfigTokenizer tokenizer = new AccessConfigTokenizer(filename, file_reader))
        {
            while (! tokenizer.done())
            {
                final Token token = tokenizer.nextToken();
                if (token == null)
                    break;

                // UAG(name) { item, "another item")
                if ("UAG".equals(token.keyword()))
                {
                    final String name = parseName(tokenizer);
                    final List<String> names = parseNames(tokenizer, '{', '}');
                    final UserAccessGroup uag = new UserAccessGroup(name, names);
                    user_groups.put(uag.name(), uag);
                }
                // HAG(name) { item, "another item")
                else if ("HAG".equals(token.keyword()))
                {
                    final String name = parseName(tokenizer);
                    final List<String> names = parseNames(tokenizer, '{', '}');
                    final List<InetAddress> hosts = new ArrayList<>();
                    for (String nm : names)
                        try
                        {
                            hosts.add(InetAddress.getByName(nm));
                        }
                        catch (Exception ex)
                        {
                            logger.log(Level.WARNING, tokenizer + ": Cannot resolve host name '" + nm + "'", ex);
                        }
                    final HostAccessGroup hag = new HostAccessGroup(name, hosts);
                    host_groups.put(hag.name(), hag);
                }
                // UAG(name) { RULE... }
                else if ("ASG".equals(token.keyword()))
                {
                    final String name = parseName(tokenizer);
                    final AccessSecurityGroup asg = new AccessSecurityGroup(name);
                    tokenizer.checkSeparator('{');
                    AccessRule last_rule = null;
                    while (!tokenizer.done())
                    {   // One or more RULEs, potentially followed by conditions
                        final Token rule_or_conditions = tokenizer.nextToken();
                        if ("RULE".equals(rule_or_conditions.keyword()))
                            asg.add(last_rule = parseRule(tokenizer));
                        else if (rule_or_conditions.separator() == '{')
                        {   // Optional '{' UAG(...) '}' for the last(!) RULE
                            if (last_rule == null)
                                throw new Exception(tokenizer + " Missing RULE to which conditions could be added");
                            while (! tokenizer.done())
                            {
                                final Token condition = tokenizer.nextToken();
                                if ("UAG".equals(condition.keyword()))
                                {
                                    for (String nm : parseNames(tokenizer, '(', ')'))
                                    {
                                        final UserAccessGroup group = user_groups.get(nm);
                                        if (group == null)
                                            throw new Exception(tokenizer + " Unknown UAG " + nm);
                                        last_rule.add(group);
                                    }
                                }
                                else if ("HAG".equals(condition.keyword()))
                                {
                                    for (String nm : parseNames(tokenizer, '(', ')'))
                                    {
                                        final HostAccessGroup group = host_groups.get(nm);
                                        if (group == null)
                                            throw new Exception(tokenizer + " Unknown HAG " + nm);
                                        last_rule.add(group);
                                    }
                                }
                                else if (condition.separator() == '}') // end of RULE(..) { ... } within ASG
                                    break;
                                else
                                  throw new Exception(tokenizer + " ASG condition expects UAG or HAG, got " + condition);
                            }
                        }
                        else if (rule_or_conditions.separator() == '}') // end of ASG(..) { .... }
                            break;
                        else
                            throw new Exception(tokenizer + " expected RULE, got " + rule_or_conditions);
                    }
                    access_groups.put(asg.getName(), asg);
                }
                else
                    throw new Exception(tokenizer + " Expected keyword UAG, HAG, ASG, got " + token);
            }
        }

        return new AccessConfig(user_groups, host_groups, access_groups);
    }

    /** Parse "( NAME )"
     *  @return NAME
     */
    private String parseName(final AccessConfigTokenizer tokenizer) throws Exception
    {
        tokenizer.checkSeparator('(');
        final String name = tokenizer.nextName();
        tokenizer.checkSeparator(')');

        return name;
    }

    /** Parse "{ NAME, NAME, ... }"
     *  @return [ NAME, NAME, ... ]
     */
    private List<String> parseNames(final AccessConfigTokenizer tokenizer, final char open, final char close) throws Exception
    {
        final List<String> items = new ArrayList<>();

        // Locate opening delimiter
        tokenizer.checkSeparator(open);
        // Collect item, "another item" until closing delimiter
        while (! tokenizer.done())
        {
            final String item = tokenizer.nextName();
            items.add(item);

            final Token sep = tokenizer.nextToken();
            if (sep == null)
                throw new Exception(tokenizer + " Expected ',' or '" + close + "'");

            // Closing delimiter ends the list
            if (sep.separator() == close)
                break;
            // Comma continues the list
            if (sep.separator() != ',')
                throw new Exception(tokenizer + " Expected ',' or '" + close + "', got " + sep);
        }
        return items;
    }

    /** Parse "RULE(1, READ)" or ".. WRITE" with optional "{ ASG... }"
     *  @return {@link AccessRule}
     */
    private AccessRule parseRule(final AccessConfigTokenizer tokenizer) throws Exception
    {
        // RULE(level,
        tokenizer.checkSeparator('(');
        String text = tokenizer.nextName();
        final int level = Integer.parseInt(text);
        tokenizer.checkSeparator(',');

        // READ) or WRITE)
        text = tokenizer.nextName();
        if (! AccessRule.MODES.contains(text.toUpperCase()))
            throw new Exception(tokenizer + " Expect " + AccessRule.MODES + ", got '" + text + "'");
        final AccessRule.Mode mode = AccessRule.Mode.valueOf(text);
        tokenizer.checkSeparator(')');

        return new AccessRule(level, mode);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        for (var uag : user_groups.values())
            buf.append(uag).append("\n");
        for (var hag : host_groups.values())
            buf.append(hag).append("\n");
        for (var asg : access_groups.values())
            buf.append(asg).append("\n");
        return buf.toString();
    }
}
