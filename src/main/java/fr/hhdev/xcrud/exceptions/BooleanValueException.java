/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.hhdev.xcrud.exceptions;

/**
 *
 * @author Francois
 */
public class BooleanValueException extends Exception {

    /**
     * Creates a new instance of <code>BooleanValueException</code> without detail message.
     */
    public BooleanValueException() {
    }


    /**
     * Constructs an instance of <code>BooleanValueException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public BooleanValueException(String msg) {
        super(msg);
    }
}
