package org.phoebus.framework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.SOURCE)
public @interface ProviderFor {

    Class<?> value();

}
