package com.columnchanger.services.fidelity;

import java.util.Set;

import com.genesyslab.platform.applicationblocks.com.ConfigException;

public class SimpleRunnable implements Runnable {

	private MyTask list = null;
	int executionDelay = 500;

	public SimpleRunnable(MyTask list, int executionDelay) {
		this.list = list;
		this.executionDelay = executionDelay;

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			for (int i = 0; i < list.size(); i++) {

				list.next().save();
				Thread.sleep(executionDelay);

			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
