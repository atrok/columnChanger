package com.columnchanger.services;

import com.columnchanger.ConfigServiceHelper;
import com.columnchanger.ContextProperties;

public abstract class UtilService {

	protected ConfigServiceHelper confService=null;
	protected ContextProperties contextproperties=null;
	public UtilService(ContextProperties contextproperties, ConfigServiceHelper confService) throws Exception{
		
		this.contextproperties=contextproperties;
		this.confService=confService;

	}
	public abstract void execute() throws Exception;
}
