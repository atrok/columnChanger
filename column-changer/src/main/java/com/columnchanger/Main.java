package com.columnchanger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import com.columnchanger.services.UtilService;
import com.columnchanger.services.fidelity.AgentGroupTask;
import com.columnchanger.services.fidelity.ColumnChangerFidelity;
import com.columnchanger.services.fidelity.MyTask;
import com.columnchanger.services.fidelity.PersonTask;
import com.columnchanger.services.fidelity.SimpleRunnable;
import com.columnchanger.services.vagstats.VAGStatsService;
import com.genesyslab.platform.applicationblocks.com.objects.CfgAgentGroup;
import com.genesyslab.platform.applicationblocks.com.objects.CfgPerson;
import com.genesyslab.platform.applicationblocks.com.queries.CfgPersonQuery;
import com.genesyslab.platform.commons.collections.KVList;
import com.genesyslab.platform.commons.collections.KeyValueCollection;
import com.genesyslab.platform.commons.collections.KeyValuePair;
import com.genesyslab.platform.commons.log.Log;
import com.genesyslab.platform.commons.log.Log4J2LoggerFactoryImpl;
import org.apache.logging.log4j.core.config.*;

import com.genesyslab.platform.configuration.protocol.exceptions.ConfRegistrationException;

public class Main {

	private static String AG_FILE = "ag.txt";
	private static String PERSON_FILE = "persons.txt";
	private static ContextProperties contextproperties = ContextProperties.getInstance();

	private static UtilService SERVICE = null;

	public static void main(String[] args) {

		initLogger();
		ConfigServiceHelper confService = null;
		try {
			Properties props = contextproperties.getProperties();

			// get our CfgApplication
			confService = new ConfigServiceHelper(props);
			confService.openService();

			// get mode

			String service=props.getProperty("service");
			
			if (service != null) {

				//System.out.println("Called service: " + service);

				switch (service) {
				case "columnchanger":
					SERVICE = new ColumnChangerFidelity(contextproperties, confService);
					break;
				case "vagstat":
					SERVICE=new VAGStatsService(contextproperties, confService);
					break;
				default:
					System.out.println("No service defined, exiting. define service=(columnchanger|vagstat) in Application Properties file" );
					return;
				}
				
				SERVICE.execute();
			}

		} catch (ConfRegistrationException ex) {
			System.out.println("Client registration failed: " + ex.getErrorDescription());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			confService.releaseConfigService();
		}
	}


	private static void initLogger() {
		ConfigurationSource source;
		InputStream is = null;
		try {
			ClassLoader ss = Thread.currentThread().getContextClassLoader();
			is = ss.getResourceAsStream("log4j2.xml");
			if (is == null) {
				System.out.println("Can't initialize log with \'log4j2.xml\'. Check if file available in resources.");
			} else {
				source = new ConfigurationSource(is);
				Configurator.initialize(null, source);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		Log.setLoggerFactory(new Log4J2LoggerFactoryImpl());
	}


}
