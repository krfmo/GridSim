package gridsim.fta;

import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible for reading failure traces from a file with the FTA format and
 * creating a list of resource characterization.
 *  
 * @param 	Platform_fileName is the name of platform file
*  @param 	Node_fileName is the name of node file

 * @author       Bahman Javadi
 * @since        GridSim Toolkit 5.0
 * @see  		 <a href="http://fta.inria.fr"> fta.inria.fr</a>
 * @see			 FailureFileReader
 * 
 */

public class ResourceFileReader{
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);

	private String platform_fileName;   		// platform filename
	private String node_fileName;   			// node filename
    private int NodeNum;						// current node id
    private String Platform_Name;				//platform name

    // using Failure Trace Archive Format 
    private int PLATFORM_NAME = 2 - 1;        // node id
    
    private int NODE_ID = 1 - 1;    		  // type of an event

    private int NODE_MAX_FIELD = 16;      	  // max number of field in the trace file
    private int PLATFORM_MAX_FIELD = 5;      // max number of field in the trace file
    private String COMMENT = "#";     	     // a string that denotes the start of a comment
    
    private String[] fieldArray = null;       // a temp array storing all the fields
    
    public ResourceFileReader(String Platform_fileName, String Node_fileName) throws IOException {
        if (Platform_fileName == null || Platform_fileName.length() == 0) {
            throw new IllegalArgumentException("Invalid Platform file name.");
        }

        if (Node_fileName == null || Node_fileName.length() == 0) {
            throw new IllegalArgumentException("Invalid Node file name.");
        }
        // create a temp array
        fieldArray = new String[NODE_MAX_FIELD];

        this.platform_fileName = Platform_fileName;
        this.node_fileName = Node_fileName;
        this.NodeNum = 0;
        
        try {
	            platform_readFile(platform_fileName);
		} catch (FileNotFoundException e1) {
			logger.log(Level.SEVERE, "File not found", e1);
		}
			try {
	            node_readFile(node_fileName);
		} catch (FileNotFoundException e2) {
			logger.log(Level.SEVERE, "File not found", e2);
		}
        
        
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
     * return the platform name
     * @return the Platform name
     */
    public String getPlatformName(){
   	 return Platform_Name;
    }

    /**
     * The number of node in the system
     * @return the number of node in the system
     */
    public int getNodeNum(){
   	 return NodeNum;
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
    
    if (index == PLATFORM_MAX_FIELD) {
        platform_extractField(fieldArray, lineNum);
    }
    if (index == NODE_MAX_FIELD) {
        node_extractField(fieldArray, lineNum);
    }
}

/**
 * Extracts relevant information from a given array
 * @param array  an array of String
 * @param line   a line number
 * @pre array != null
 * @pre line > 0
 */
private void platform_extractField(String[] array, int line) {
    try {
        String obj = null;
        
        // get the node ID
        obj = new String( array[PLATFORM_NAME].trim() );
//        int id = obj.intValue();
        Platform_Name = obj;
    }
    catch (Exception e) {
    	logger.log(Level.WARNING, "Exception reading file at line #" + line, e);
    }
}

/**
 * Extracts relevant information from a given array
 * @param array  an array of String
 * @param line   a line number
 * @pre array != null
 * @pre line > 0
 */
private void node_extractField(String[] array, int line) {
    try {
        Integer obj = null;
        
        // get the node ID
        obj = new Integer( array[NODE_ID].trim() );
        int id = obj.intValue();
        NodeNum++;
    }
    catch (Exception e) {
    	logger.log(Level.WARNING, "Exception reading file at line #" + line, e);
    }
}
    
/**
 * Reads a text file one line at the time
 * @param flName a file name
 * @return <code>true</code> if successful, <code>false</code> otherwise.
 * @throws IOException if the there was any error reading the file
 * @throws FileNotFoundException if the file was not found
 */
private boolean platform_readFile(String flName) throws IOException, FileNotFoundException {
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
 * Reads a text file one line at the time
 * @param flName a file name
 * @return <code>true</code> if successful, <code>false</code> otherwise.
 * @throws IOException if the there was any error reading the file
 * @throws FileNotFoundException if the file was not found
 */
private boolean node_readFile(String flName) throws IOException, FileNotFoundException {
    boolean success = false;
    BufferedReader reader = null;
    int line = 0;
    
    try {
        FileInputStream file = new FileInputStream(flName);
        reader = new BufferedReader(new InputStreamReader(file));

        // read one line at the time
        line = 1;
        while (reader.ready()) {
            parseValue(reader.readLine(), line);
            line++;
        }

        reader.close(); 
        success = true;
    } finally {
        if (reader != null) {
        	reader.close(); 
        }
    }
    return success;
}
}
