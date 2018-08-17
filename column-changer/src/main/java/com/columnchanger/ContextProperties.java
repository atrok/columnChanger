package com.columnchanger;

import java.io.File;

public class ContextProperties {
	private String slash=System.getProperty("file.separator");
	public File jarPath = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
	public String propertiesPath = jarPath.getParentFile().getAbsolutePath() + slash+"conf"+slash;
	public String resultPath = jarPath.getParentFile().getAbsolutePath() + slash+"results"+slash;
	

	private static ContextProperties obj=new ContextProperties();
	public static ContextProperties getProperties(){
		
		return obj;
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("jarPath:\t"+jarPath.toString()+"\n");
		sb.append("propertiesPath:\t"+propertiesPath+"\n");
		sb.append("resultPath:\t"+resultPath+"\n");
		
		return sb.toString();
	}
	
}
