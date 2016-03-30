package gridsim.fta;

import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class is responsible for reading failure traces from a file and
 * creating a list of events.
 * 
 * @param filename is the name of event_trace file
 * @param TraceStartTime is the time of starting the trace
 * 		  (if more than the actual start time, then ignore all events before that time)
 * 
 * @author 		Bahman Javadi
 * @since       GridSim Toolkit 5.0
 * @see  		<a href="http://fta.inria.fr"> fta.inria.fr</a>
 * @see			 ResourceFileReader
*/

public class FailureFileReader implements FailureModel{
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);

	private String fileName;   			// file name
    private int curID;					// current node id
    private int eventCtr;				//event counter
    private ArrayList<FailureEvent> events = null;  // a list for getting all the Gridlets
    private FailureEvent nodeEvent = null;

    // using Failure Trace Archive Format 
    private int NODE_ID = 3 - 1;        // node id
    private int EVENT_TYPE = 6 - 1;    // type of an event
    private int START_TIME = 7 - 1;       // start time of an event
    private int END_TIME = 8 - 1;       // end time of an event
    private int MAX_FIELD = 9;      	// max number of field in the trace file
    private String COMMENT = "#";     	// a string that denotes the start of a comment
    
    private String[] fieldArray = null;       // a temp array storing all the fields
    private double TraceStartTime = 0;

    boolean addtolist = false;				// to control the event list  
    
    public FailureFileReader(String fileName, double TraceStartTime) {
        if (fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("Invalid trace file name.");
        }

        this.fileName = fileName;
        this.curID = 0;
        this.eventCtr = 0;
        this.TraceStartTime = TraceStartTime;
    }
    
    /**
     * Identifies the start of a comment line.
     * @param cmt  a character that denotes the start of a comment, e.g. ";" or "#"
     * @return <code>true</code> if it is successful, <code>false</code> otherwise
     * @pre comment != null
     * @post $none
     */
    public boolean setComment(String cmt) {
        boolean success = false;
        if (cmt != null && cmt.length() > 0) {
            COMMENT = cmt;
            success = true;
        }
        return success;
    }
    
    /**
     * Tells this class what to look in the trace file.
     * This method should be called before the start of the simulation.
     * <p>
     * By default, this class follows the failure trace archive format as specified
     * in <a href="http://fta.inria.fr">
     * fta.inria.fr</a> <br>
     * However, you can use other format by calling this method.
     * <p>
     * The parameters must be a positive integer number starting from 1.
     *
 
     * @param maxField  max. number of field/column in one row
     * @param nodeID    field/column number for locating the node ID
     * @param eventType   field/column number for locating the event type (availability/unavailability)
     * @param startTime   field/column number for locating the event start time
     * @param endTime   field/column number for locating the event end time
     *                  

     * @return <code>true</code> if successful, <code>false</code> otherwise
     * @throws IllegalArgumentException if any of the arguments are not 
     * within the acceptable ranges
     * @pre maxField > 0
     * @pre nodeID > 0
     * @pre eventType > 0
     * @pre startTime > 0
     * @pre endTime > 0
     * @post $none
     */
    public boolean setField(int maxField, int nodeID, int eventType,
                            int startTime, int endTime) {
        // need to subtract by 1 since array starts at 0.
        if (nodeID > 0) {
            NODE_ID = nodeID - 1;
        } else 
        	throw new IllegalArgumentException("Invalid node id field.");
        

        // get the max. number of field
        if (maxField > 0) {
            MAX_FIELD = maxField;
        } else {
        	throw new IllegalArgumentException("Invalid max. number of field.");
        }

        // get the event type field
        if (eventType > 0) {
            EVENT_TYPE = eventType - 1;
        } else {
        	throw new IllegalArgumentException("Invalid event type field.");
        }

        // get the start time field
        if (startTime > 0) {
            START_TIME = startTime - 1;
        } else {
        	throw new IllegalArgumentException("Invalid start time field.");
        }

        // get the end time field
        if (endTime > 0) {
            END_TIME = endTime - 1;
        } else {
        	throw new IllegalArgumentException("Invalid end time field.");
        }

        return true;
    }

    /**
     * Reads failure information from a given file.
     * @return the list of events read from the file; <code>null</code> 
     * in case of error.
     */
    public ArrayList<FailureEvent> generateFailure() {
    	if(events == null) {
	        events = new ArrayList<FailureEvent>();
	
	        // create a temp array
	        fieldArray = new String[MAX_FIELD];
	        
	        try {
		        if (fileName.endsWith(".gz")) {
		        	readGZIPFile(fileName);
		        }
		        else if (fileName.endsWith(".zip")) {
		            readZipFile(fileName);
		        }
		        else {
		            readFile(fileName);
		        }
			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE, "File not found", e);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Error reading file", e);
			}
    	}
        
        return events;
    }


//------------------- PRIVATE METHODS -------------------

/**
 * Breaks a line of string into many fields.
 * @param line  a line of string
 * @param lineNum   a line number
 * @pre line != null
 * @pre lineNum > 0
 * @post $none
 */
private void parseValue(String line, int lineNum) {
    // skip a comment line
    if (line.startsWith(COMMENT)) {
        return;
    }

    String[] sp = line.split("\\s+");  // split the fields based on a space
    int len = 0;        // length of a string
    int index = 0;      // the index of an array

    // check for each field in the array
    for(String elem : sp) {
        len = elem.length();  // get the length of a string

        // if it is empty then ignore
        if (len == 0) {
            continue;
        }
        // if not, then put into the array
        else {
            fieldArray[index] = elem;
            index++;
        }
    }
    
    if (index == MAX_FIELD) {
        extractField(fieldArray, lineNum);
    }
}

/**
 * Extracts relevant information from a given array
 * @param array  an array of String
 * @param line   a line number
 * @pre array != null
 * @pre line > 0
 */
private void extractField(String[] array, int line) {
    try {
        Integer obj = null;
        
        // get the node ID
        obj = new Integer( array[NODE_ID].trim() );
        int id = obj.intValue();
        
        // if we read the new node, reset the event counter
        if (id != curID){
        	eventCtr = 0;
        	addtolist = true;
        	curID = id;
        }
        else{
        	eventCtr++; 	
        }
        // get the submit time
        obj = new Integer( array[EVENT_TYPE].trim() );
        int eventType = obj.intValue();

        // get the run time
        Double l = new Double( array[START_TIME].trim() );
        double startTime = l.doubleValue();

        // get the run time
        l = new Double( array[END_TIME].trim() );
        double endTime = l.doubleValue();
        
        createFailureEvent(id, eventType, startTime, endTime, addtolist);
    }
    catch (Exception e) {
    	logger.log(Level.WARNING, "Exception reading file at line #" + line, e);
    }
}

/**
 * Creates an event with the given information and adds to the list
 * @param id  an event ID
 * @param eventType   event's type		
 * @param startTime   event's start time
 * @param endTime	  event's end time
 * @param flag        new node (true: add new node)
 * @pre id >= 0
 * @pre startTime>= 0
 * @pre endTime >= 0
 * @pre flag {true, false}
 * @post $none
 */
private void createFailureEvent(int id, int eventType, double startTime, double endTime, boolean flag) {
    
    // with this option we could start the failure from anywhere in the trace file
	if (startTime-TraceStartTime < 0)
    	return;

    if (nodeEvent==null){
    	nodeEvent = new FailureEvent(id,eventType,startTime-TraceStartTime,endTime-TraceStartTime);
    	addtolist = false;

    }else{
    if (flag){
    	events.add(nodeEvent);
    	nodeEvent = new FailureEvent(id,eventType,startTime-TraceStartTime,endTime-TraceStartTime);
    	addtolist = false;
    }
    else{
	nodeEvent.insertEvent(id,eventType,startTime-TraceStartTime,endTime-TraceStartTime);
    }
    }
}
    
/**
 * Reads a text file one line at the time
 * @param flName a file name
 * @return <code>true</code> if successful, <code>false</code> otherwise.
 * @throws IOException if the there was any error reading the file
 * @throws FileNotFoundException if the file was not found
 */
private boolean readFile(String flName) throws IOException, FileNotFoundException {
    boolean success = false;
    BufferedReader reader = null;
    
    try {
        FileInputStream file = new FileInputStream(flName);
        reader = new BufferedReader(new InputStreamReader(file));

        // read one line at the time
        int line = 1;
        while (reader.ready()) {
            parseValue(reader.readLine(), line);
            line++;
        }
    	// Finalize the event for last node 
        events.add(nodeEvent);

        reader.close(); 
        success = true;
    } finally {
        if (reader != null) {
        	reader.close(); 
        }
    }

    return success;
}

/**
 * Reads a gzip file one line at the time
 * @param flName   a gzip file name
 * @return <code>true</code> if successful; <code>false</code> otherwise.
 * @throws IOException if the there was any error reading the file
 * @throws FileNotFoundException if the file was not found
 */
private boolean readGZIPFile(String flName) 
					throws IOException, FileNotFoundException {
    boolean success = false;
    BufferedReader reader = null;

    try {
        FileInputStream file = new FileInputStream(flName);
        GZIPInputStream gz =  new GZIPInputStream(file);
        reader = new BufferedReader(new InputStreamReader(gz));


        // read one line at the time
        int line = 1;
        while (reader.ready()) {
            parseValue(reader.readLine(), line);
            line++;
        }
    	// Finalize the event for last node 
        events.add(nodeEvent);

        reader.close();   
        success = true;
    } finally {
        if (reader != null) {
        	reader.close();    
        }
    }

    return success;
}

/**
 * Reads a Zip file.
 * @param flName a zip file name
 * @return <code>true</code> if reading a file is successful; 
 * <code>false</code> otherwise.
 * @throws IOException if the there was any error reading the file
 */
private boolean readZipFile(String flName) throws IOException {
    boolean success = false;
    ZipFile zipFile = null;

    try {
        BufferedReader reader = null;

        // ZipFile offers an Enumeration of all the files in the file
        zipFile = new ZipFile(flName);
        Enumeration<? extends ZipEntry> e = zipFile.entries();
        while (e.hasMoreElements()) {
            success = false;    // reset the value again
            ZipEntry zipEntry = e.nextElement();

            reader = new BufferedReader(
            		new InputStreamReader(zipFile.getInputStream(zipEntry)));


            // read one line at the time
            int line = 1;
            while (reader.ready()) {
                parseValue(reader.readLine(), line);
                line++;
            }
        	// Finalize the event for last node 
            events.add(nodeEvent);

            reader.close();   
            success = true;
        }
    } finally {
        if (zipFile != null) {
        	zipFile.close();    
        }
    }

    return success;
}
} 

