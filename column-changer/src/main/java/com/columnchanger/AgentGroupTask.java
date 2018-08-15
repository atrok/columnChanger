package com.columnchanger;

import java.util.Set;

import com.genesyslab.platform.applicationblocks.com.CfgObject;
import com.genesyslab.platform.applicationblocks.com.ConfigException;
import com.genesyslab.platform.applicationblocks.com.objects.CfgAgentGroup;
import com.genesyslab.platform.applicationblocks.com.objects.CfgPerson;

public class AgentGroupTask implements MyTask{

	private Set list=null;
	CfgAgentGroup current=null;
	public AgentGroupTask(Set list){
		this.list=list;
	}
	
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public AgentGroupTask next() {
		current=(CfgAgentGroup)list.iterator().next();
		return this;
	}

	@Override
	public void save() {
		try {
			current.save();
		} catch (ConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
