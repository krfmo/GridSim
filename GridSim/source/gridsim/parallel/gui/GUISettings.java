/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;

/**
 * This class stores settings shared by all the windows of the graphical
 * user interface.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */

class GUISettings {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	
	private Properties properties;
	private static final String DEF_PATH = "gridsim/parallel/gui/gui.properties";
	private static final String ppPrefix = "gridsim.gui.";
	
	private long timeSpan = 200; // the time span used by the windows
	
	// Colours used by the resource window that shows a resource's schedule
	private Color grBGColor, grColor, grBDColor;
	private Color gridColor, lbColor, xTextColor, tlColor;
	
	private BasicStroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_ROUND, 
			BasicStroke.JOIN_ROUND, 4, new float[]{2.0f}, 0);
	
	private BasicStroke normalStroke = new BasicStroke(1);
	private Font graphFont = new Font("Dialog", Font.BOLD, 10);
	
	private Composite transparentComposite = 
		AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);
		
	// parameters are read from configuration file to populate these palettes
	private Color[] cQueued, cDone, cExec, cARNonComm, cARComm, cARExec;
	private final Color[] colorQueues = 
		new Color[]{Color.WHITE, Color.RED, Color.GREEN, Color.CYAN, 
			Color.DARK_GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK, 
			Color.YELLOW, Color.BLUE};
	
	private static GUISettings settings = null;
	
    /**
     * Returns the single instance of the {@link GUISettings} 
     * object, creating it if it has not already been instantiated.
     * @return the GUI settings instance.
     */
    public static GUISettings getInstance() {
    	if(settings == null) {
    		settings = new GUISettings();
    	}

    	return settings;
    }
    
    private GUISettings() {
    	loadDefaultProperties();
    	loadSystemProperties();

    	// load colours at once
		grBGColor = createColor(properties.getProperty(ppPrefix + "resource.bgcolor"));
		grBDColor = createColor(properties.getProperty(ppPrefix + "resource.bdcolor"));
		grColor = createColor(properties.getProperty(ppPrefix + "resource.color"));
		gridColor = createColor(properties.getProperty(ppPrefix + "resource.gridcolor"));
		lbColor = createColor(properties.getProperty(ppPrefix + "resource.lbcolor"));
		xTextColor = createColor(properties.getProperty(ppPrefix + "resource.xcolor"));
		tlColor = createColor(properties.getProperty(ppPrefix + "resource.tlcolor"));
		cQueued = createPalette(properties.getProperty(ppPrefix + "resource.queuedpl"));
		cDone = createPalette(properties.getProperty(ppPrefix + "resource.donepl"));
		cExec = createPalette(properties.getProperty(ppPrefix + "resource.execpl"));
		cARNonComm = createPalette(properties.getProperty(ppPrefix + "resource.arncpl"));
		cARComm = createPalette(properties.getProperty(ppPrefix + "resource.arcpl"));
		cARExec = createPalette(properties.getProperty(ppPrefix + "resource.arexecpl"));
    }
    
    /**
     * Gets the time span for the GUI components
     * @return the time span for the GUI components
     */
	public long getTimeSpan() {
		return timeSpan;
	}

	/**
	 * Sets the time span for the GUI components
	 * @param timeSpan the time span for the GUI components
	 * @return <code>true</code> if the time span has been updated
	 * or <code>false</code> otherwise.
	 */
	public boolean setTimeSpan(long timeSpan) {
		boolean success = false;
		if(timeSpan > this.timeSpan)  {
			this.timeSpan = timeSpan;
			success = true;
		}
		
		return success;
	}

	/**
	 * Returns a dashed stroke object to be used to draw
	 * time lines on the GUI windows
	 * @return the dashed stroke
	 */
	public BasicStroke getDashedStroke() {
		return dashedStroke;
	}

	/**
	 * Returns a normal stroke to be used to draw
	 * the generic lines on the windows
	 * @return the generic stroke
	 */
	public BasicStroke getNormalStroke() {
		return normalStroke;
	}

	/**
	 * Returns the background colour for the graphs
	 * of scheduling queues 
	 * @return the background colour
	 */
	public Color getGraphBGColor() {
		return grBGColor;
	}

	/**
	 * Returns the colour of the border of the area of the graphs
	 * @return the colour of the border of the area of the graphs
	 */
	public Color getGraphBDColor() {
		return grBDColor;
	}
	
	/**
	 * Returns the colour of the area of the graph
	 * @return the colour of the graph area
	 */
	public Color getGraphAreaColor() {
		return grColor;
	}

	/**
	 * Returns the colour of the time grid
	 * @return the colour of the time grid
	 */
	public Color getTimeGridColor() {
		return gridColor;
	}

	/**
	 * Returns the colour of the top text
	 * @return the colour of the top text
	 */
	public Color getLabelColor() {
		return lbColor;
	}
	
	/**
	 * Returns the colour of the text in the x axis of the graphs
	 * @return the colour of the text in the x axis of the graphs
	 */
	public Color getXAxisTextColor() {
		return xTextColor;
	}
	
	/**
	 * Returns the colour of the current time line
	 * @return the colour of the current time line
	 */
	public Color getTimeLineColor() {
		return tlColor;
	}

	/**
	 * Returns the font to be used in the graphs
	 * @return the font to be used in the graphs
	 */
	public Font getGraphFont() {
		return graphFont;
	}

	/**
	 * Returns a composite object to be used to draw objects that
	 * overlap in the resource windows
	 * @return a composite object to be used to draw objects that
	 * overlap in the resource windows
	 */
	public Composite getTransparentComposite() {
		return transparentComposite;
	}

	/**
	 * Return colours for job queues or partitions
	 * @return the colours for the partitions
	 */
	public Color[] getQueueColors() {
		return colorQueues;
	}
	
	/**
	 * Returns colours for waiting jobs.
	 * @return an array of colours
	 */
	public Color[] getJobQueuedColors() {
		return cQueued;
	}

	/**
	 * Returns colours for jobs completed.
	 * @return an array of colours
	 */
	public Color[] getJobDoneColors() {
		return cDone;
	}

	/**
	 * Returns colours for jobs in execution.
	 * @return an array of colours
	 */
	public Color[] getJobInExecColors() {
		return cExec;
	}
	
	/**
	 * Returns colours for advance reservations not confirmed by the user
	 * @return an array of colours
	 */
	public Color[] getARNonCommittedColors() {
		return cARNonComm;
	}
	
	/**
	 * Returns colours for advance reservations confirmed by the user
	 * @return an array of colours
	 */
	public Color[] getARCommittedColors() {
		return cARComm;
	}
	
	/**
	 * Returns colours for advance reservations in progress
	 * @return an array of colours
	 */
	public Color[] getARInProgressColors() {
		return cARExec;
	}
	
	/**
	 * Get the value of a property.
	 * @param key the property's key
	 * @return the value associated to the key.
	 */
	protected String getProperty(String key) {
		synchronized (properties) {
			return this.properties.getProperty(key);
		}
	}
	    
    /* Reads default properties */
	private void loadDefaultProperties() {
		Properties default_properties = new Properties();
		InputStream in = GUISettings.class.getClassLoader().getResourceAsStream(DEF_PATH);
		try {
			default_properties.load(in);
			in.close();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Error reading properties.", ex);
		}
		
		properties = new Properties(default_properties);
	}

	/* Loads the system properties whose names start with gridsim.gui */
	private void loadSystemProperties() {
		for (Object p : System.getProperties().keySet()) {
			String key = ((String) p).toLowerCase();
			if(key.startsWith(ppPrefix)) {
				properties.setProperty(key, System.getProperty((String) p));
			}
		}
	}
	    
	/* Creates a colour object based on a R,G,B string. E.g. 20,45,255 */
	private static Color createColor(String color) {
		Color result = null;
		try { 
			String[] sColors = color.trim().split(",");
			if (sColors.length < 3) {
				throw new IllegalArgumentException("Invalid color string: " + color);
			}
			
			int red = Integer.parseInt(sColors[0]);
			int green = Integer.parseInt(sColors[1]);
			int blue = Integer.parseInt(sColors[2]);
			
			result = new Color(red, green, blue);
			
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error creating color from string: " + color, e);
			result = null;
		}
		
		return result;
	}
	
	
	/* Creates a colour object based on a R,G,B,increment,number_colours string. */
	private static Color[] createPalette(String color) {
		Color[] result = null;
		try { 
			String[] sColors = color.trim().split(",");
			if (sColors.length < 5) {
				throw new IllegalArgumentException("Invalid color palette string: " + color);
			}
			
			int red = Integer.parseInt(sColors[0]);
			int green = Integer.parseInt(sColors[1]);
			int blue = Integer.parseInt(sColors[2]);
			int dec = Integer.parseInt(sColors[3]);
			int nColors = Integer.parseInt(sColors[4]);
			
			result = createPalette(red, green, blue, dec, nColors); 
			
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error creating color pallete: " + color, e);
			result = null;
		}
		
		return result;
	}
	
	/*
	 * Creates a colour palette
	 * @return the colour palette
	 */
	private static Color[] createPalette(int red, int green, 
			int blue, int dec, int numColors) {
		boolean[] itRGB = new boolean[] {false, false, false};
		Color[] colors = new Color[numColors];
		int smallest = 0;
		int largest = 0;
	
		if(red <= green && red <= blue) {
			itRGB[0] = true;
			smallest = red;
		} else {
			largest = red;
		}
		
		if(green <= red && green <= blue) {
			itRGB[1] = true;
			smallest = green;
		} else {
			largest = green;
		}

		if(blue <= red && green <= green) {
			itRGB[2] = true;
			smallest = blue;
		} else {
			largest = blue;
		}
		
		int genCol = 0;
		for(int i=smallest; i >= 0; i-=dec) {
			if(genCol == numColors) {
				break;
			}

			red = (itRGB[0]) ? i : red;
			green = (itRGB[1]) ? i : green;
			blue = (itRGB[2]) ? i : blue;
			colors[genCol] = new Color(red, green, blue);
			genCol++;
		}
		
		for(int i=largest-51; i >= 0; i-=dec) {
			if(genCol == numColors) {
				break;
			}

			red = (!itRGB[0]) ? i : red;
			green = (!itRGB[1]) ? i : green;
			blue = (!itRGB[2]) ? i : blue;
			colors[genCol] = new Color(red, green, blue);
			genCol++;
		}
		
		return colors;
	}
}
