package com.columnchanger.services.vagstats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.columnchanger.ConfigServiceHelper;
import com.columnchanger.ContextProperties;
import com.columnchanger.services.UtilService;
import com.genesyslab.platform.applicationblocks.com.objects.CfgAgentGroup;
import com.genesyslab.platform.applicationblocks.com.objects.CfgDN;
import com.genesyslab.platform.commons.collections.KeyValueCollection;
import com.genesyslab.platform.commons.collections.KeyValuePair;

public class VAGStatsService extends UtilService {

	private String VAG_SECTION = "virtual";
	private String SCRIPT_OPTION_MATCH = "script";
	private Pattern pattern_section = Pattern.compile(VAG_SECTION);
	private Pattern pattern_option = Pattern.compile(SCRIPT_OPTION_MATCH);

	private HashMap<String, String> result = new HashMap<String, String>();
	private HashMap<BigInteger, Integer> vag_scripts_count = new HashMap<BigInteger, Integer>();
	private HashMap<BigInteger, String> vag_duplicated = new HashMap<BigInteger, String>();
	private HashMap<BigInteger, String> vag_scripts = new HashMap<BigInteger, String>();
	private HashMap<String, Integer> vag_routedns_count = new HashMap<String, Integer>();
	private HashMap<String, Integer> vag_population_count = new HashMap<String, Integer>();
	private HashMap<String, Integer> vag_state = new HashMap<String, Integer>();
	private HashMap<String, BigInteger> vag_hash = new HashMap<String, BigInteger>();

	private MD5Hash hashgenerator = new MD5Hash();

	public VAGStatsService(ContextProperties contextproperties, ConfigServiceHelper confService) throws Exception {
		super(contextproperties, confService);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void execute() throws Exception {
		// TODO Auto-generated method stub
		try {

			long start = Instant.now().getEpochSecond();
			// get all agent groups
			ArrayList<CfgAgentGroup> agentgroups = (ArrayList<CfgAgentGroup>) confService.getAllAgentGroup();
			result.put("Agent Groups, Total", Integer.toString(agentgroups.size()));
			ArrayList<CfgAgentGroup> vags = getVAGs(agentgroups);

			result.put("Virtual Agent Groups, Total", Integer.toString(vags.size()));

			findAgentGroupsPopulation();
			findDuplicatedAgentGroups();

			// findAgentGroupsWithoutRouteDN();

			System.out.println("----------------------------------------");
			System.out.println("Execution time: " + (Instant.now().getEpochSecond() - start) + " sec");
			System.out.println("========================================");

		} catch (Exception ex) {
			System.out.println("Service vagstat experienced exception");
			throw ex;
		}

	}

	private void findDuplicatedAgentGroups() {
		FileWriter writer = new FileWriter("vags_duplicated");
		try {


			writer.write("hash,vags,script");

			vag_scripts_count.forEach((k, v) -> {
				if (v > 1) {
					try {
						String vags = vag_duplicated.get(k);
						String script = (vag_scripts.containsKey(k)) ? vag_scripts.get(k) : "";

						writer.write(k + "," + vags + "," + script);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}catch(Exception e){
			System.out.println("General exception: "+e.getMessage());
			e.printStackTrace();
		}finally{
			writer.close();
		}


	}

	private void findAgentGroupsWithoutRouteDN() {
		System.out.println("========================================");
		System.out.println("VAGs without configured RouteDNs:");
		System.out.println("----------------------------------------");
		int total = 0;

		vag_routedns_count.forEach((k, v) -> {
			if (v == 0) {
				System.out.println(k);

			}
		});

	}

	private void findAgentGroupsPopulation() throws IOException {
		System.out.println("========================================");
		System.out.println("VAGs population:");
		System.out.println("----------------------------------------");
		int total = 0;
		try {

			long enabled = vag_state.values().stream().map(s -> Integer.valueOf(s)).filter(number -> number == 1)
					.count();
			HashMap<String, Integer> disabled = (HashMap<String, Integer>) StreamsOperations.filterByValue(vag_state,
					number -> number == 2);
			long disabled_populated = disabled.keySet().stream().filter(key -> vag_population_count.get(key) > 0)
					.count();
			HashMap<String, Integer> disabled_population = (HashMap<String, Integer>) StreamsOperations
					.filterByKey(vag_population_count, key -> vag_state.get(key) == 2);
			HashMap<String, Integer> duplicated_vags = (HashMap<String, Integer>) StreamsOperations
					.filterByKey(vag_population_count, key -> vag_scripts_count.get(vag_hash.get(key)) > 1);

			long members_count = StreamsOperations.sumByValue(vag_population_count, v -> v == v); // v==v
																									// to
																									// count
																									// all
																									// values
			int zero_populated = StreamsOperations.countByValue(vag_population_count, v -> v == 0);
			int sum_duplicated = vag_scripts_count.values().stream().mapToInt(i -> (i > 1) ? i : 0).sum();
			int duplicated_population = StreamsOperations.sumByValue(duplicated_vags, v -> v == v);
			int duplicated_with_zero_population = StreamsOperations.countByValue(duplicated_vags, v -> v == 0);

			System.out.println("Total: " + vag_hash.size());
			System.out.println("VAGs population, total: " + members_count);
			System.out.println("Enabled groups: " + enabled);
			System.out.println("- out of which with 0 population: " + zero_populated);
			System.out.println("- VAGs with duplicated script expressions: " + sum_duplicated);
			System.out.println("-- out of which with 0 population: " + duplicated_with_zero_population);
			System.out.println("-- population: " + duplicated_population);
			System.out.println("Disabled groups: " + disabled.size());
			System.out.println("- with members: " + disabled_populated);
			System.out.println("- Disabled VAGs population, total: "
					+ StreamsOperations.sumByValue(disabled_population, v -> v == v));

			FileWriter writer = new FileWriter("vags");

			writer.write("vag,population,state,routedn,hash");

			vag_population_count.forEach((key, val) -> {
				int state = vag_state.get(key);
				int rdn = vag_routedns_count.get(key);
				BigInteger hash = vag_hash.get(key);
				try {
					// System.out.println(key + "," + val + "," + state + "," +
					// rdn);
					writer.write(key + "," + val + "," + state + "," + rdn + "," + hash);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(Exception e){
			System.out.println("General exception: "+e.getMessage());
			e.printStackTrace();
		}
	}

	private ArrayList<CfgAgentGroup> getVAGs(ArrayList<CfgAgentGroup> agentgroups) {
		ArrayList<CfgAgentGroup> vags = new ArrayList<CfgAgentGroup>();
		for (CfgAgentGroup group : agentgroups) {
			if (isVag(group))
				vags.add(group);
		}
		return vags;
	}

	private boolean isVag(CfgAgentGroup group) {
		KeyValueCollection options = group.getGroupInfo().getUserProperties();

		for (Object sectionObj : options) {
			KeyValuePair sectionKvp = (KeyValuePair) sectionObj;
			KeyValueCollection sectionkvlist = (KeyValueCollection) sectionKvp.getValue();

			String section = sectionKvp.getStringKey();

			if (pattern_section.matcher(section).matches()) { // found VAG!
				for (Object recordObj : sectionKvp.getTKVValue()) { // looking
																	// for
																	// 'script'
																	// option
					KeyValuePair recordKvp = (KeyValuePair) recordObj;

					if (pattern_option.matcher(recordKvp.getStringKey()).matches()) { // found
																						// it
						// System.out.println(" \"" +
						// recordKvp.getStringKey() + "\" = \""
						// + recordKvp.getStringValue() + "\"")

						BigInteger hash = hashgenerator.generate(recordKvp.getStringValue());

						/// finding duplicated VAGs

						if (vag_scripts_count.containsKey(hash)) {
							Integer i = vag_scripts_count.computeIfPresent(hash, (k, v) -> v = v + 1);
							String h = vag_duplicated.computeIfPresent(hash,
									(k, v) -> v.concat(":" + group.getGroupInfo().getName()));
							// System.out.println(i+","+h);;
						}
						vag_scripts_count.putIfAbsent(hash, 1);
						vag_duplicated.putIfAbsent(hash, group.getGroupInfo().getName());
						vag_scripts.putIfAbsent(hash, recordKvp.getStringValue());

						vag_hash.put(group.getGroupInfo().getName(), hash);

						/// Find ROuteDNs
						Collection<Integer> dns = group.getGroupInfo().getRouteDNDBIDs();
						vag_routedns_count.put(group.getGroupInfo().getName(), (dns != null) ? dns.size() : 0);
						Collection<Integer> agent_dbids = group.getAgentDBIDs();
						vag_population_count.put(group.getGroupInfo().getName(),
								(agent_dbids != null) ? agent_dbids.size() : 0);
						vag_state.put(group.getGroupInfo().getName(), group.getGroupInfo().getState().asInteger());

						return true;

					}
				}
			}
		}
		return false;
	}

	private static class StreamsOperations {
		/*
		 * Map<String, Integer> filteredMap = filterByValue(originalMap, value
		 * -> value == 2);
		 */

		public static <K, V> Map<K, V> filterByValue(Map<K, V> map, Predicate<V> predicate) {
			return map.entrySet().stream().filter(entry -> predicate.test(entry.getValue()))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		}

		public static <K, V> Map<K, V> filterByKey(Map<K, V> map, Predicate<K> predicate) {
			return map.entrySet().stream().filter(entry -> predicate.test(entry.getKey()))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		}

		public static <K, V> Integer sumByValue(Map<K, Integer> map, Predicate<Integer> predicate) {

			return map.values().stream().mapToInt(Integer::intValue).filter(entry -> predicate.test(entry)).sum();

		}

		public static <K, V> Integer countByValue(Map<K, Integer> map, Predicate<Integer> predicate) {

			return (int) map.values().stream().mapToInt(Integer::intValue).filter(entry -> predicate.test(entry))
					.count();

		}
	}

	private class FileWriter {
		private BufferedWriter bf = null;
		Path createdFile = null;

		public FileWriter(String filename) {

			try{
			createdFile = Files.createFile(getFilename(filename));

			System.out.println("Storing results to: " + createdFile);
			}catch(IOException e){
				System.out.println("Can't create file "+createdFile+" : "+e.getMessage());
				e.printStackTrace();
			}
		}

		private Path getFilename(String f) {
			LocalDateTime date = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss.SSS");
			String text = date.format(formatter);

			String fname = f.concat("_").concat(text).concat(".out");

			return Paths.get("./" + fname);
		}

		public void write(String line) throws IOException {

			try {
				if (bf == null) {

					bf = Files.newBufferedWriter(createdFile, StandardOpenOption.WRITE);
				}
				line = line + "\n";
				bf.write(line);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void close(){
			try{
			bf.close();
			}catch(IOException e){
				System.out.println("Can't flush buffer, "+e.getMessage());
				e.printStackTrace();
			}
		}

	}

	private class MD5Hash {

		private MessageDigest m = null;
		BigInteger bigInt = null;

		public MD5Hash() throws NoSuchAlgorithmException {
			m = MessageDigest.getInstance("MD5");
		}

		public BigInteger generate(String str) {

			m.reset();
			m.update(str.getBytes());
			byte[] digest = m.digest();
			bigInt = new BigInteger(1, digest);
			return bigInt;
		}

		public String toString() {
			return bigInt.toString(16);
		}
	}
}
