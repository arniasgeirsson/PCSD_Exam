package com.acertainsupplychain.performance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Class used to write data to an excel document which is specified in the
 * filepath.properties file.
 * 
 * Note that this class is one that I have used for several other projects
 * before including one of the course assignments. This is the same file as used
 * in the course assignment only with minor alterations.
 * 
 * @author Arni
 * 
 */
public class PerformanceLogger {

	private final static String SHEET_NAME = "Performance Data";
	private final static String PROPERTY_PATH = "filepath.properties";
	private final static String PROPERTY_NAME = "performancelogpath";
	private final static String IDENTIFIER_PROPERTY_NAME = "performanceidentifier";
	private boolean firstTime;

	private final String fullPath;

	/**
	 * Initialize the PerformanceLogger object. The function creates the
	 * hardcoded sheet column 'titles'.
	 */
	public PerformanceLogger() {
		fullPath = readFullPath();
		if (fullPath == null)
			return;
		firstTime = false;
		File file = new File(fullPath);
		if (!file.exists()) {
			Object[] columNames = new Object[] { "Identifier", "Time-Stamp",
					"# OM's", "# ops", "# ops/OM", "# executeStep",
					"% executeStep", "# getOrdersPerItem",
					"% getOrdersPerItem", "Avg. ns per executeStep (latency)",
					"Avg. ms per executeStep (latency)",
					"Avg. ns per getOrdersPerItem (latency)",
					"Avg. ms per getOrdersPerItem (latency)", "# item IDs",
					"Avg. Latency - NanoSeconds/Operation",
					"Agg. Throughput - Operations/nanoSeconds",
					"Avg. Latency - MiliSeconds/Operation",
					"Agg. Throughput - Operations/miliSeconds",
					"Total run time in NanoSeconds",
					"Total run time in Miliseconds", };
			// Blank workbook
			XSSFWorkbook workbook = new XSSFWorkbook();

			// Create a blank sheet
			XSSFSheet sheet = workbook.createSheet(SHEET_NAME);
			firstTime = true;
			writeRowToSheet(sheet, columNames);
			firstTime = false;
			writeToFile(workbook);
		}
	}

	/**
	 * This function writes a collection of PerformanceLogs to the file.
	 * 
	 * @param logs
	 */
	public void writeLogs(Collection<PerformanceLog> logs) {
		if (logs == null)
			return;
		for (PerformanceLog performanceLog : logs) {
			writeLog(performanceLog);
		}
	}

	/**
	 * This function writes a single PerformanceLog object to the file.
	 * 
	 * @param log
	 */
	public void writeLog(PerformanceLog log) {
		if (log == null)
			return;
		writeRowAndFile(log.toList());
	}

	/**
	 * This function writes a row into the sheet and to the file. It writes a
	 * given list of objects, which must either be strings or integers as a row
	 * in the file and tries to write to the next empty row in the file.
	 * 
	 * @param list
	 *            , the list of objects to write to the file.
	 */
	private void writeRowAndFile(Object[] list) {
		XSSFWorkbook workbook;
		try {
			workbook = new XSSFWorkbook(new FileInputStream(new File(fullPath)));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		XSSFSheet sheet = workbook.getSheet(SHEET_NAME);
		writeRowToSheet(sheet, list);
		writeToFile(workbook);
	}

	/**
	 * Writes a row of objects to a given sheet.
	 * 
	 * @param sheet
	 * @param rowData
	 */
	private void writeRowToSheet(XSSFSheet sheet, Object[] rowData) {
		// Iterate over data and write to sheet
		int rownum = sheet.getLastRowNum();
		if (firstTime) {
			rownum = -1;
		}
		Row row = sheet.createRow(++rownum);
		int cellnum = 0;

		if (!firstTime) {
			Cell cell = row.createCell(cellnum++);
			cell.setCellValue(getIdentifer());
			cell = row.createCell(cellnum++);
			cell.setCellValue(getTimeStamp());
		}

		for (Object obj : rowData) {
			Cell cell = row.createCell(cellnum++);
			if (obj instanceof String) {
				String stringRep = (String) obj;
				// Note assuming that float are represented with dots as decimal
				// pointers
				cell.setCellValue(stringRep/* .replaceAll("\\.", ",") */);
			} else if (obj instanceof Integer) {
				cell.setCellValue((Integer) obj);
			}
		}
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
	 * This function reads in the fullpath name of the excel file from the
	 * property file and property name as specified.
	 * 
	 * @return the full path name. Null if the property file or property does
	 *         not exist.
	 */
	private String readFullPath() {
		return readPropertyFromPropertyFile(PROPERTY_PATH, PROPERTY_NAME);
	}

	/**
	 * This functions reads in the optional identifier name from the specified
	 * property file.
	 * 
	 * @return the identifier name. Null if the property file or property does
	 *         not exist.
	 */
	private String getIdentifer() {
		return readPropertyFromPropertyFile(PROPERTY_PATH,
				IDENTIFIER_PROPERTY_NAME);
	}

	/**
	 * Reads a property from a specified property file with a specified property
	 * name.
	 * 
	 * @param filename
	 *            , the filename of the property file.
	 * @param propertyName
	 *            , the property name of the property.
	 * @return the value of the property. Null if the property file or property
	 *         does not exist.
	 */
	private String readPropertyFromPropertyFile(String filename,
			String propertyName) {
		Properties properties = new Properties();
		InputStream input = null;

		try {
			input = getClass().getClassLoader().getResourceAsStream(filename);
			properties.load(input);
			return properties.getProperty(propertyName);
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
		return null;
	}

	/**
	 * Writes a workbook to the file used in this class.
	 * 
	 * @param workbook
	 */
	private void writeToFile(XSSFWorkbook workbook) {
		try {
			// Write the workbook in file system
			File file = new File(fullPath);
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}

			FileOutputStream out = new FileOutputStream(file);
			workbook.write(out);
			out.close();
			// System.out.println("PerformanceLogger wrote " + fullPath
			// + " successfully to disk.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("PerformanceLogger failed to write " + fullPath
					+ " to disk.");
		}
	}
}