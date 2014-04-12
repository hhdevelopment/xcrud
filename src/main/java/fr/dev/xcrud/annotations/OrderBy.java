/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.dev.xcrud.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Francois
 */
@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface OrderBy {
    String [] values() default {};
    boolean asc() default true;
}
