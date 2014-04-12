/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.dev.xcrud.exceptions;

/**
 *
 * @author Francois
 */
public class InvalidIdException extends Exception {

    /**
     * Creates a new instance of <code>InvalidIdException</code> without detail message.
     */
    public InvalidIdException() {
    }


    /**
     * Constructs an instance of <code>InvalidIdException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public InvalidIdException(String msg) {
        super(msg);
    }
}
