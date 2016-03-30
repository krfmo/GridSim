/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */
package gridsim.parallel.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * A log formatter based on the {@link SimpleFormatter} of java.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */
public class LogFormatter extends Formatter {
	Date dat = new Date();
    private static final String format = "{0,date} {0,time}";
    private MessageFormat formatter;

    private Object args[] = new Object[1];
    private String lineSeparator = (String) System.getProperty("line.separator");
    

	@Override
	public synchronized String format(LogRecord record) {
		StringBuffer sb = new StringBuffer();
		dat.setTime(record.getMillis());
		args[0] = dat;

		StringBuffer text = new StringBuffer();
		if (formatter == null) {
			formatter = new MessageFormat(format);
		}
		
		formatter.format(args, text, null);
		sb.append(text);
		sb.append(" ");
		
		if (record.getSourceClassName() != null) {
			sb.append(record.getSourceClassName().substring(
					record.getSourceClassName().lastIndexOf(".") + 1));
		} else {
			sb.append(record.getLoggerName());
		}
		
		if (record.getSourceMethodName() != null) {
			sb.append(".");
			sb.append(record.getSourceMethodName() + "()");
		}
		
		sb.append(" ");
		String message = formatMessage(record);
		sb.append(record.getLevel().getLocalizedName());
		sb.append(": ");
		sb.append(message);
		sb.append(lineSeparator);
		
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) { };
		}
		
		return sb.toString();
	}
}
