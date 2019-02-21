package com.columnchanger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class ContextProperties {
	private String slash = System.getProperty("file.separator");
	public File jarPath = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
	public String propertiesPath = jarPath.getParentFile().getAbsolutePath() + slash + "conf" + slash;
	public String resultPath = jarPath.getParentFile().getAbsolutePath() + slash + "results" + slash;
	private Properties props = null;

	public static String APPLICATIONPROPERTIES = "application.properties";

	private static ContextProperties obj = new ContextProperties();

	public static ContextProperties getInstance() {

		return obj;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("jarPath:\t" + jarPath.toString() + "\n");
		sb.append("propertiesPath:\t" + propertiesPath + "\n");
		sb.append("resultPath:\t" + resultPath + "\n");

		return sb.toString();
	}

	public List<String> getFileContent(String filename) {
		List<String> list = new ArrayList<String>();

		try {

			InputStream is = new FileInputStream(this.propertiesPath + filename);
			Scanner scanner = new Scanner(is);
			while (scanner.hasNextLine()) {
				list.add(scanner.nextLine());
			}

			scanner.close();

			return list;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			return list;
		}
	}

	public Properties getProperties() {
		InputStream is = null;

		try {
			if (props == null) {
				// is =
				// Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties");

				is = new FileInputStream(this.propertiesPath + APPLICATIONPROPERTIES);

				if (is != null) {
					props = new Properties();
					props.load(is);
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			props = null;

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
			
			if (props==null){
				System.out.println("Can't get \'" + APPLICATIONPROPERTIES + "\'. Check if file available in resources.");
				printClasspath();
			}
		}
		return props;
	}

	private void printClasspath() {
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			System.out.println(url.getFile());
		}
	}
}
