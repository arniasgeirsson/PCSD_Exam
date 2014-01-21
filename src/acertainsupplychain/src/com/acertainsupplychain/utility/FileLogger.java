package com.acertainsupplychain.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

/**
 * Class used to appends strings to a text file.
 * 
 * @author Arni
 * 
 */
public class FileLogger {

	private final String fullPath;
	private final static String FILE_TYPE = "txt";

	private final static String PROPERTY_PATH = "filepath.properties";
	private final static String PROPERTY_NAME = "logfilepath";
	private final static String DEFAULT_FILENAME = "default_log";
	private String initialContent;

	/**
	 * Initialize the FileLogger object with a specific title of the log file
	 * and the initial content of the file.
	 * 
	 * @param fileTitle
	 * @param initialContent
	 */
	public FileLogger(String fileTitle, String initialContent) {
		String title = fileTitle;
		if (!validateFileTitle(fileTitle)) {
			System.out.println("FileLogger: The given file title [" + fileTitle
					+ "] is invalid, using default file title ["
					+ DEFAULT_FILENAME + "]");
			title = DEFAULT_FILENAME;
		}
		this.initialContent = initialContent;
		if (this.initialContent == null) {
			this.initialContent = "";
		}

		fullPath = createFullPath(title, FILE_TYPE);
		// Create the file if it does not exist
		logToFile("", false);
	}

	/**
	 * Validates the given file title by returning true if it is valid.
	 * 
	 * @param title
	 * @return
	 */
	private boolean validateFileTitle(String title) {
		if (title == null)
			return false;
		if (title.isEmpty())
			return false;
		if (title.endsWith(System.getProperty("file.separator")))
			return false;
		return true;
	}

	/**
	 * This function creates the full file path specified by the given file
	 * title, type and the property file.
	 * 
	 * @param fileTitle
	 * @param type
	 * @return
	 */
	private String createFullPath(String fileTitle, String type) {

		Properties properties = new Properties();
		InputStream input = null;
		String fullTitle = fileTitle + "." + type;

		try {
			input = getClass().getClassLoader().getResourceAsStream(
					PROPERTY_PATH);
			properties.load(input);
			return appendTitleToFolder(properties.getProperty(PROPERTY_NAME),
					fullTitle);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// As a last resort try to fall back to the user home directory
		// http://stackoverflow.com/questions/585534/what-is-the-best-way-to-find-the-users-home-directory-in-java
		System.out.println("FileLogger: Could not read " + PROPERTY_PATH
				+ ", trying to use user 'home' directory instead.");
		return appendTitleToFolder(System.getProperty("user.home"), fullTitle);
	}

	/**
	 * Appends the file title to a folder path.
	 * 
	 * @param folder
	 * @param title
	 * @return
	 */
	private String appendTitleToFolder(String folder, String title) {
		if (!folder.endsWith(System.getProperty("file.separator"))) {
			folder = folder + System.getProperty("file.separator");
		}
		return folder + title;
	}

	/**
	 * Log a string to the associated log file.
	 * 
	 * @param log
	 *            , the string to log.
	 * @param addTimeStamp
	 *            , a boolean indicated whether or not a timestamp shall be
	 *            automatically prepending to the given string.
	 */
	public void logToFile(String log, boolean addTimeStamp) {
		String string = log;
		if (addTimeStamp) {
			string = "[" + getTimeStamp() + "] " + log;
		}
		logToFile(string);
	}

	/**
	 * Get a timestamp based on the current date and time.
	 * 
	 * @return a string representation of the timestamp.
	 */
	private String getTimeStamp() {
		Date date = new Date();
		return new Timestamp(date.getTime()).toString();
	}

	/**
	 * Log a string to the associated log file.
	 * 
	 * @param log
	 *            , the string to log.
	 */
	synchronized public void logToFile(String log) {
		String content = log;

		File file = new File(fullPath);

		if (!file.exists()) {
			content = initialContent + log;
		}

		if (file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(file, true);
			fileWriter.write(content);
		} catch (Exception e) {
			System.out.println("FileLogger: Could not write log to logfile"
					+ " with path [" + fullPath + "]");
			e.printStackTrace();
		}
		try {
			fileWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
