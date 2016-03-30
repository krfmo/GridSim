/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */
package gridsim.parallel.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Implements default logging behaviour in case the user does not specify any.
 *  
 * @author Marcos Dias de Assuncao
 */
public class Logging {
	private static Level level = Level.INFO;
	private static Formatter formatter = new LogFormatter();
	
	static {
		loadLoggers();
	}
	
	private Logging() {
		throw new UnsupportedOperationException("Logging cannot be instantiated");
	}
	
	/**
	 * Returns the corresponding log object.
	 * @param log the path of logger
	 * @return the logger object
	 */
	public static Logger getLogger(LoggerEnum log) {
		return LogManager.getLogManager().getLogger(log.getPath());
	}
	
	/**
	 * Sets the logging level for all default loggers.
	 * @param lv the logging level
	 */
	public static void setLevel(Level lv) {
		level = lv;
		loadLoggers();
	}
	
	/**
	 * Reinitialise the logging properties and reread the logging configuration 
	 * from the given stream, which should be in {@link java.util.Properties} format.
	 * <p>
	 * Any log level definitions in the new configuration file will be 
	 * applied using Logger.setLevel(), if the target Logger exists. 
	 * @param ins  stream to read properties from 
	 * @throws IOException if there are problems reading from the stream.
	 * @throws SecurityException if a security manager exists and if the 
	 * caller does not have LoggingPermission("control").
	 */
	public void readConfiguration(InputStream ins) 
						throws IOException, SecurityException {
		removeDefaultHandlers();
		LogManager.getLogManager().readConfiguration(ins);
	}
	
	private static void loadLoggers() {
		for(LoggerEnum log : LoggerEnum.values()) {
			Logger logger = Logger.getLogger(log.getPath());
			logger.setLevel(level);
			logger.setUseParentHandlers(false);

			for(Handler handler : logger.getHandlers()){
				logger.removeHandler(handler);
			}
			
			Handler hd = new ConsoleHandler();
			hd.setFormatter(formatter);
			logger.addHandler(hd);
			
			LogManager.getLogManager().addLogger(logger);
		}
	}
	
	private static void removeDefaultHandlers() {
		for(LoggerEnum log : LoggerEnum.values()) {
			Logger logger = Logger.getLogger(log.getPath());
			logger.setLevel(level);
			logger.setUseParentHandlers(false);

			for(Handler handler : logger.getHandlers()){
				logger.removeHandler(handler);
			}
	
			LogManager.getLogManager().addLogger(logger);
		}
	}
}
