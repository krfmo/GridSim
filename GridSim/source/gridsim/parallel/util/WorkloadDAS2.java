package gridsim.parallel.util;

import gridsim.Gridlet;
import gridsim.parallel.util.WorkloadJob;
import gridsim.parallel.util.WorkloadModel;
import java.util.ArrayList;
import java.util.Random;

/**
 * The main purpose of this class is to create a realistic simulation
 * environment where your jobs are competing with others. This class is 
 * responsible for creating jobs according to the workload model
 * generator presented by Hui Li.
 * 
 * <p>
 * ---------------------- <br>
 * Changing the runtime:  <br>
 * ---------------------- <br>
 * Let g ~ lognormal(art,brt) then the expectation g is:<br>
 * E(g) = exp(art+brt^2/2)
 * 
 * Use {@link #setRunTimeParameters(double, double)}
 * in order to change the runtime parameters of jobs.
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
 * <p>
 * Use {@link #setParallelJobProbabilities(double, double, double, double)}
 * to change the number of nodes and probability of parallel jobs. <br>
 * Use {@link #setPower2Probability(double)} to change the probability of
 * power of two jobs. <br>
 * Use {@link #setSerialProbability(double)} to change the probability for
 * serial or sequential jobs. <br> 
 *
 * <p>
 * -------------------------- <br>
 * Changing the arrival time: <br>
 * -------------------------- <br>
 * The arrival time follows the weibull distribution
 * i = weibull(aarr, barr)
 * The parameters 'aarr' and 'barr' represent the inter-arrival-time in the
 * peak days.
 * 
 * Use {@link #setInterArrivalTimeParameters(double, double)}
 * to change the inter-arrival parameters.
 *
 * <p>
 * For more information on the workload model implemented here, please read 
 * the following paper: <br>
 * Hui Li, David Groep, and Lex Wolters, "Workload Characteristics of a Multi-cluster Supercomputer"
 * Lecture Notes in Computer Science, 2005, Volume 3277/2005, 176-193.
 * <br>
 *
 * 
 * @author   	 Bahman Javadi
 * @since        GridSim Toolkit 5.0
 * @see 		 WorkloadLublin99
 */
public class WorkloadDAS2 implements WorkloadModel {
    private int rating;        // a PE rating
    private int size;          // job size for sending it through a network
    private ArrayList<WorkloadJob> jobs;   // a list with all the jobs generated
    protected Random random = null;   	// the number generator to be used
    
    
    // ------------ CONSTANTS USED BY THE WORKLOAD MODEL --------------

    //  no more than two days =exp(12) for runtime
    private static final int TOO_MUCH_TIME = 162754; 
    
    // ------------------- MODEL DEFAULT PARAMETERS -----------------------
    
    public static final double SERIAL_PROB = 0.024;
    public static final double POW2_PROB = 0.78;
    public static final double ULOW = 0.8f;       // The smallest parallel job is 2 nodes
    public static final double UMED = 4.5f;
    public static final double UHI = 7f;          // SYSTEM SIZE is 2^UHI == 128
    public static final double UPROB = 0.90;


    public static final double AARR = 23.375;
    public static final double BARR = 0.522;
    public static final double ART = 4.40;
    public static final double BRT = 1.7;

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
    
    private double aarr, barr, art, brt;
    private double serialProb, pow2Prob, uLow, uMed, uHi, uProb;

    
    /* the number of seconds from the beginning of the simulation*/
    private long timeFromBegin_; 
    
    private long seed;
    /**
     * Create a new workload model object. <br>
     * <tt>NOTE:</tt>
     * @param rating    the resource's PE rating
     * @param seed seed used by the random number generator
     * @throws IllegalArgumentException if resource PE rating <= 0
     * @pre rating > 0
     * @post $none
     */
    public WorkloadDAS2(int rating, long seed) {

        // check the input parameters first
    	if (rating <= 0) {
            throw new IllegalArgumentException("Resource PE rating must be > 0.");
        }
    	this.seed = seed;
    	
        init(rating, seed);
    }

    /**
     * Initialises all the attributes.
     * @param rtg resource PE rating
     * @param seed the simulation seed to be used
     * @pre $none
     * @post $none
     */
    private void init(int rtg, long seed) {
        this.rating = rtg;
        this.jobs = null;
        random = new Random(seed);
        workloadDuration = Double.MAX_VALUE;
        start = START;
        

       	serialProb = SERIAL_PROB;
        pow2Prob = POW2_PROB;
        uLow = ULOW;
        uMed = UMED;
        uHi = UHI;
        uProb = UPROB;

        aarr = AARR;
        barr = BARR;
        
        art = ART;
        brt = BRT;
        	
        timeFromBegin_ = 0;
        
    }    
    /**
     * Sets the probability for serial jobs
     * @param prob the probability
     * @return <tt>true</tt> if the probability has been set
     * <tt>false</tt> otherwise.
     */
    public boolean setSerialProbability(double prob) {
    	serialProb = prob;
    	    		
    	return true;
    }
    
    /**
     * Gets the probability for serial jobs
     * @return prob the probability; <tt>-1</tt> if an error occurs.
     */
    public double getSerialProbability() {
  	
    	return serialProb;
    }

    /**
     * Sets the probability for power of two jobs
     * @param prob the probability
     * @return <tt>true</tt> if the probability has been set, or
     * <tt>false</tt> otherwise.
     */
    public boolean setPower2Probability(double prob) {
    	
    		pow2Prob = prob;
    	return true;
    }
    
    /**
     * Gets the probability for power of two jobs
     * @return prob the probability; <tt>-1</tt> if an error occurs.
     */
    public double getPower2Probability() {
    	
    	return pow2Prob;
    }

    /**
     * Sets the parameters for the two-stage-uniform 
     * which is used to calculate the number of nodes for parallel jobs.
     * @param uLow is the log2 of the minimal size of job in the system 
     * (you can add or subtract 0.2 to give less/more probability to the 
     * minimal size).
     * @param uMed should be in [uHi-1.5 , uHi-3.5]
     * @param uHi is the log2 of the maximal size of a job in the system
     * (system's size)
     * @param uProb should be in [0.7 - 0.95]
     * @return <tt>true</tt> if the probabilities have been set, or
     * <tt>false</tt> otherwise.
     */
    public boolean setParallelJobProbabilities( 
    		double uLow, double uMed, double uHi, double uProb) {
    	if (uLow > uHi) {
    		return false;
    	} else if (uMed > uHi-1.5 || uMed < uHi-3.5) {
    		return false;
    	} else if(uProb < 0.7 || uProb > 0.95) {
    		return false;
    	}
   		this.uLow = uLow;
       	this.uMed = uMed;
       	this.uHi  = uHi;
       	this.uProb = uProb;
    	
        return true;
    }
    
    /**
     * Gets the probability of the job being a parallel job 
     * @return the value of uProb; <tt>-1</tt> if an error occurs.
     */
    public double getParallelJobUProb() {	
    	return uProb;
    }
    
    /**
     * Gets the log2 of the maximal size of a job in the system (system's size)
     * @return the value of uHi; <tt>-1</tt> if an error occurs.
     */
    public double getParallelJobUHi() {    	
    	return uHi;
    }
    
    /**
     * Gets the medium size of parallel jobs in the system. It is log2 of 
     * the size.
     * @param jobType the type of jobs 
     * @return the value of uMed; <tt>-1</tt> if an error occurs.
     */
    public double getParallelJobUMed() {    	
    	return uMed;
    }
    
    /**
     * Gets the the log2 of the minimal size of job in the system (you can add or 
     * subtract 0.2 to give less/more probability to the minimal size).
     * @return the value of uLow; <tt>-1</tt> if an error occurs.
     */
    public double getParallelJobULow(int jobType) {   	
    	return uLow;
    }
    
    /** 
     * Sets the parameters for the running time
     * The running time is computed using hyper-gamma distribution.
     * The parameters aaar and barr are the parameters of the weibull distribution
     * @param art weibull distribution parameter.
     * @param brt weibull distribution parameter.
     * @return <tt>true</tt> if the parameters have been set, or
     * <tt>false</tt> otherwise.
     */
    public boolean setRunTimeParameters(double art, double brt) {
    	
    	this.art = art;  
    	this.brt = brt;
    	
        return true;
    }
    
    /**
     * Gets the runtime parameters. That is, it returns the parameters
     * used for the weibull distribution. For more detail on the
     * parameters, please check the paper that describes the model.
     * @return an array where: <br>
     * array[0] = art - weibull distribution scale parameter. <br>
     * array[1] = brt - weibull distribution shape parameter. <br>
     * The method will return <tt>null</tt> if an error occurs.
     */
    public double[] getRunTimeParameters() {
    	
    	return new double[] { this.art, this.brt};
    }
    
    /** 
     * Sets the parameters for the inter-arrival time. <br>
     * The inter-arrival time is calculated using weibull distribution.
     * weibull(aarr,barr) represents the inter-arrival time for the daily peak hours. 
     * @param aarr the scale parameter for weibull(aarr,barr) distribution
     * @param barr the shape parameter for weibull(aarr,barr) distribution
     * @return <tt>true</tt> if the parameters have been set; 
     * <tt>false</tt> if they have not been set.
     */
    public boolean setInterArrivalTimeParameters(double aarr, double barr) 
    {
    	this.aarr = aarr ;  
    	this.barr = barr;

        return true;
    }

    /**
     * Returns the parameters for the inter-arrival time
     * The inter-arrival time is calculated using weibull distribution.
     * weibull(aarr,barr) represents the inter-arrival time for the daily peak hours. 
     * @param aarr the scale parameter for weibull(aarr,barr) distribution
     * @param barr the shape parameter for weibull(aarr,barr) distribution
     */
    public double[] getInterArrivalTimeParameters() {
	
    	return new double[] { this.aarr, this.barr};
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
    	int nodes = 1; 
    	double runTime = -1;
    	long arrTime;
        
    	
    	for (int i=0; i<numJobs; i++) {
    		arrTime = getNextArrival(aarr, barr);
    	    
    	    if(arrTime > workloadDuration) {
    	    	System.out.println("WorkloadDAS2: number of generated jobs: "+ i);
    	    	return true;
    	    }

    	    nodes = calcNumberOfNodes(serialProb , pow2Prob,
    					 uLow, uMed, uHi, uProb);
    	    
    	    runTime = timetoRun(art, brt);
    	    
            double len = runTime * rating;    // calculate a job length for each PE
            Gridlet gl = new Gridlet(i+1, len, size, size);
            gl.setNumPE(nodes);             // set the requested num of proc

            // check the submit time
            if (arrTime < 0) {
            	arrTime = 0;
            }

            WorkloadJob job = new WorkloadJob(gl, arrTime);
            jobs.add(job);
    	}

    	System.out.println("WorkloadDAS2: number of generated jobs: "+ numJobs);
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
     * timeFromNodes returns a value of a random number from Lognormal distribution.
     * The alpha1,beta1 are the parameters of this distribution.
     * if the value that was randomly chosen is too big (larger than 
     * TOO_MUCH_TIME) then we choose another random value.
     */
    private double timetoRun(double alpha1, double beta1) {
    	double hg;
      
    	do {
    		hg = lognrnd(alpha1 , beta1); 
    	} while (hg > TOO_MUCH_TIME);
      
    	return hg;
    }
 
   /* 
    * Return the time for next job to arrive the system.
    */
    private long getNextArrival(double aarr, double barr) {
    	
    	long res;
    	double nextArrival;
    	

   		nextArrival = wblrnd(aarr, barr);

    	timeFromBegin_ += nextArrival;
    	res = timeFromBegin_;       // save the job's arrival time
    	
    	return res;
    }
    
	/*
	 * wblrnd returns a value of a random variable of weibull(scale, shape).
	*/
	private double wblrnd(double scale, double shape) {
	       
		double shape_recip = 1.0 / shape;
        return scale * Math.pow (-Math.log (random.nextDouble()), shape_recip);

	}

	/*
	 * lognrnd returns a value of a random variable of lognormal(mean, std).
	 * e^(mean+std*X), X ~ N(0,1)
	*/
	private double lognrnd(double mu, double sigma) {

		Double rnd = Math.pow (Math.E, mu + sigma*random.nextGaussian());
		
		return rnd;

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
}