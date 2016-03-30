/*
 * @(#) Weibull.java     1.3     98/1/20
 *
 * Copyright (c) 2005, John Miller, Zhiwei Zhang
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 * 3. Neither the name of the University of Georgia nor the names
 *    of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior
 *    written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @version     1.3, 28 Jan 2005
 * @author      John Miller, Zhiwei Zhang
 */


package gridsim.util;


/**
 * A random number generator based on the Weibull distribution. <br>
 * NOTE: This class is taken from
 * <a href="http://www.cs.uga.edu/~jam/jsim/">JSim</a>.<br>
 * Moreover, the {@link eduni.simjava.distributions.Sim_poisson_obj#sample()}
 * method always return NaN value.
 * @since GridSim Toolkit 4.1
 */

public class Weibull extends Variate
{
    /////////////////// Immutable Variables \\\\\\\\\\\\\\\\\\\\\\\
    /**
     * Scale parameter
     */
    private final double  scale;

    /**
     * Reciprocal of shape parameter
     */
    private final double  shape_recip;

    /**
     * Array of parameters
     */
    private  final Double []  params = new Double [3];


    /**************************************************************
     * Constructs a Weibull random variate.
     * @param  scale  scale parameter
     * @param  shape  shape parameter
     * @param  i      random number stream
     */
    public Weibull (double scale, double shape, int i)
    {
        super (i);
        if (scale <= 0.0 || shape <= 0.0) {
            System.err.println ("Weibull argument error: " +
                        "arguments are less than zero");
            System.exit (0);
        }; // if
        this.scale  = scale;
        shape_recip = 1.0 / shape;

        params [0] = new Double (scale);
        params [1] = new Double (shape);
        params [2] = new Double (i);

    }


    /**************************************************************
     * Get the parameters of the constuctor
     */
    public Double [] getParameters ()
    {
        return  params;
    }


    /**************************************************************
     * Generates a random number from Weibull distribution.
     * @return  double  random number from Weibull distribution
     */
    public double gen ()
    {
        return scale * Math.pow (-Math.log (super.gen ()), shape_recip);

    }

}

