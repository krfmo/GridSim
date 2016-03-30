package gridsim.parallel.util;

import java.util.ArrayList;
import java.util.Random;

import gridsim.Gridlet;

/**
 * The main purpose of this class is to create a realistic simulation
 * environment where your jobs are competing with others. This class is 
 * responsible for creating jobs according to the workload model
 * generator presented by Lublin and Feitelson.
 * <p>
 * Details on how the distributions have been implemented and how their 
 * parameters can be configured are described below. Please note that the 
 * following text was extracted from the C implementation of the model 
 * performed by Uri Lublin and Dror Feitelson:
 * <p>
 * The load generated can be changed by changing the values of the model parameters.
 * Remember to set the number-of-nodes' parameters to fit your system. 
 * I distinguish between the two different types (batch or interactive) if the
 * useJobType in the constructors is set to <tt>true</tt>. The program starts 
 * by initialising the model's parameters (according to the following constants 
 * definitions -- parameters values). Notice that if job type is set to 
 * <tt>true</tt>, there are different values to batch-parameters and 
 * interactive-parameters, and if this flag is <tt>false</tt> then they both get
 * the same value (and the arbitrary type interactive). The workload calculates 
 * for each job its arrival arrival time (using 2 gamma distributions), 
 * number of nodes (using a two-stage-uniform distribution), runtime 
 * (using the number of nodes and hyper-gamma distribution) and type.
 *
 * <p>
 * Some definitions:
 * <ul>
 *      <li> 'l' - the mean load of the system
 *      <li> 'r' - the mean runtime of a job
 *      <li> 'ri' - the runtime of job 'i' 
 *      <li> 'n' - the mean number of nodes of a job 
 *      <li> 'ni' - the number of nodes of job 'i'
 *      <li> 'a' - the mean inter-arrival time of a job
 *      <li> 'ai' - the arrival time (from the beginning of the simulation) of job 'i'
 *      <li> 'P'   - the number of nodes in the system (the system's size).    
 * </ul>
 * 
 * Given ri,ni,ai,N or r,n,a,N we can calculate the expected load on the system
 * 
 * <p>
 * <pre>
 *           sum(ri * ni)  
 * load =  ---------------
 *           P * max(ai)
 * </pre>
 * 
 * <p>
 * We can calculate an approximation (exact if 'ri' and 'ni' are independent)
 *
 *<p>
 *<pre>
 *                     r * n
 * approximate-load = -------
 *                     P * a
 *</pre>
 *
 * <p>
 * The model controls the r,m,a.
 * One can change them by changing the values of the model's parameters.
 *
 * <p>
 * ---------------------- <br>
 * Changing the runtime:  <br>
 * ---------------------- <br>
 * Let g ~ gamma(alpha,beta) then the expectation and variation of g are:<br>
 * E(g) = alpha*beta  ; Var(g) = alpha*(beta^2)<br>
 * so the coefficient of variation is CV  =  sqrt(Var(g))/E(g)  =  1/sqrt(alpha).
 * One who wishes to enlarge the gamma random value without changing the CV 
 * can set a larger value to the parameter beta . If one wishes that the CV will 
 * change he may set a larger value to alpha parameter.
 * Let hg ~ hyper-gamma(a1,b1,a2,b2,p) then the expectation and variation are: <br>
 * E(hg) = p*a1*b1 + (1-p)*a2*b2<br>
 * Var(hg) = p^2*a1*b1^2 + (1-p)^2*a2*b2^2<br>
 * One who wishes to enlarge the hyper-gamma (or the runtime) random value may 
 * do that using one (or more) of the three following ways:<br>
 * <p><ul>
 * <li> enlarge the first gamma.<br>
 * <li> and/or enlarge the second gamma.<br>
 * <li> and/or set a smaller value to the 'p' parameter.(parameter p of hyper-gamma
 *    is the proportion of the first gamma).
 *    since in my model 'p' is dependent on the number of nodes (p=pa*nodes+pb) 
 *    this is done by diminishing 'pa' and/or diminishing 'pb' such that 'p' 
 *    will be smaller. Note that changing 'pa' affects the correlation 
 *    between the number of nodes a job needs and its runtime. <br>
 * </ul>
 * Use {@link #setRunTimeParameters(int, double, double, double, double, double, double)}
 * in order to change the runtime parameters of jobs.
 * 
 * <p>
 * ------------------------ <br>
 * Changing the correlation between the number of nodes a job needs and its runtime: <br> 
 * ------------------------ <br>
 * The parameter 'pa' is responsible for that correlation. Since its negative
 * as the number of nodes get larger so is the runtime. 
 * One who wishes no correlation between the nodes-number and the runtime should
 * set 'pa' to be 0 or a small negative number close to 0.
 * One who wishes to have strong such a correlation should set 'pa' to be not so 
 * close to 0 , for example -0.05 or -0.1 . Note that this affect the runtime 
 * which will be larger. One can take care of that by changing the other runtime 
 * parameters (a1,b1,a2,b2,pb). <br>
 *
 * <p>
 * ----------------------------- <br>
 * Changing the number of nodes: <br>
 * ----------------------------- <br>
 * Let u ~ uniform(a,b) then the expectation of u is E(u) = (a+b)/2. <br>
 * Let tsu ~ two-stage-uniform(Ulow,Umed,Uhi,Uprob) then the expectation of tsu
 * is E(tsu) = Uprob*(Ulow+Umed)/2 + (1-Uprob)*(Umed+Uhi)/2  =  
 *           = (Uprob*Ulow + Umed + (1-Uprob)*Uhi)/2. <br>
 * <ul>
 * <li>'Ulow' is the log2 of the minimal number of nodes a job may run on.
 * <li>'Uhi' is the log2 of the system size.
 * <li>'Umed' is the changing point of the cdf function and should be set to
 * 'Umed' = 'Uhi' - 2.5. ('2.5' could be change between 1.5 to 3.5).
 * </ul>
 * For example if the system size is 8 (Uhi = log2(8) = 3) it makes no sense to
 * set Umed to be 0 or 0.5 . In this case I would set Umed to be 1.5 , and maybe 
 * set Ulow to be 0.8 , so the probability of 2 would not be too small.
 * 'Uprob' is the proportion of the first uniform (Ulow,Umed) and should be set 
 * to a value between 0.7 to 0.95.
 *<br>
 * One who wishes to enlarge the mean number of nodes may do that using one of  
 * the two following ways:
 * <p><ul>
 * <li> set a smaller value to 'prob'
 * <li> and/or enlarge 'uMed'
 * </ul>
 * Remember that changing the mean number of nodes will affect on the runtime 
 * too. 
 * <p>
 * Use {@link #setParallelJobProbabilities(int, double, double, double, double)}
 * to change the number of nodes and probability of parallel jobs. <br>
 * Use {@link #setPower2Probability(int, double)} to change the probability of
 * power of two jobs. <br>
 * Use {@link #setSerialProbability(int, double)} to change the probability for
 * serial or sequential jobs. <br> 
 *
 * <p>
 * -------------------------- <br>
 * Changing the arrival time: <br>
 * -------------------------- <br>
 * The arrival time is calculated in two stages:
 * <ul>
 * <li><b>First stage:</b> calculate the proportion of number of jobs arrived at every 
 *              time interval (bucket). this is done using the gamma(anum,bnum) 
 *              cdf and the CYCLIC_DAY_START. The weights array holds the points
 *              of all the buckets.
 * <li><b>Second stage:</b> for each job calculate its inter-arrival time:
 *              Generate a random value from the gamma(aarr,barr).
 *              The exponential of the random value is the 
 *              points we have. While we have less points than the current 
 *              bucket , "pay" the appropriate number of points and move to the
 *              next bucket (updating the next-arrival-time).
 * </ul>
 * The parameters 'aarr' and 'barr' represent the inter-arrival-time in the
 * rush hours. The parameters 'anum' and 'bnum' represent the number of jobs
 * arrived (for every bucket). The proportion of bucket 'i' is: 
 * cdf (i+0.5 , anum , bnum) - cdf(i-0.5 , anum , bnum).
 * We calculate this proportion for each i from BUCKETS to (24+BUCKETS) , 
 * and the buckets (their indices) that are larger than or equal to 24 are move 
 * cyclically to the right place:
 * <pre>
 * (24 --> 0 , 25 --> 1 , ... , (24+BUCKETS) --> BUCKETS)
 * </pre>
 * One who wishes to change the mean inter-arrival time may do that in the 
 * following way: <br>
 * - enlarge the rush-hours-inter-arrival time.
 * This is done by enlarging the value of aarr or barr according to the wanted
 * change of the CV. One who wishes to change the daily cycle may change the 
 * values of 'anum' and/or 'bnum'
 * <p>
 * Use {@link #setInterArrivalTimeParameters(int, double, double, double, double, double)}
 * to change the inter-arrival parameters.
 *
 * <p>
 * For more information on the workload model implemented here, please read 
 * the following paper: <br>
 *   Uri Lublin and Dror G. Feitelson, The Workload on Parallel Supercomputers: 
 *   Modeling the Characteristics of Rigid Jobs. J. Parallel & Distributed 
 *   Comput. 63(11), pp. 1105-1122, Nov 2003.
 * <br>
 *
 * @see gridsim.GridSim#init(int, Calendar, boolean)
 * 
 * @author   Marcos Dias de Assuncao
 * @since  5.0
 */
public class WorkloadLublin99 implements WorkloadModel {
    private int rating;        // a PE rating
    private int size;          // job size for sending it through a network
    private ArrayList<WorkloadJob> jobs;   // a list with all the jobs generated
    protected Random random = null;   	// the number generator to be used
    
    // Log PI for log gamma method 
    private static final double LOGPI  =  1.14472988584940017414;
    private static final int UNKNOWN = -1;
    
    // ------------ CONSTANTS USED BY THE WORKLOAD MODEL --------------

    //  no more than two days =exp(12) for runtime
    private static final int TOO_MUCH_TIME = 12; 
    
    // no more than 5 days =exp(13) for arrivetime 
    private static final int TOO_MUCH_ARRIVE_TIME = 13; 

    // number of buckets in one day -- 48 half hours
    private static final int BUCKETS = 48;
    
    // we now define (calculate) how many seconds are in one hour,day,bucket
    private static final int SECONDS_IN_HOUR = 3600; 
    private static final int SECONDS_IN_DAY = (24*SECONDS_IN_HOUR);
    private static final int SECONDS_IN_BUCKET = (SECONDS_IN_DAY/BUCKETS); 
    private static final int HOURS_IN_DAY = 24;

    // The hours of the day are being moved cyclically.  
    // instead of [0,23] make the day [CYCLIC_DAY_START,(24+CYCLIC_DAY_START-1)]
    private static final int CYCLIC_DAY_START = 11;

    // max iterations number for the iterative calculations
    private static final int ITER_MAX = 1000;
    
    // small epsilon for the iterative algorithm's accuracy
    private static final double EPS = 1E-10;
    
    /** Represents interactive jobs */
    public static final int INTERACTIVE_JOBS = 0;
    
    /** Represents batch jobs */
    public static final int BATCH_JOBS = 1;

    // ------------------- MODEL DEFAULT PARAMETERS -----------------------
    
    /** The default proportion of batch serial jobs */
    public static final double SERIAL_PROB_BATCH = 0.2927;
    /** The default proportion of batch jobs with power 2 number of nodes */
    public static final double POW2_PROB_BATCH = 0.6686;
    
    /** ULow, UMed, UHi and Uprob are the parameters for the two-stage-uniform 
     * which is used to calculate the number of nodes for parallel jobs. 
     * ULow is the default log2 of the minimal size of job in the system for batch jobs. */
    public static final double ULOW_BATCH = 1.2f; 	// smallest parallel batch job has 2 nodes
    /** UHi is the default log2 of the maximal size of a batch job in the system (system's size) */
    public static final double UHI_BATCH = 7;  		// biggest batch job has 128 nodes
    /** Default UMed for batch jobs. Note that UMed should be in [UHi-1.5 , UHi-3.5] */
    public static final double UMED_BATCH = 5;
    /** Default UProb for batch jobs. Note that Uprob should be in [0.7 - 0.95] */
    public static final double UPROB_BATCH = 0.875;

    /** The default proportion of interactive serial jobs */
    public static final double SERIAL_PROB_ACTIVE = 0.1541;
    /** The default proportion of interactive jobs with power 2 number of nodes */
    public static final double POW2_PROB_ACTIVE = 0.625;

    /** ULow, UMed, UHi and Uprob are the parameters for the two-stage-uniform 
     * which is used to calculate the number of nodes for parallel jobs. 
     * ULow is the default log2 of the minimal size of job in the system for interactive jobs. */
    public static final double ULOW_ACTIVE = 1;  // smallest interactive parallel job has 2 nodes
    /** UHi is the default log2 of the maximal size of an interactive job in the system (system's size) */
    public static final double UHI_ACTIVE = 5.5f;  // biggest interactive job has 45 nodes
    /** Default UMed for interactive jobs. Note that UMed should be in [UHi-1.5 , UHi-3.5] */
    public static final double UMED_ACTIVE = 3;
    /** Default UProb for interactive jobs. Note that Uprob should be in [0.7 - 0.95] */
    public static final double UPROB_ACTIVE = 0.705;

    /**
     * The parameters for the running time
     * The running time is computed using hyper-gamma distribution.
     * The parameters a1,b1,a2,b2 are the parameters of the two gamma distributions
     * The p parameter of the hyper-gamma distribution is calculated as a straight 
     * (linear) line p = pa*nodes + pb.
     * 'nodes' will be calculated in the program, here we defined the 'pa','pb' 
     * parameters.
     */
    public static final double A1_BATCH = 6.57;
    public static final double B1_BATCH = 0.823;
    public static final double A2_BATCH = 639.1;
    public static final double B2_BATCH = 0.0156;
    public static final double PA_BATCH = -0.003;
    public static final double PB_BATCH = 0.6986;

    public static final double A1_ACTIVE = 3.8351; 
    public static final double B1_ACTIVE = 0.6605;
    public static final double A2_ACTIVE = 7.073; 
    public static final double B2_ACTIVE = 0.6856; 
    public static final double PA_ACTIVE = -0.0118; 
    public static final double PB_ACTIVE = 0.9156; 


    /* The parameters for the inter-arrival time
     * The inter-arriving time is calculated using two gamma distributions.
     * gamma(aarr,barr) represents the inter_arrival time for the rush hours. It is 
     * independent on the hour the job arrived at.
     * The cdf of gamma(bnum,anum) represents the proportion of number of jobs which 
     * arrived at each time interval (bucket).
     * The inter-arrival time is calculated using both gammas
     * Since gamma(aarr,barr) represents the arrive-time at the rush time , we use
     * a constant ,ARAR (Arrive-Rush-All-Ratio), to set the alpha parameter (the new
     * aarr) so it will represent the arrive-time at all hours of the day.
     */
    public static final double AARR_BATCH = 6.0415;
    public static final double BARR_BATCH = 0.8531;
    public static final double ANUM_BATCH = 6.1271;
    public static final double BNUM_BATCH = 5.2740;
    public static final double ARAR_BATCH = 1.0519;

    public static final double AARR_ACTIVE = 6.5510;
    public static final double BARR_ACTIVE = 0.6621;
    public static final double ANUM_ACTIVE = 8.9186;
    public static final double BNUM_ACTIVE = 3.6680;
    public static final double ARAR_ACTIVE = 0.9797;

    /* 
     * Here are the model's parameters for typeless data (no batch nor interactive)
     * We use those parameters when useJobType_ is false
     */
    public static final double SERIAL_PROB = 0.244;
    public static final double POW2_PROB = 0.576;
    public static final double ULOW = 0.8f;       // The smallest parallel job is 2 nodes
    public static final double UMED = 4.5f;
    public static final double UHI = 7f;          // SYSTEM SIZE is 2^UHI == 128
    public static final double UPROB = 0.86;

    public static final double A1 = 4.2;
    public static final double B1 = 0.94;
    public static final double A2 = 312;
    public static final double B2 = 0.03;
    public static final double PA = -0.0054;
    public static final double PB = 0.78;

    public static final double AARR = 10.2303;
    public static final double BARR = 0.4871;
    public static final double ANUM = 8.1737;
    public static final double BNUM = 3.9631;
    public static final double ARAR = 1.0225;

    // start the simulation at midnight (hour 0)
    private static final int START = 0;
    
    // -------------- Variable dependent attributes --------------------

    // The workload duration, that is, for how long the workload can 
    // go creating and submitting gridlets. numJobs keeps the number of
    // jobs that this workload should generate. The workload will 
    // finish when it apporaches whichever of these values    
    private double workloadDuration;
    private int numJobs = 1000;		// usa a default of 1000 jobs
    
    // hour of the day when the simulation starts
    private int start; 
    
    private double[] a1, b1, a2, b2, pa, pb;
    private double[] aarr, barr, anum, bnum;
    private double[] serialProb, pow2Prob, uLow, uMed, uHi, uProb;

    // the appropriate weight (points) for each   
    // time-interval used in the arrive function  
    double[][] weights = new double[2][BUCKETS]; 
    
   /* useJobType us if we should differ between batch and interactive 
    * jobs or not. If the value is true, then we use both batch and interactive 
    * values for the parameters and the output sample includes both interactive 
    * and batch jobs. We choose the type of the job to be the type whose next 
    * job arrives to the system first (smaller next arrival time).
    * If the value is false, then we use the "whole sample" parameters. The output 
    * sample includes jobs from the same type (arbitrarily we choose batch).
    * We force the type to be interactive by setting the next arrival time of 
    * interactive jobs to be Long.MAX_VALUE -- always larger than the batchs's 
    * next arrival. */
    private boolean useJobType_ = true; 
    
    /* current time interval (the bucket's number) */
    private int[] current_;

    /* the number of seconds from the beginning of the simulation*/
    private long[] timeFromBegin_; 
    
    /**
     * Create a new workload model object. <br>
     * <tt>NOTE:</tt>
     * @param rating    the resource's PE rating
     * @param jobType useJobType us if we should differ between batch and interactive 
     * jobs or not. If the value is <code>true</code>, then we use both batch and interactive 
     * values for the parameters and the output sample includes both interactive 
     * and batch jobs. We choose the type of the job to be the type whose next 
     * job arrives to the system first (smaller next arrival time).
     * If the value is <code>false</code>, then we use the "whole sample" parameters. The output 
     * sample includes jobs from the same type (arbitrarily we choose batch).
     * We force the type to be interactive by setting the next arrival time of 
     * interactive jobs to be <code>Long.MAX_VALUE</code> -- always larger than the batchs's 
     * next arrival.
     * @param seed seed used by the random number generator
     * @throws IllegalArgumentException if resource PE rating <= 0
     * @pre rating > 0
     * @post $none
     */
    public WorkloadLublin99(int rating, boolean jobType, long seed) {

        // check the input parameters first
    	if (rating <= 0) {
            throw new IllegalArgumentException("Resource PE rating must be > 0.");
        }

        init(rating, jobType, seed);
    }

    /**
     * Initialises all the attributes.
     * @param rtg resource PE rating
     * @param jobType <tt>true</tt> whether the workload should generate
     * @param seed the simulation seed to be used
     * both interactive and batch jobs; <tt>false</tt> if the workload 
     * should generate only batch jobs.
     * @pre $none
     * @post $none
     */
    private void init(int rtg, boolean jobType, long seed) {
    	this.useJobType_ = jobType;
        this.rating = rtg;
        this.jobs = null;
        random = new Random(seed);
        workloadDuration = Double.MAX_VALUE;
        start = START;
        
        a1 = new double[2]; b1 = new double[2];
        a2 = new double[2]; b2 = new double[2];
        pa = new double[2]; pb = new double[2];
        aarr = new double[2]; barr = new double[2];
        anum = new double[2]; bnum = new double[2];
        serialProb = new double[2]; pow2Prob = new double[2];
        uLow = new double[2]; uMed = new double[2];
        uHi = new double[2]; uProb = new double[2];
        current_ = new int[2];
        timeFromBegin_ = new long[2];

        // separate batch from interactive
        if (useJobType_) { 
        	serialProb[BATCH_JOBS] = SERIAL_PROB_BATCH;
            pow2Prob[BATCH_JOBS] = POW2_PROB_BATCH;
            uLow[BATCH_JOBS] = ULOW_BATCH;
            uMed[BATCH_JOBS] = UMED_BATCH;
            uHi[BATCH_JOBS] = UHI_BATCH;
            uProb[BATCH_JOBS] = UPROB_BATCH;

            serialProb[INTERACTIVE_JOBS] = SERIAL_PROB_ACTIVE;
            pow2Prob[INTERACTIVE_JOBS] = POW2_PROB_ACTIVE;
            uLow[INTERACTIVE_JOBS] = ULOW_ACTIVE;
            uMed[INTERACTIVE_JOBS] = UMED_ACTIVE;
            uHi[INTERACTIVE_JOBS] = UHI_ACTIVE;
            uProb[INTERACTIVE_JOBS] = UPROB_ACTIVE;

            a1[BATCH_JOBS] =  A1_BATCH;    b1[BATCH_JOBS] =  B1_BATCH;
            a2[BATCH_JOBS] =  A2_BATCH;    b2[BATCH_JOBS] =  B2_BATCH;
            pa[BATCH_JOBS] =  PA_BATCH;    pb[BATCH_JOBS] = PB_BATCH;

            a1[INTERACTIVE_JOBS] = A1_ACTIVE;   b1[INTERACTIVE_JOBS] = B1_ACTIVE;
            a2[INTERACTIVE_JOBS] = A2_ACTIVE;   b2[INTERACTIVE_JOBS] = B2_ACTIVE;
            pa[INTERACTIVE_JOBS] = PA_ACTIVE;   pb[INTERACTIVE_JOBS] = PB_ACTIVE;

            aarr[BATCH_JOBS] = AARR_BATCH*ARAR_BATCH;     barr[BATCH_JOBS] = BARR_BATCH;
            anum[BATCH_JOBS] = ANUM_BATCH;                bnum[BATCH_JOBS] = BNUM_BATCH;

            aarr[INTERACTIVE_JOBS] = AARR_ACTIVE*ARAR_ACTIVE;  
            barr[INTERACTIVE_JOBS] = BARR_ACTIVE;
            anum[INTERACTIVE_JOBS] = ANUM_ACTIVE;              
            bnum[INTERACTIVE_JOBS] = BNUM_ACTIVE;
        }
        else {
        	// whole sample -- make all interactive jobs
        	serialProb[BATCH_JOBS] = serialProb[INTERACTIVE_JOBS] = SERIAL_PROB;
        	pow2Prob[BATCH_JOBS] = pow2Prob[INTERACTIVE_JOBS] = POW2_PROB;
        	uLow[BATCH_JOBS] = uLow[INTERACTIVE_JOBS] = ULOW;
        	uMed[BATCH_JOBS] = uMed[INTERACTIVE_JOBS] = UMED;
        	uHi[BATCH_JOBS] = uHi[INTERACTIVE_JOBS] = UHI;
        	uProb[BATCH_JOBS] = uProb[INTERACTIVE_JOBS] = UPROB;

        	a1[BATCH_JOBS] = a1[INTERACTIVE_JOBS] = A1;
        	b1[BATCH_JOBS] = b1[INTERACTIVE_JOBS] = B1;
        	a2[BATCH_JOBS] = a2[INTERACTIVE_JOBS] = A2;
        	b2[BATCH_JOBS] = b2[INTERACTIVE_JOBS] = B2;
        	pa[BATCH_JOBS] = pa[INTERACTIVE_JOBS] = PA;
        	pb[BATCH_JOBS] = pb[INTERACTIVE_JOBS] = PB;

        	aarr[BATCH_JOBS] = aarr[INTERACTIVE_JOBS] = AARR * ARAR;
        	barr[BATCH_JOBS] = barr[INTERACTIVE_JOBS] = BARR;
        	anum[BATCH_JOBS] = anum[INTERACTIVE_JOBS] = ANUM;
        	bnum[BATCH_JOBS] = bnum[INTERACTIVE_JOBS] = BNUM;
        	
        	timeFromBegin_[BATCH_JOBS] = Long.MAX_VALUE;
        }
    }
    
    /**
     * Sets the probability for serial jobs
     * @param jobType the type of jobs 
     * @param prob the probability
     * @return <tt>true</tt> if the probability has been set, or
     * <tt>false</tt> otherwise.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public boolean setSerialProbability(int jobType, double prob) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return false;
    	}
    	
    	if(useJobType_) {
    		serialProb[jobType] = prob;
    	} else {
    		serialProb[INTERACTIVE_JOBS] = serialProb[BATCH_JOBS] = prob;
    	}
    		
    	return true;
    }
    
    /**
     * Gets the probability for serial jobs
     * @param jobType the type of jobs 
     * @return prob the probability; <tt>-1</tt> if an error occurs.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public double getSerialProbability(int jobType) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return UNKNOWN;
    	}
    	
    	return serialProb[jobType];
    }

    /**
     * Sets the probability for power of two jobs
     * @param jobType the type of jobs 
     * @param prob the probability
     * @return <tt>true</tt> if the probability has been set, or
     * <tt>false</tt> otherwise.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public boolean setPower2Probability(int jobType, double prob) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return false;
    	}
    	
    	if(useJobType_) {
    		pow2Prob[jobType] = prob;
    	} else {
    		pow2Prob[INTERACTIVE_JOBS] = pow2Prob[BATCH_JOBS] = prob;
    	}
    	
    	return true;
    }
    
    /**
     * Gets the probability for power of two jobs
     * @param jobType the type of jobs 
     * @return prob the probability; <tt>-1</tt> if an error occurs.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public double getPower2Probability(int jobType) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return UNKNOWN;
    	}
    	
    	return pow2Prob[jobType];
    }

    /**
     * Sets the parameters for the two-stage-uniform 
     * which is used to calculate the number of nodes for parallel jobs.
     * @param jobType the type of jobs 
     * @param uLow is the log2 of the minimal size of job in the system 
     * (you can add or subtract 0.2 to give less/more probability to the 
     * minimal size).
     * @param uMed should be in [uHi-1.5 , uHi-3.5]
     * @param uHi is the log2 of the maximal size of a job in the system
     * (system's size)
     * @param uProb should be in [0.7 - 0.95]
     * @return <tt>true</tt> if the probabilities have been set, or
     * <tt>false</tt> otherwise.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public boolean setParallelJobProbabilities(int jobType, 
    		double uLow, double uMed, double uHi, double uProb) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return false;
    	} else if (uLow > uHi) {
    		return false;
    	} else if (uMed > uHi-1.5 || uMed < uHi-3.5) {
    		return false;
    	} else if(uProb < 0.7 || uProb > 0.95) {
    		return false;
    	}
    	
    	if(useJobType_) {
    		this.uLow[jobType] = uLow;
	       	this.uMed[jobType] = uMed;
	       	this.uHi[jobType]  = uHi;
	       	this.uProb[jobType] = uProb;
    	}
    	else {
    		this.uLow[INTERACTIVE_JOBS] = this.uLow[BATCH_JOBS] = uLow;
	       	this.uMed[INTERACTIVE_JOBS] = this.uMed[BATCH_JOBS] = uMed;
	       	this.uHi[INTERACTIVE_JOBS] = this.uHi[BATCH_JOBS] = uHi;
	       	this.uProb[INTERACTIVE_JOBS] = this.uProb[BATCH_JOBS] = uProb;
    	}
    	
        return true;
    }
    
    /**
     * Gets the probability of the job being a parallel job 
     * @param jobType the type of jobs 
     * @return the value of uProb; <tt>-1</tt> if an error occurs.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public double getParallelJobUProb(int jobType) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return UNKNOWN;
    	}
    	
    	return uProb[jobType];
    }
    
    /**
     * Gets the log2 of the maximal size of a job in the system (system's size)
     * @param jobType the type of jobs 
     * @return the value of uHi; <tt>-1</tt> if an error occurs.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public double getParallelJobUHi(int jobType) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return UNKNOWN;
    	}
    	
    	return uHi[jobType];
    }
    
    /**
     * Gets the medium size of parallel jobs in the system. It is log2 of 
     * the size.
     * @param jobType the type of jobs 
     * @return the value of uMed; <tt>-1</tt> if an error occurs.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public double getParallelJobUMed(int jobType) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return UNKNOWN;
    	}
    	
    	return uMed[jobType];
    }
    
    /**
     * Gets the the log2 of the minimal size of job in the system (you can add or 
     * subtract 0.2 to give less/more probability to the minimal size).
     * @param jobType the type of jobs 
     * @return the value of uLow; <tt>-1</tt> if an error occurs.
     * @see #INTERACTIVE_JOBS
     * @see #BATCH_JOBS
     */
    public double getParallelJobULow(int jobType) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return UNKNOWN;
    	}
    	
    	return uLow[jobType];
    }
    
    /** 
     * Sets the parameters for the running time
     * The running time is computed using hyper-gamma distribution.
     * The parameters a1,b1,a2,b2 are the parameters of the two gamma distributions
     * The p parameter of the hyper-gamma distribution is calculated as a straight 
     * (linear) line p = pa*nodes + pb. * 'nodes' will be calculated 
     * in the program, here we defined the 'pa','pb' 
     * parameters.
     * @param jobType the type of jobs 
     * @param a1 hyper-gamma distribution parameter.
     * @param a2 hyper-gamma distribution parameter.
     * @param b1 hyper-gamma distribution parameter.
     * @param b2 hyper-gamma distribution parameter.
     * @param pa hyper-gamma distribution parameter.
     * @param pb hyper-gamma distribution parameter.
     * @return <tt>true</tt> if the parameters have been set, or
     * <tt>false</tt> otherwise.
     */
    public boolean setRunTimeParameters(int jobType, 
    		double a1, double a2, double b1, double b2,
    		double pa, double pb) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return false;
    	}
    	
    	if(useJobType_) {
	    	this.a1[jobType] = a1;  this.b1[jobType] = b1;
	    	this.a2[jobType] = a2;  this.b2[jobType] = b2;
	    	this.pa[jobType] = pa;  this.pb[jobType] = pb;
    	}
    	else {
	    	this.a1[INTERACTIVE_JOBS] = this.a1[BATCH_JOBS] = a1;  
	    	this.b1[INTERACTIVE_JOBS] = this.b1[BATCH_JOBS] = b1;
	    	this.a2[INTERACTIVE_JOBS] = this.a2[BATCH_JOBS] = a2;  
	    	this.b2[INTERACTIVE_JOBS] = this.b2[BATCH_JOBS] = b2;
	    	this.pa[INTERACTIVE_JOBS] = this.pa[BATCH_JOBS] = pa;  
	    	this.pb[INTERACTIVE_JOBS] = this.pb[BATCH_JOBS] = pb;
    	}
    	
        return true;
    }
    
    /**
     * Gets the runtime parameters. That is, it returns the parameters
     * used for the hyper-gamma distribution. For more detail on the
     * parameters, please check the paper that describes the model.
     * @param jobType the type of jobs 
     * @return an array where: <br>
     * array[0] = a1 - hyper-gamma distribution parameter. <br>
     * array[1] = a2 - hyper-gamma distribution parameter. <br>
     * array[2] = b1 - hyper-gamma distribution parameter. <br>
     * array[3] = b2 - hyper-gamma distribution parameter. <br>
     * array[4] = pa - hyper-gamma distribution parameter. <br>
     * array[5] = pb - hyper-gamma distribution parameter. <br>
     * The method will return <tt>null</tt> if an error occurs.
     */
    public double[] getRunTimeParameters(int jobType) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return null;
    	}
    	
    	return new double[] { this.a1[jobType],	this.a2[jobType],
    	    	this.b1[jobType], this.b2[jobType],
    	    	this.pa[jobType], this.pb[jobType]};
    }
    
    /** 
     * Sets the parameters for the inter-arrival time. <br>
     * The inter-arrival time is calculated using two gamma distributions.
     * gamma(aarr,barr) represents the inter-arrival time for the rush hours. 
     * It is independent on the hour the job arrived at. The cdf of 
     * gamma(bnum,anum) represents the proportion of number of jobs which 
     * arrived at each time interval (bucket). The inter-arrival time is 
     * calculated using both gammas. Since gamma(aarr,barr) represents the 
     * arrival-time at the rush time, we use a constant, ARAR 
     * (Arrive-Rush-All-Ratio), to set the alpha parameter (the new aarr) 
     * so it will represent the arrive-time at all hours of the day.
     * @param jobType the type of job for which the inter-arrival parameters
     * will be modified.
     * @param aarr the first parameter for gamma(aarr,barr) distribution
     * @param barr the second parameter for gamma(aarr,barr) distribution
     * @param anum the first parameter for gamma(bnum,anum) distribution
     * @param bnum the second parameter for gamma(bnum,anum) distribution
     * @param arar Arrive-Rush-All-Ratio
     * @return <tt>true</tt> if the parameters have been set; 
     * <tt>false</tt> if they have not been set.
     */
    public boolean setInterArrivalTimeParameters(int jobType, 
    		double aarr, double barr, double anum, double bnum,	double arar) {
    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return false;
    	}
    	
    	if(useJobType_) {
	    	this.aarr[jobType] = aarr * arar;  
	    	this.barr[jobType] = barr;
	    	this.anum[jobType] = anum;  
	    	this.bnum[jobType] = bnum;
    	}
    	else {
	    	this.aarr[INTERACTIVE_JOBS] = this.aarr[BATCH_JOBS] = aarr * arar;  
	    	this.barr[INTERACTIVE_JOBS] = this.barr[BATCH_JOBS] = barr;
	    	this.anum[INTERACTIVE_JOBS] = this.barr[BATCH_JOBS] = anum;  
	    	this.bnum[INTERACTIVE_JOBS] = this.barr[BATCH_JOBS] = bnum;
    	}

        return true;
    }

    /**
     * Returns the parameters for the inter-arrival time
     * The inter-arriving time is calculated using two gamma distributions.
     * gamma(aarr,barr) represents the inter_arrival time for the rush hours. 
     * It is independent on the hour the job arrived at.
     * The cdf of gamma(bnum,anum) represents the proportion of number of
     * jobs which arrived at each time interval (bucket).
     * The inter-arrival time is calculated using both gammas
     * Since gamma(aarr,barr) represents the arrive-time at the rush time, 
     * we use a constant, ARAR (Arrive-Rush-All-Ratio), to set the alpha 
     * parameter (the new aarr) so it will represent the arrive-time at 
     * all hours of the day.
     * @param jobType the type of jobs 
     * @return an array where: <br>
     * array[0] = aarr - gamma distribution parameter. <br>
     * array[1] = barr - gamma distribution parameter. <br>
     * array[2] = anum - gamma distribution parameter. <br>
     * array[3] = bnum - gamma distribution parameter. <br>
     * The method will return <tt>null</tt> if an error occurs.
     */
    public double[] getInterArrivalTimeParameters(int jobType) {

    	if(jobType > BATCH_JOBS || jobType < INTERACTIVE_JOBS) {
    		return null;
    	}
    	
    	return new double[] { this.aarr[jobType], this.barr[jobType],
    			this.anum[jobType], this.bnum[jobType]};
    }
    
    /**
     * Sets the maximum number of jobs to be generated by this workload model
     * @param numJobs the number of jobs
     * @return <tt>true</tt> if the number of jobs has been set; 
     * <tt>false</tt> otherwise. 
     */
	public boolean setNumJobs(int numJobs) {
		if(numJobs < 1) {
			return false;
		}
		
		this.numJobs = numJobs;
		return true;
	}

	/**
	 * Sets the maximum time duration of the workload. The workload will create
	 * jobs whose time of submission is less or equals to workloadDuration. 
	 * The workload model will stop when it approaches workloadDuration.
	 * @param duration the maximum duration of the workload.
	 * @return <tt>true</tt> if the duration has been set; 
     * <tt>false</tt> otherwise.
	 */
	public boolean setMaximumWorkloadDuration(double duration) {
		if(duration <= 0) {
			return false;
		}
		
		workloadDuration = duration;
		return true;
	}

	/**
	 * Gets the hour of the day when the simulation should start
	 * @return the start hour (between 0 and 23)
	 */
	public int getStartHour() {
		return start;
	}

	/**
	 * Sets the hour of the day when the simulation should start
	 * @param start the start hour to set (between 0 and 23)
	 * @return <tt>true</tt> if set; <tt>false</tt> otherwise.
	 */
	public boolean setStartHour(int start) {
		if(start < 0 || start > 23) {
			return false;
		}
		
		this.start = start;
		return false;
	}

	/**
     * Sets a Gridlet file size (in byte) for sending to/from a resource.
     * @param size  a Gridlet file size (in byte)
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre size > 0
     * @post $none
     */
    public boolean setGridletFileSize(int size) {
        if (size < 0) {
            return false;
        }

        this.size = size;
        return true;
    }

    /**
     * Generates jobs according to the model.
     * @return the list of jobs generated; <code>null</code> 
     * in case of failure.
     */
    public ArrayList<WorkloadJob> generateWorkload() {
    	if(jobs == null) {
    		jobs = new ArrayList<WorkloadJob>();
    		
    		if(!createGridlets()) {
    			jobs = null;
    		}
    	}
    	
    	return jobs;
    }
    
    //------------------------- PRIVATE METHODS -------------------------

	/**
     * Creates the gridlets according to the workload model and sends them
     * to the grid resource entity.
     * @return <tt>true</tt> if the gridlets were created successfully or
     * <tt>false</tt> otherwise.
     */
    private boolean createGridlets() {
    	int type = INTERACTIVE_JOBS;
    	int nodes = 1, runTime = -1;
    	long arrTime = -1;
        
    	arrivalInit(aarr, barr, anum, bnum, start, weights);
    	
    	for (int i=0; i<numJobs; i++) {
    		long[] info = getNextArrival(type, weights, aarr, barr);
    		type = (int)info[0];
    	    arrTime = info[1];
    	    
    	    if(arrTime > workloadDuration) {
    	    	return true;
    	    }

    	    nodes = calcNumberOfNodes(serialProb[type] , pow2Prob[type],
    					 uLow[type], uMed[type], uHi[type], uProb[type]);
    	    
    	    runTime = (int)timeFromNodes(a1[type], b1[type], a2[type], b2[type],
    				       pa[type], pb[type], nodes);
    	    
            int len = runTime * rating;    // calculate a job length for each PE
            Gridlet gl = new Gridlet(i+1, len, size, size);
            gl.setNumPE(nodes);             // set the requested num of proc

            // check the submit time
            if (arrTime < 0) {
            	arrTime = 0;
            }

            WorkloadJob job = new WorkloadJob(gl, arrTime);
            jobs.add(job);
    	}
 
    	return true;
    }
    
    /*
     * We distinguish between serial jobs , power2 jobs and other.
     * for serial job (with probability SerialProb) the number of nodes is 1
     * for all parallel jobs (both power2 and other jobs) we randomly choose a 
     * number (called 'par') from two-stage-uniform distribution.
     * if the job is a power2 job then we make it an integer (par = round(par)), 
     * the number of nodes will be 2^par but since it must be an integer we return
     * round(pow(2,par)).
     * if we made par an integer then 2^par is ,obviously, a power of 2.
     */
    private int calcNumberOfNodes(double serialProb, double pow2Prob, double uLow,
    				  double uMed, double uHi, double uProb) {
      
    	double u = random.nextDouble();
    	if (u <= serialProb) {// serial job 
    		return 1;
    	}
    	double par = twoStageUniform(uLow, uMed, uHi, uProb);
    	if (u <= (serialProb + pow2Prob)) {      // power of 2 nodes parallel job 
    		par = (int)(par + 0.5);              // par = round(par)
    	}

    	int numNodes = (int)(Math.pow(2, par) + 0.5);    // round(2^par)
    	int maxNodes = (int)(Math.pow(2, uHi) + 0.5);
    	return numNodes <= maxNodes ? numNodes : maxNodes; 
    }
    
    /*
     * timeFromNodes returns a value of a random number from hyperGamma distribution.
     * The a1,b1,a2,b2 are the parameters of both gammas.
     * The 'p' parameter is calculated from the 'nodes' 'pa' and 'pb' arguments 
     * using the formula:   p = pa * nodes + pb.
     * we keep 'p' a probability by forcing its value to be in the interval [0,1].
     * if the value that was randomly chosen is too big (larger than 
     * TOO_MUCH_TIME) then we choose another random value.
     */
    private long timeFromNodes(double alpha1, double beta1, 
    			      double alpha2, double beta2,
    			      double pa, double pb, int nodes) {
    	double hg;
    	double p = pa*nodes + pb;
      
    	if (p>1)
    		p=1;
    	else if (p<0)
    		p=0;
    	do {
    		hg = hyperGamma(alpha1 , beta1 , alpha2 , beta2 , p); 
    	} while (hg > TOO_MUCH_TIME);
      
    	return (long)Math.exp(hg);
    }

    /* Initialises the variables for the arrival process.
     * 'getNextArrival' returns arrival time (from the beginning of 
     * the simulation) of the current job.
     * The (gamma distribution) parameters 'aarr' and 'barr' represent the 
     * inter-arrival time at rush hours.
     * The (gamma distribution) parameters 'anum' and 'bnum' represent the number
     * of jobs arriving at different times of the day. Those parameters fit a day 
     * that contains the hours [CYCLIC_DAY_START..24+CYCLIC_DAY_START] and are 
     * cyclically moved back to 0..24
     * 'start' is the starting time (in hours 0-23) of the simulation.
     * If the inter-arrival time randomly chosen is too big (larger than
     * TOO_MUCH_ARRIVE_TIME) then another value is chosen.
     *
     * The algorithm (briefly):
     * A. Preparations (calculated only once in 'arrivalInit()':
     * 1. foreach time interval (bucket) calculate its proportion of the number 
     *    of arriving jobs (using 'anum' and 'bnum'). This value will be the 
     *    bucket's points.
     * 2. calculate the mean number of points in a bucket ()
     * 3. divide the points in each bucket by the points mean ("normalize" the 
     *    points in all buckets)
     * B. randomly choosing a new arrival time for a job:
     * 1. get a random value from distribution gamma(aarr, barr).
     * 2. calculate the points we have.
     * 3. accumulate inter-arrival time by passing buckets (while paying them 
     *    points for that) until we do not have enough points.
     * 4. handle reminders - add the new reminder and subtract the old reminder.
     * 5. update the time variables ('current_' and 'timeFromBegin_')
     */
    private void arrivalInit(double[] aarr, double[] barr, 
    		double[] anum, double[] bnum, int start_hour, double weights[][]) {
    	
    	int idx, moveto = CYCLIC_DAY_START;
    	double[] mean = new double[] {0,0};

    	for(int i=0; i<weights.length; i++) {
    		for(int j=0; j<weights[i].length; j++) {
    			weights[i][j] = 0;
    		}
    	}
    	
    	current_[BATCH_JOBS] = 
    		current_[INTERACTIVE_JOBS] = start_hour * BUCKETS / HOURS_IN_DAY; 

    	/* 
    	 * for both batch and interactive calculate the propotion of each bucket ,
    	 * and their mean */
    	for (int j=0 ; j<=1 ; j++) {
    		for (int i=moveto ; i<BUCKETS+moveto ; i++) {
    			idx = (i-1)%BUCKETS; /* i-1 since array indices are 0..47 and not 1..48 */
    			weights[j][idx] =  
    				gamcdf(((double)i)+0.5, anum[j], bnum[j]) - gamcdf(((double)i)-0.5, anum[j],bnum[j]);
    			mean[j] += weights[j][idx];
    		}
    		mean[j] /= BUCKETS;
    	}

    	/* normalize it so we associates between seconds and points correctly */
    	for (int j=0 ; j<=1 ; j++) {
    		for (int i=0 ; i<BUCKETS ; i++) {
    			weights[j][i] /= mean[j];
    		}
    	}

      	calcNextArrival(BATCH_JOBS ,weights,aarr,barr);
      	calcNextArrival(INTERACTIVE_JOBS,weights,aarr,barr);
    }
    
    /* 
     * calcNextArrival calculates the next inter-arrival time according 
     * to the current time of the day.  
     * 'type' is the type of the next job -- interactive or batch
     * alpha and barr are the parameters of the gamma distribution of the 
     * inter-arrival time.
     * NOTE: this function changes the global variables concerning the arrival time.
     */
    private void calcNextArrival(int type, double[][] weights, 
    		double[] aarr, double[] barr) {
    	
    	double[] points = new double []{0,0};
    	double[] reminder = new double[]{0,0};
    	int bucket;
    	double gam, nextArrival, newReminder, moreTime;
      
    	bucket = current_[type]; // the bucket of the current time
    	do {     // randomly choose a (not too big) number from gamma distribution 
    		gam = gamrnd(aarr[type],barr[type]);
    	} while (gam > TOO_MUCH_ARRIVE_TIME);
      
    	points[type] += (Math.exp(gam) / SECONDS_IN_BUCKET); // number of points 
    	nextArrival = 0;
    	while (points[type] > weights[type][bucket]) { // while have more points 
    		points[type] -= weights[type][bucket];     // pay points to this bucket
    		bucket = (bucket+1)  % 48;                 //   ... and goto the next bucket
    		nextArrival += SECONDS_IN_BUCKET;          // accumulate time in next_arrive
    	}
    	
    	newReminder = points[type]/weights[type][bucket];
    	moreTime = SECONDS_IN_BUCKET * ( newReminder - reminder[type]);
    	nextArrival += moreTime;        // add reminders   
    	reminder[type] = newReminder;   // save it for next call

    	// update the attributes
    	timeFromBegin_[type] += nextArrival;
    	current_[type] = bucket;
    }

   /* 
    * Return the time for next job to arrive the system.
    * returns also the type of the next job , which is the type that its 
    * next arrive time (time_from_begin) is closer to the start (smaller).
    * notice that since calc_next_arrive changes time_from_begin[] we must save
    * the time_from_begin in 'res' so we would be able to return it.
    * @returns an array where array[0] = type of the job, array[1] = arrival time
    */
    private long[] getNextArrival(int type, double[][] weights, 
    		double[] aarr, double[] barr) {
    	
    	long res;

    	type = (timeFromBegin_[BATCH_JOBS] < timeFromBegin_[INTERACTIVE_JOBS]) ? 
    			BATCH_JOBS : INTERACTIVE_JOBS;
    	
    	res = timeFromBegin_[type];       // save the job's arrival time

    	// randomly choose the next job's (of the same type) arrival time
    	calcNextArrival(type, weights, aarr, barr); 
    	return new long[] {type, res};
    }
    
    // --- Methods required by the distributions used by the workload model  ---
    // --- Most of these methods were ported from C to Java, but the logic   ---
    // --- remains the same as in Lublin's workload model in originally in C ---

    /*
	 * hyperGamma returns a value of a random variable of mixture of two
	 * gammas. its parameters are those of the two gammas: a1,b1,a2,b2 and the
	 * relation between the gammas (p = the probability of the first gamma). we
	 * first randomly decide which gamma will be active ((a1,b1) or (a2,b2)).
	 * then we randomly choose a number from the chosen gamma distribution.
	 */
	private double hyperGamma(double a1, double b1, double a2, 
			double b2, double p) {
		double a, b, hg, u = random.nextDouble();

		if (u <= p) { // gamma(a1,b1) 
			a = a1;
			b = b1;
		} else { // gamma(a2,b2) 
			a = a2;
			b = b2;
		}

		// generates a value of a random variable from distribution gamma(a,b) 
		hg = gamrnd(a, b);
		return hg;
	}
    
	/*
	 * gamrnd returns a value of a random variable of gamma(alpha,beta).
	 * gamma(alpha,beta) = gamma(int(alpha),beta) +
	 * gamma(alpha-int(alpha),beta). This function and the following 3 functions
	 * were written according to Jain Raj, 'THE ART OF COMPUTER SYSTEMS
	 * PERFORMANCE ANALYSIS Techniques for Experimental Design, Measurement,
	 * Simulation, and Modeling'. Jhon Wiley & Sons , Inc. 1991. Chapter 28 -
	 * RANDOM-VARIATE GENERATION (pages 484,485,490,491)
	 * can be improved by getting 'diff' and 'intalpha' as function's parameters
	 */
	private double gamrnd(double alpha, double beta) {
		double diff, gam = 0;
		long intalpha = (long) alpha;
		if (alpha >= 1)
			gam += gamrndIntAlpha(intalpha, beta);
		if ((diff = alpha - intalpha) > 0)
			gam += gamrndAlphaSmaller1(diff, beta);
		return gam;
	}

	/*
	 * gamrndIntAlpha returns a value of a random variable of gamma(n,beta)
	 * distribution where n is integer(unsigned long actually). gamma(n,beta) ==
	 * beta*gamma(n,1) == beta* sum(1..n){gamma(1,1)} == beta* sum(1..n){exp(1)} ==
	 * beta* sum(1..n){-ln(uniform(0,1))}
	 */
	private double gamrndIntAlpha(long n, double beta) {
		double acc = 0;
		for (int i = 0; i < n; i++)
			acc += Math.log(random.nextDouble()); 	// sum the exponential
													// random variables
		return (-acc * beta);
	}
    
	/*
	 * gamrndAlphaSmaller1 returns a value of a random variable of
	 * gamma(alpha,beta) where alpha is smaller than 1. This is done using the
	 * Beta distribution. (alpha<1) ==> (1-alpha<1) ==> we can use
	 * beta_less_1(alpha,1-alpha) gamma(alpha,beta) = exponential(beta) *
	 * Beta(alpha,1-alpha)
	 */
	private double gamrndAlphaSmaller1(double alpha, double beta) {
		double x = betarndLess1(alpha, 1 - alpha); // beta random variable
		double y = -Math.log(random.nextDouble()); // exponential random
													// variable 
		return (beta * x * y);
	}
    
	/*
	 * betarndLess1 returns a value of a random variable of beta(alpha,beta)
	 * distribution where both alpha and beta are smaller than 1 (and larger
	 * than 0)
	 */
	private double betarndLess1(double alpha, double beta) {
		double x, y, u1, u2;
		do {
			u1 = random.nextDouble();
			u2 = random.nextDouble();
			x = Math.pow(u1, 1D / alpha);
			y = Math.pow(u2, 1D / beta);
		} while (x + y > 1);
		return (x / (x + y));
	}
    
	/* 
	 * Returns the cumulative distribution function of gamma(alpha, beta)
	 * distribution at the point 'x'; return -1 if an error (non-convergence)
	 * occurs. 
	 * This function and the following two functions were written
	 * according to William H. Press , Brian P. Flannery , Saul A. Teukolsky and
	 * William T. Vetterling. NUMERICAL RECIPES IN PASCAL The Art of Scientific
	 * Computing. Cambridge University Press 1989 Chapter 6 - Special Functions
	 * (pages 180-183).
	 */
	private double gamcdf(double x, double alpha, double beta) {
		x /= beta;
		if (x < (alpha + 1)) {
			return gser(x, alpha);
		}

		// x >= a+1 
		return 1 - gcf(x, alpha);
	}
    

	/*
	 * Gser
	 */
	private double gser(double x, double a) {
		int i;
		double sum, monom, aa = a;

		sum = monom = 1 / a;

		for (i = 0; i < ITER_MAX; i++) {
			++aa;
			monom *= (x / aa);
			sum += monom;
			if (monom < sum * EPS)
				return (sum * Math.exp(-x + a * Math.log(x) - logGamma(a)));
		}
		return -1; // error, did not converged
	}

	/*
	 * Returns the GCF
	 */
	private double gcf(double x, double a) {
		int i;
		double gold = 0, g, a0 = 1, a1 = x, b0 = 0, b1 = 1, fac = 1, anf, ana;

		for (i = 1; i <= ITER_MAX; i++) {
			ana = i - a;
			a0 = (a1 + a0 * ana) * fac;
			b0 = (b1 + b0 * ana) * fac;
			anf = i * fac;
			a1 = x * a0 + anf * a1;
			b1 = x * b0 + anf * b1;
			if (a1 != 0.0) {
				fac = 1 / a1;
				g = b1 * fac;
				if (Math.abs((g - gold) / g) < EPS)
					return (g * Math.exp(-x + a * Math.log(x) - logGamma(a)));
				gold = g;
			}
		}
		return 2; // gamcdf will return -1
	}
    
    /*
	 * This method returns a random variable from a mixture of two uniform
	 * distributions : [low,med] and [med,hi]. 'prob' is the proportion of the
	 * first uniform. first we randomly choose the active uniform according to
	 * prob. then we randomly choose a value from the chosen uniform
	 * distribution. <b>Note</t> that this was extracted from Lublin's model.
	 */
    private double twoStageUniform(double low, double med, 
    		double hi, double prob) {
    	double a, b, tsu, u = random.nextDouble();

		if (u <= prob) { // uniform(low , med)
			a = low;
			b = med;
		} else { // uniform(med , hi) 
			a = med;
			b = hi;
		}

		// generate a value of a random variable from distribution uniform(a,b)
		tsu = (random.nextDouble() * (b - a)) + a;
		return tsu;
	}

	private double A[] = { 
			8.11614167470508450300E-4, 
			-5.95061904284301438324E-4,
			7.93650340457716943945E-4, 
			-2.77777777730099687205E-3,
			8.33333333333331927722E-2 };
	
	private double B[] = { 
			-1.37825152569120859100E3, 
			-3.88016315134637840924E4,
			-3.31612992738871184744E5, 
			-1.16237097492762307383E6,
			-1.72173700820839662146E6, 
			-8.53555664245765465627E5 };
	
	private double C[] = {
			// 1.00000000000000000000E0,
			-3.51815701436523470549E2, 
			-1.70642106651881159223E4,
			-2.20528590553854454839E5, 
			-1.13933444367982507207E6,
			-2.53252307177582951285E6, 
			-2.01889141433532773231E6 };
    
    /*
	 * Natural logarithm of gamma function
	 * Cephes Math Library Release 2.2:  July, 1992
	 * Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier
	 * Direct inquiries to 30 Frost Street, Cambridge, MA 02140
	 */
	private double logGamma(double x) 
			throws ArithmeticException {
		double p, q, w, z;

		if (x < -34.0) {
			q = -x;
			w = logGamma(q);
			p = Math.floor(q);
			if (p == q)
				throw new ArithmeticException("logGamma: Overflow");
			z = q - p;
			if (z > 0.5) {
				p += 1.0;
				z = p - q;
			}
			z = q * Math.sin(Math.PI * z);
			if (z == 0.0)
				throw new ArithmeticException("logGamma: Overflow");
			z = LOGPI - Math.log(z) - w;
			return z;
		}

		if (x < 13.0) {
			z = 1.0;
			while (x >= 3.0) {
				x -= 1.0;
				z *= x;
			}
			while (x < 2.0) {
				if (x == 0.0)
					throw new ArithmeticException("logGamma: Overflow");
				z /= x;
				x += 1.0;
			}
			if (z < 0.0)
				z = -z;
			if (x == 2.0)
				return Math.log(z);
			x -= 2.0;
			p = x * polevl(x, B, 5) / p1evl(x, C, 6);
			return (Math.log(z) + p);
		}

		if (x > 2.556348e305)
			throw new ArithmeticException("logGamma: Overflow");

		q = (x - 0.5) * Math.log(x) - x + 0.91893853320467274178;
		if (x > 1.0e8)
			return (q);

		p = 1.0 / (x * x);
		if (x >= 1000.0)
			q += ((7.9365079365079365079365e-4 
					* p - 2.7777777777777777777778e-3)
					* p + 0.0833333333333333333333)	/ x;
		else
			q += polevl(p, A, 4) / x;
		return q;
	}
	
	private static double polevl(double x, double coef[], int N)
			throws ArithmeticException {
		double ans;
		ans = coef[0];
		for (int i = 1; i <= N; i++) {
			ans = ans * x + coef[i];
		}
		return ans;
	}

	private static double p1evl(double x, double coef[], int N)
			throws ArithmeticException {
		double ans;
		ans = x + coef[0];
		for (int i = 1; i < N; i++) {
			ans = ans * x + coef[i];
		}
		return ans;
	}
} 

