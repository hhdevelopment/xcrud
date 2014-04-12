/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.dev.xcrud.exceptions;

/**
 *
 * @author Francois
 */
public class NoVariableException extends Exception {
	private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>InvalidIdException</code> without detail message.
     */
    public NoVariableException() {
    }


    /**
     * Constructs an instance of <code>InvalidIdException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NoVariableException(String msg) {
        super(msg);
    }
}
