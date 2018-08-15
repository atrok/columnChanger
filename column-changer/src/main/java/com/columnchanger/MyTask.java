package com.columnchanger;

import com.genesyslab.platform.applicationblocks.com.CfgObject;

public interface MyTask {
	
	public int size();
	public void save();
	public MyTask next();

}
