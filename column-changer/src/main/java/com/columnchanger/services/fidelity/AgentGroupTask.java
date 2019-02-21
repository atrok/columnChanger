package com.columnchanger.services.fidelity;

import java.util.Iterator;
import java.util.Set;

import com.genesyslab.platform.applicationblocks.com.CfgObject;
import com.genesyslab.platform.applicationblocks.com.ConfigException;
import com.genesyslab.platform.applicationblocks.com.objects.CfgAgentGroup;
import com.genesyslab.platform.applicationblocks.com.objects.CfgPerson;

public class AgentGroupTask implements MyTask{

	private Set list=null;
	private CfgAgentGroup current=null;
	private Iterator it=null;
	
	public AgentGroupTask(Set list){
		this.list=list;
		this.it=this.list.iterator();
	}
	
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public AgentGroupTask next() {
		current=(CfgAgentGroup)this.it.next();
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
