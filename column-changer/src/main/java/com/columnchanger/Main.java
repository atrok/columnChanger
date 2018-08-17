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
	private static String PERSON_OPTION_MATCH = "customized\\.workbin\\.email\\..+";
	private static String IW_SECTION = "interaction-workspace";
	private static String AG_OPTION1_MATCH = "workbin\\.email\\.IC\\.displayed-columns";
	private static String AG_OPTION_MATCH = "workbin\\.email\\..+";
	private static String AG_OPTION_VALUE = "MessageType,FromAddress,To,Subject,ReceivedAt";
	private static String AG_OPTION1_VALUE = "MessageType,FromAddress,To,Subject,ReceivedAt,Team_Code";
	private static int EXECTMOUT = 10;

	private static String APPLICATIONPROPERTIES = "application.properties";

	private static ContextProperties properties = ContextProperties.getProperties();

	public static void main(String[] args) {
		initLogger();
		Properties props = getProperties();
		List<MyTask> tasks = new ArrayList<MyTask>();

		if (props == null) {
			System.out.println("Can't get \'" + APPLICATIONPROPERTIES + "\'. Check if file available in resources.");
			printClasspath();

			return;
		}

		ConfigServiceHelper confService = null;

		try {
			// get our CfgApplication
			confService = new ConfigServiceHelper(props);
			confService.openService();

			// get list of person dbids

			if (props.getProperty("execution.tasks") != null) {
				List<String> items = Arrays.asList(props.getProperty("execution.tasks").split(","));

				if (null != props.getProperty("execution.timeout"))
					EXECTMOUT = Integer.parseInt(props.getProperty("execution.timeout"));

				System.out.println("execution.timeout is set to: " + EXECTMOUT + " msec");

				for (String item : items) {
					switch (item) {
					case "person":
						tasks.add(processPersons(confService));
						break;
					case "ag":
						tasks.add(processAgentGroups(confService));
						break;
					default:
						break;
					}
				}

				System.out.println("Starting Single Thread Executor to submit changes to CS");

				ExecutorService executor = Executors.newSingleThreadExecutor();
				for (MyTask task : tasks) {
					if (null != task)
						executor.execute(new SimpleRunnable(task, EXECTMOUT));
				}
				executor.shutdown();
				// dirty hack to make sure configuration server connection won't
				// get closed prematurely
				while (!executor.isTerminated()) {

				}
			} else {
				System.out.println("Tasks aren't defined. Please add execution.tasks option to " + APPLICATIONPROPERTIES
						+ " file. Values are comma-separated list of next values: person,ag");
			}

		} catch (ConfRegistrationException ex) {
			System.out.println("Client registration failed: " + ex.getErrorDescription());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			confService.releaseConfigService();
		}
	}

	private static Properties getProperties() {
		InputStream is = null;
		Properties props = null;
		try {
			// is =
			// Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties");

			is = new FileInputStream(properties.propertiesPath + APPLICATIONPROPERTIES);

			if (is != null) {
				props = new Properties();
				props.load(is);
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
		}
		return props;
	}

	private static List<String> getFileContent(String filename) {
		List<String> list = new ArrayList<String>();

		try {

			InputStream is = new FileInputStream(properties.propertiesPath+filename);
			Scanner scanner = new Scanner(is);
			while (scanner.hasNextLine()) {
				list.add(scanner.nextLine());
			}

			return list;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			return list;
		}
	}

	private static MyTask processPersons(ConfigServiceHelper confService) throws Exception {
		List<String> persons = getFileContent(PERSON_FILE);
		Pattern pattern_section = Pattern.compile(IW_SECTION);
		Pattern pattern_option = Pattern.compile(PERSON_OPTION_MATCH);

		Set<CfgPerson> updated_persons = new HashSet<CfgPerson>();

		System.out.println("Checking person objects");
		for (String dbid : persons) {
			List<String> found_options = new ArrayList<String>();

			CfgPerson agent = confService.getPerson(dbid);

			if (agent != null) {

				// System.out.println("Person :"+agent.getUserName()+"
				// dbid:"+agent.getObjectDbid());
				// System.out.println();
				KeyValueCollection agentOptions = agent.getUserProperties();

				System.out.println("Checking Person :" + agent.getUserName() + " dbid:" + agent.getObjectDbid());
				for (Object sectionObj : agentOptions) {
					KeyValuePair sectionKvp = (KeyValuePair) sectionObj;

					String section = sectionKvp.getStringKey();

					if (pattern_section.matcher(section).matches()) {
						for (Object recordObj : sectionKvp.getTKVValue()) {
							KeyValuePair recordKvp = (KeyValuePair) recordObj;

							if (pattern_option.matcher(recordKvp.getStringKey()).matches()) {
								// System.out.println(" \"" +
								// recordKvp.getStringKey() + "\" = \""
								// + recordKvp.getStringValue() + "\"");

								found_options.add(recordKvp.getStringKey());

							}
						}
					}
				}

				// remove found options from agent userproperties
				if (found_options.size() > 0) {
					for (String index : found_options) {
						KeyValueCollection k = agentOptions.getList(IW_SECTION);
						KeyValuePair deleted = k.remove(index);
					}
					System.out.println("Updating Person :" + agent.getUserName() + " dbid:" + agent.getObjectDbid());
					updated_persons.add(agent);
				}
			}
		}

		if (!updated_persons.isEmpty()) {
			System.out.println("Found persons to be updated: " + updated_persons.size());
			/*
			 * Iterator it=updated_persons.iterator(); while (it.hasNext()) {
			 * CfgPerson agent = (CfgPerson) it.next(); agent.save();
			 * System.out.println("Saved Person :" + agent.getUserName() +
			 * " dbid:" + agent.getObjectDbid()); }
			 * 
			 */
			return new PersonTask(updated_persons);
		}
		return null;
	}

	private static MyTask processAgentGroups(ConfigServiceHelper confService) throws Exception {
		List<String> groups = getFileContent(AG_FILE);
		Pattern pattern_section = Pattern.compile(IW_SECTION);
		Pattern pattern_option = Pattern.compile(AG_OPTION_MATCH);
		Pattern pattern_option1 = Pattern.compile(AG_OPTION1_MATCH);

		Set<CfgAgentGroup> updated_objects = new HashSet<CfgAgentGroup>();

		System.out.println("Checking Agent Groups");
		for (String dbid : groups) {
			List<KeyValuePair> found_options = new ArrayList<KeyValuePair>();

			CfgAgentGroup group = confService.getAgentGroup(dbid);
			if (group != null) {
				System.out.println("Checking AG :" + group.getGroupInfo().getName() + " dbid:" + group.getObjectDbid());
				KeyValueCollection options = group.getGroupInfo().getUserProperties();

				for (Object sectionObj : options) {
					KeyValuePair sectionKvp = (KeyValuePair) sectionObj;
					KeyValueCollection sectionkvlist = (KeyValueCollection) sectionKvp.getValue();

					String section = sectionKvp.getStringKey();

					if (pattern_section.matcher(section).matches()) {
						for (Object recordObj : sectionKvp.getTKVValue()) {
							KeyValuePair recordKvp = (KeyValuePair) recordObj;

							if (pattern_option.matcher(recordKvp.getStringKey()).matches()) {// workbin.email.*
								System.out.println("            \"" + recordKvp.getStringKey() + "\" = \""
										+ recordKvp.getStringValue() + "\"");

								String optionname = recordKvp.getStringKey();
								String[] a = optionname.split("\\.");

								if (!a[2].equals("IC") && a.length == 3) { // create
																			// option
																			// name
									// to be added as
									// KeyValuePair
									optionname = optionname + ".displayed-columns";
									found_options.add(new KeyValuePair(optionname, AG_OPTION_VALUE));

									System.out.println("added: " + optionname + " = " + AG_OPTION_VALUE);
								}

								if (pattern_option1.matcher(recordKvp.getStringKey()).matches()) {
									recordKvp.setStringValue(AG_OPTION1_VALUE);
									System.out.println("replaced on: " + AG_OPTION1_VALUE);
									// dirty hacking
									// adding empty value to satisfy
									found_options.add(null);
								}

							}

						}
					}
				}

				// remove found options from agent userproperties
				if (found_options.size() > 0) {

					for (KeyValuePair item : found_options) {
						if (null != item) {
							KeyValueCollection k = options.getList(IW_SECTION);

							if (k.getPair(item.getStringKey()) != null)
								k.remove(item.getStringKey());// to avoid
																// creating
																// duplicately
																// named options

							k.addPair(item);
						}
					}
					updated_objects.add(group);

				}
			}
		}
		if (!updated_objects.isEmpty()) {
			System.out.println("Found Agent Groups to be updated: " + updated_objects.size());

			/*
			 * Iterator it=updated_objects.iterator(); while(it.hasNext()){
			 * 
			 * CfgAgentGroup group=(CfgAgentGroup) it.next(); group.save();
			 * 
			 * System.out.println("Saved AG :" + group.getGroupInfo().getName()+
			 * " dbid:" + group.getObjectDbid()); }
			 */

			return new AgentGroupTask(updated_objects);
		}
		return null;
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

	private static void printClasspath() {
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			System.out.println(url.getFile());
		}
	}

}
