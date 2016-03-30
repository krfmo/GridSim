/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.log;

/**
 * Defines names for package level loggers.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */
public enum LoggerEnum {
	/**
	 * GridSim package.
	 */
	GRIDSIM("gridsim."),
	
	/**
	 * Auction package.
	 */
	AUCTION(GRIDSIM.path + "auction"),
	
	/**
	 * Datagrid package and sub-packages.
	 */
	DATAGRID(GRIDSIM.path + "datagrid"),
	
	/**
	 * Network package.
	 */
	NET(GRIDSIM.path + "net"),
	
	/**
	 * Parallel package and sub-packages.
	 */
	PARALLEL(GRIDSIM.path + "parallel"),
	
	/**
	 * Resource failure modelling package.
	 */
	FAILURE(GRIDSIM.path + "resFailure"),
	
	/**
	 * Util package. 
	 */
	UTIL(GRIDSIM.path + "util"),
	
	/**
	 * Filter package.
	 */
	FILTER(GRIDSIM.path + "filter"),
	
	/**
	 * Index package.
	 */
	INDEX(GRIDSIM.path + "index");

	private String path = null;
	
	private LoggerEnum(String path) {
		this.path = path;
	}
	
	/**
	 * Returns the logger's path.
	 * @return the logger's path.
	 */
	public String getPath() {
		return path;
	}
	
}
