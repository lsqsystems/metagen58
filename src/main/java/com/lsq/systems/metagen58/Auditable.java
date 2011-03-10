/*
 * @(#)Auditable.java Copyright 2011 LSQ Systems, Inc. All rights reserved.
 */
package com.lsq.systems.metagen58;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation to mark all of the entities that are auditable.
 * 
 * @author Viet Trinh
 * @since 0.7
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface Auditable
{

}
