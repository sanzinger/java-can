package at.sanzinger.can;

import static java.lang.String.format;

import java.util.Date;

public class Log {
	public static void warn(String msg, Object ... values) {
		log("WARN", msg, values);
	}
	
	public static void info(String msg, Object ... values) {
		log("INFO", msg, values);
	}
	public static void error(String msg, Object ... values) {
		log("ERROR", msg, values);
	}
	
	public static void log(String level, String msg, Object ... values) {
		String out = format("%s [%s]: %s", new Date(), level, format(msg, values));
		System.out.println(out);
	}
}
