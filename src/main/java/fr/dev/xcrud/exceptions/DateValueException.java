/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.dev.xcrud.exceptions;

/**
 *
 * @author Francois
 */
public class DateValueException extends Exception {

    /**
     * Creates a new instance of <code>BooleanValueException</code> without detail message.
     */
    public DateValueException() {
    }


    /**
     * Constructs an instance of <code>BooleanValueException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DateValueException(String msg) {
        super(msg);
    }
}
