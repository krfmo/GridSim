/*
 * ** Network and Service Differentiation Extensions to GridSim 3.0 **
 *
 * Gokul Poduval & Chen-Khong Tham
 * Computer Communication Networks (CCN) Lab
 * Dept of Electrical & Computer Engineering
 * National University of Singapore
 * October 2004
 *
 * License: GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2004, The University of Melbourne, Australia and
 * National University of Singapore
 * ParameterException.java - Thrown for illegal parameters
 *
 */

package gridsim;

/**
 * This exception is to report bad or invalid parameters given during
 * constructor.
 *
 * @invariant $none
 * @since GridSim Toolkit 3.1
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 */
public class ParameterException extends Exception
{
    private String message_;

    /**
     * Constructs a new exception with <tt>null</tt> as its detail message.
     * @pre $none
     * @post $none
     */
    public ParameterException() {
        message_ = null;
    }

    /**
     * Creates a new ParameterException object.
     * @param message   an error message
     * @pre $none
     * @post $none
     */
    public ParameterException(String message) {
        this.message_ = message;
    }

    /**
     * Returns an error message of this object
     * @return an error message
     * @pre $none
     * @post $none
     */
    public String toString() {
        return message_;
    }

} 

