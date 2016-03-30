/*
 * @(#) LCGRandom.java     1.3     98/1/20
 *
 * Copyright (c) 2005, John Miller
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
 * @author      John Miller
 */

package gridsim.util;

/**
 * Basic random number generator.
 * It is a multiplicative Linear Congruential Generator (LCG).
 * The generator can be adjusted simply by changing the
 * constants MULTIPIER and MODULUS (be careful).
 * The complete generated stream of MODULUS numbers is split
 * into MAX_STREAMS (stream from 0 to MAX_STREAMS - 1) by
 * appropriate setting of the SEED array (again be careful). <br>
 * NOTE: This class is taken from
 * <a href="http://www.cs.uga.edu/~jam/jsim/">JSim</a>.
 * @since GridSim Toolkit 4.1
 */

public class LCGRandom
{
    //////////////////////// Constants \\\\\\\\\\\\\\\\\\\\\\\\\\\\
    private static final long   MULTIPIER   = 16807;       // 7^5
    private static final long   MODULUS     = 2147483647;  // 2^31 - 1
    private static final double SCALER      = 1.0 / MODULUS;

    private static final long   SEED [] = { 428956419,
                                           1954324947,
                                           1145661099,
                                           1835732737,
                                            794161987,
                                           1329531353,
                                            200496737,
                                            633816299,
                                           1410143363,
                                           1282538739 };

    /**
     * Random number stream
     */
    private int  stream;

    /**
     * Array of parameters
     */
    private  final Double []  params = new Double [1];


    /**************************************************************
     * Constructs an LCG-based Random Number Generator.
     * @param  stream  stream number (integer from 0 to MAX_STREAMS - 1)
     */
    public LCGRandom (int stream)
    {
        this.stream  = stream;
        params [0] = new Double (stream);

    }

    public void incrStream ()
    {
        stream++;
        params [0] = new Double (stream);
    }

    /**************************************************************
     * Get the parameters of the constuctor
     */
    public Double [] getParameters ()
    {
        return  params;
    }

    /**************************************************************
     * Generates a real-valued random number in the range 0 to 1.
     * @return  double  random number
     */
    public double gen ()
    {
        long z = SEED [stream];
        z = (MULTIPIER * z) % MODULUS;  // 64-bit integer arithmetic
        SEED [stream] = z;
        return (double) z * SCALER;

    }

    /**************************************************************
     * Generates an integer-valued random number in the range
     * 0 to MODULUS - 1.
     * @return  long   integer-valued random number
     */
    public long igen ()
    {
        long z = SEED [stream];
        z = (MULTIPIER * z) % MODULUS;  // 64-bit integer arithmetic
        SEED [stream] = z;
        return z;

    }

}

