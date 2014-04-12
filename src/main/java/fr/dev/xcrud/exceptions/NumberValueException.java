/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.dev.xcrud.exceptions;

/**
 *
 * @author Francois
 */
public class NumberValueException extends Exception {

    /**
     * Creates a new instance of <code>NumberValueException</code> without detail message.
     */
    public NumberValueException() {
    }


    /**
     * Constructs an instance of <code>NumberValueException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NumberValueException(String msg) {
        super(msg);
    }
}
