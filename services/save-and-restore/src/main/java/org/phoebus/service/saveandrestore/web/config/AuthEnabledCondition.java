/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.service.saveandrestore.web.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} subclass used to determine if authentication/authorization is enabled through an
 * application property setting.
 */
public class AuthEnabledCondition implements Condition {
    /**
     * @param context  the condition context
     * @param metadata the metadata of the {@link org.springframework.core.type.AnnotationMetadata class}
     *                 or {@link org.springframework.core.type.MethodMetadata method} being checked
     * @return <code>true</code> if application property <code>auth.impl</code> is anything other than <code>none</code>.
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !"none".equalsIgnoreCase(context.getEnvironment().getProperty("auth.impl").trim());
    }
}
