package com.acertainsupplychain.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

public class FileLogger {

	private final String fullPath;
	private final static String FILE_TYPE = "txt";

	private final static String PROPERTY_PATH = "filepath.properties";
	private final static String PROPERTY_NAME = "path";
	private final static String DEFAULT_FILENAME = "default_log";
	private String initialContent;

	// TODO Make custom exception instead of using root exception (Exception)
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

	private boolean validateFileTitle(String title) {
		if (title == null)
			// throw new
			// Exception("FileLogger: The given title cannot be null.");
			return false;
		if (title.isEmpty())
			// throw new
			// Exception("FileLogger: The given title cannot be empty.");
			return false;
		if (title.endsWith("/")) // TODO must be system generic
			// throw new Exception(
			// "FileLogger: The given title cannot be a folder.");
			return false;
		return true;
	}

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

	private String appendTitleToFolder(String folder, String title) {
		if (!folder.endsWith("/")) {// TODO must be system generic
			folder = folder + "/";// TODO must be system generic
		}
		return folder + title;
	}

	public void logToFile(String log, boolean addTimeStamp) {
		String string = log;
		if (addTimeStamp) {
			string = "[" + getTimeStamp() + "] " + log;
		}
		logToFile(string);
	}

	private String getTimeStamp() {
		Date date = new Date();
		return new Timestamp(date.getTime()).toString();
	}

	// TODO why is it synchronized?
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
			System.out.println("FileLogger: Could not write log to logfile.");
			e.printStackTrace();
		}
		try {
			fileWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
