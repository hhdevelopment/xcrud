/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.hhdev.xcrud.exceptions;

/**
 *
 * @author tns624
 */
public class NullValueException extends Exception {

    /**
     * Creates a new instance of <code>NumberValueException</code> without detail message.
     */
    public NullValueException() {
    }


    /**
     * Constructs an instance of <code>NumberValueException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NullValueException(String msg) {
        super(msg);
    }
}
