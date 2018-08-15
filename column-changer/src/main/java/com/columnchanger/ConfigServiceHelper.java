package com.columnchanger;

import java.util.Properties;

import com.genesyslab.platform.applicationblocks.com.ConfServiceFactory;
import com.genesyslab.platform.applicationblocks.com.IConfService;
import com.genesyslab.platform.applicationblocks.com.objects.CfgAgentGroup;
import com.genesyslab.platform.applicationblocks.com.objects.CfgApplication;
import com.genesyslab.platform.applicationblocks.com.objects.CfgPerson;
import com.genesyslab.platform.applicationblocks.com.queries.CfgAgentGroupQuery;
import com.genesyslab.platform.applicationblocks.com.queries.CfgApplicationQuery;
import com.genesyslab.platform.applicationblocks.com.queries.CfgPersonQuery;
import com.genesyslab.platform.commons.protocol.ChannelState;
import com.genesyslab.platform.commons.protocol.Endpoint;
import com.genesyslab.platform.commons.protocol.Protocol;
import com.genesyslab.platform.configuration.protocol.ConfServerProtocol;
import com.genesyslab.platform.configuration.protocol.types.CfgAppType;

/**
* Helper class to create, release Config Service and read Configuration Objects.
*
* See http://docs.genesys.com/Documentation/PSDK/latest/Developer/UsingtheCOMAB
*/
public class ConfigServiceHelper {

	private IConfService service;

	private String host;
	private int port;
	private String userName;
	private String userPassword;
	private String applicationName;
	private CfgAppType applicationType;

	/**
	 * Creates helper to communicate with Configuration Server.
	 * Properties specify Configuration Server address and user credentials. 
	 */
	public ConfigServiceHelper(Properties props) {
		this.host = props.getProperty("config.server.host");
		this.port = Integer.parseInt(props.getProperty("config.server.port"));
		this.userName = props.getProperty("config.server.user");
		this.userPassword = props.getProperty("config.server.password");
		this.applicationName = props.getProperty("application.name");
		this.applicationType = CfgAppType.valueOf(props.getProperty("application.type"));
	}

	/**
	 * Creates helper to communicate with Configuration Server. 
	 * Args represent Configuration Server address and user credentials. 
	 */
	public ConfigServiceHelper(String host, int port, String userName, String userPassword,
			String applicationName, CfgAppType applicationType) {

		this.host = host;
		this.port = port;
		this.userName = userName;
		this.userPassword = userPassword;
		this.applicationName = applicationName;
		this.applicationType = applicationType;

	}

	/**
	 * Creates IConfService object and opens connection to Configuration Server
	 * @throws Exception if IConfService wasn't created or Configuration Protocol wasn't opened.
	 */
	public void openService() throws Exception {
		try {

			ConfServerProtocol protocol = new ConfServerProtocol(new Endpoint(host, port));
			protocol.setUserName(userName);
			protocol.setUserPassword(userPassword);
			protocol.setClientName(applicationName);
			protocol.setClientApplicationType(applicationType.ordinal());

			service = ConfServiceFactory.createConfService(protocol);
			protocol.open();
			
			System.out.println("Connection to CS is opened, "+protocol.toString());

		} finally {
			if (service != null) {
				Protocol protocol = service.getProtocol();
				if (protocol.getState() != ChannelState.Opened) {
					releaseConfigService();
					service = null;
				}
			}
		}
	}

	/**
	 * Releases IConfService
	 */
	public void releaseConfigService() {
		try {
			System.out.println("Closing connection to CS");
			if (service != null) {
				Protocol protocol = service.getProtocol();
				if (protocol.getState() != ChannelState.Closed)
					protocol.close();
				ConfServiceFactory.releaseConfService(service);
			}
		} catch (Exception e) {
			System.out.println("Exception occured while releasing Config Service");
			e.printStackTrace();
		}
	}

	/**
	 * Tries to get CfgApplication using CfgApplicationQuery
	 * @param name application name
	 * @return CfgApplication
	 * @throws Exception
	 */
	public CfgApplication getApplication(String name) throws Exception {
		CfgApplicationQuery query = new CfgApplicationQuery(service);
		query.setName(name);
		return query.executeSingleResult();
	}
	
	/**
	 * Tries to get CfgPerson using CfgPersonQuery
	 * @param dbid person dbid
	 * @return CfgPerson
	 * @throws Exception
	 */
	public CfgPerson getPerson(String dbid) throws Exception {
		CfgPersonQuery query = new CfgPersonQuery(service);
		query.setDbid(Integer.parseInt(dbid));
		return query.executeSingleResult();
	}
	
	/**
	 * Tries to get CfgPerson using CfgPersonQuery
	 * @param dbid person dbid
	 * @return CfgPerson
	 * @throws Exception
	 */
	public CfgAgentGroup getAgentGroup(String dbid) throws Exception {
		CfgAgentGroupQuery query = new CfgAgentGroupQuery(service);
		query.setDbid(Integer.parseInt(dbid));
		return query.executeSingleResult();
	}
}
