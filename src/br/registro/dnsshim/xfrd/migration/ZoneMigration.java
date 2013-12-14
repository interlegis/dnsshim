package br.registro.dnsshim.xfrd.migration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import br.registro.dnsshim.common.repository.FileSystemUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.domain.DnskeyInfo;
import br.registro.dnsshim.xfrd.domain.DnskeyStatus;
import br.registro.dnsshim.xfrd.domain.SlaveGroup;
import br.registro.dnsshim.xfrd.domain.User;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;

public class ZoneMigration implements Migration {
	private static final String EXTENSION = ".properties";
	private String baseDir;
	private ZoneInfoDao dao;
	private EntityManager entityManager;
	
	private HashMap<String, ZoneInfo> zoneInfos;
	private TreeMap<String, String> signKeys;
	private TreeMap<String, String> publishKeys;
	private TreeMap<String, String> noneKeys;
	
	public ZoneMigration(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.dao = new ZoneInfoDao(entityManager);
		StringBuilder xfrdDir = new StringBuilder();
		// {DNSSHIM_HOME|user.home}/dnsshim/xfrd/zones/SHA1#1/SHA1#2/SHA1#3/<zones>.properties
		if (System.getenv("DNSSHIM_HOME") == null) {
			xfrdDir.append(System.getProperty("user.home"));
			xfrdDir.append(File.separatorChar);
			xfrdDir.append("dnsshim");
		} else {
			xfrdDir.append(System.getenv("DNSSHIM_HOME"));
		}
		xfrdDir.append(File.separatorChar);
		xfrdDir.append("xfrd");
		xfrdDir.append(File.separatorChar);
		baseDir = xfrdDir.toString();
		
		zoneInfos = new HashMap<String, ZoneInfo>();
		signKeys = new TreeMap<String, String>();
		publishKeys = new TreeMap<String, String>();
		noneKeys = new TreeMap<String, String>();
	}

	public void migrate() {

		List<String> zones = readZonenames();

		int count = 0;
		StringBuilder path = new StringBuilder();

		System.out.println((new Date()).toString() + ": Building zone paths ");
		TreeMap<String, String> zonenames = new TreeMap<String, String>();
		for (String zonename : zones) {
			try {
				String hash = FileSystemUtil.hash(zonename);
				path.delete(0, path.length());
				path.append(baseDir);
				path.append("zones");
				path.append(File.separatorChar);
				path.append(hash);
				path.append(File.separatorChar);
				path.append(zonename);
				path.append(EXTENSION);

				zonenames.put(path.toString(), zonename);
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}

		System.out.println((new Date()).toString() + ": Reading zone files");
		for (Entry<String, String> entries : zonenames.entrySet()) {
			try {
				File file = new File(entries.getKey());
				loadZone(file);
			} catch (IOException e) {
					System.out.println((new Date()).toString() + ": Error reading zone file " + entries.getKey());
			}
		}

		System.out.println((new Date()).toString() + ":" + zoneInfos.entrySet().size() + " zones read");

		System.out.println((new Date()).toString() + ": reading key files");
		for (Entry<String, String> entry : signKeys.entrySet()) {
			String keyPath = entry.getKey();
			String zonename = entry.getValue();
			loadKey(keyPath, zonename, DnskeyStatus.SIGN);
		}

		for (Entry<String, String> entry : publishKeys.entrySet()) {
			String keyPath = entry.getKey();
			String zonename = entry.getValue();
			loadKey(keyPath, zonename, DnskeyStatus.PUBLISH);
		}

		for (Entry<String, String> entry : noneKeys.entrySet()) {
			String keyPath = entry.getKey();
			String zonename = entry.getValue();
			loadKey(keyPath, zonename, DnskeyStatus.NONE);
		}

		System.out.println((new Date()).toString() + ": Saving to database");
		for (Entry<String, ZoneInfo> e : zoneInfos.entrySet()) {
			dao.save(e.getValue());
			count++;
			if (count % 10000 == 0) {
				entityManager.getTransaction().commit();
				System.out.println((new Date()).toString() + ": Committed " + count + " zones");
				entityManager.clear();
				entityManager.getTransaction().begin();				
			}	
		}

		System.out.println((new Date()).toString() + ": Done migrating " + zoneInfos.size() + " zones");
	}
	
	private List<String> readZonenames() {
		try {
			StringBuilder path = new StringBuilder(baseDir);
			path.append("conf");
			path.append(File.separatorChar);
			path.append("zonename.list");

			File file = new File(path.toString());
			BufferedReader br = null;
			List<String> zones = new ArrayList<String>();
			br = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					zones.add(line);
				}	
			}
			br.close();
			
			return zones;
		} catch (IOException e) {
			return null;
		}
	}
	
	private void loadZone(File file) throws IOException {
		Properties conf = new Properties();
		FileInputStream fis = new FileInputStream(file);
		conf.load(fis);
		fis.close();
		
		String zonename = conf.getProperty("zonename", ZoneInfo.DEFAULT_ZONENAME);
		if (zonename.equals(ZoneInfo.DEFAULT_ZONENAME)) {
			return;
		}
		
		// saved as long to be unsigned
		int maxIncrements = (int)Long.parseLong(conf.getProperty("max_increments", String.valueOf(ZoneInfo.DEFAULT_MAX_INCREMENTS)));
		int expiration = (int)Long.parseLong(conf.getProperty("expiration", String.valueOf(ZoneInfo.DEFAULT_VALIDITY)));
		int soa_minimum = (int)Long.parseLong(conf.getProperty("soa_minimum_ttl", String.valueOf(ZoneInfo.DEFAULT_SOA_MINIMUM)));
		int ttl_dnskey = (int)Long.parseLong(conf.getProperty("ttl_dnskey", String.valueOf(ZoneInfo.DEFAULT_TTL_DNSKEY)));
		int ttl_zone = (int)Long.parseLong(conf.getProperty("ttl_zone", String.valueOf(ZoneInfo.DEFAULT_TTL_ZONE)));
		String next_publication = conf.getProperty("next_publication", "19700101000000");
		boolean signed = Boolean.parseBoolean(conf.getProperty("signed", String.valueOf(ZoneInfo.DEFAULT_SIGNED)));
		
		String[] zoneSignKeys = conf.getProperty("keys_sign","").split(";");
		String[] zonePublishKeys = conf.getProperty("keys_publish","").split(";");
		String[] zoneNoneKeys = conf.getProperty("keys_none","").split(";");
		String[] slaves = conf.getProperty("slaves","").split(";");
		String[] users = conf.getProperty("users","").split(";");
		
		ZoneInfo zoneInfo = new ZoneInfo();
		zoneInfo.setMaxIncrements(maxIncrements);
		zoneInfo.setValidity(expiration);
		zoneInfo.setSoaMinimum(soa_minimum);
		zoneInfo.setTtlDnskey(ttl_dnskey);
		zoneInfo.setTtlZone(ttl_zone);
		zoneInfo.setZonename(zonename);
		
		zoneInfo.setSigned(signed);
		
		TimeZone timeZoneUTC = TimeZone.getTimeZone("UTC");
		TimeZone.setDefault(timeZoneUTC);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Date nextPub = null;
		try {
			nextPub = dateFormat.parse(next_publication);
		} catch (ParseException e) {
			nextPub = new Date();
			nextPub.setTime(0);
		}
		zoneInfo.setExpirationDate(nextPub);
		zoneInfo.scheduleResignDate();
		
	
		for (String keyname : zoneSignKeys) {
			if (keyname.isEmpty()) {
				continue;
			}
			
			String hash = FileSystemUtil.hash(keyname);
			
			StringBuilder keyPath = new StringBuilder();
			keyPath.append(baseDir);
			keyPath.append("keys");
			keyPath.append(File.separatorChar);
			keyPath.append(hash);
			keyPath.append(keyname);
			keyPath.append(".key");
			signKeys.put(keyPath.toString(), zonename);
		}
		
		for (String keyname : zonePublishKeys) {
			if (keyname.isEmpty()) {
				continue;
			}
			
			String hash = FileSystemUtil.hash(keyname);
			
			StringBuilder keyPath = new StringBuilder();
			keyPath.append(baseDir);
			keyPath.append("keys");
			keyPath.append(File.separatorChar);
			keyPath.append(hash);
			keyPath.append(keyname);
			keyPath.append(".key");
			publishKeys.put(keyPath.toString(), zonename);
		}

		for (String keyname : zoneNoneKeys) {
			if (keyname.isEmpty()) {
				continue;
			}
			
			String hash = FileSystemUtil.hash(keyname);
			
			StringBuilder keyPath = new StringBuilder();
			keyPath.append(baseDir);
			keyPath.append("keys");
			keyPath.append(File.separatorChar);
			keyPath.append(hash);
			keyPath.append(keyname);
			keyPath.append(".key");
			noneKeys.put(keyPath.toString(), zonename);
		}

		for (String slave : slaves) {
			if (slave.isEmpty()) {
				continue;
			}
			SlaveGroup sg = new SlaveGroup();
			sg.setName(slave);
			zoneInfo.assignSlaveGroup(sg);
		}
		
		for (String username : users) {
			if (username.isEmpty()) {
				continue;
			}
			
			User user = new User();
			user.setUsername(username);
			zoneInfo.addUser(user);
		}
		
		zoneInfos.put(zonename, zoneInfo);
	}
	
	private void loadKey(String keyPath, String zonename, DnskeyStatus keyStatus) {
		File file = new File(keyPath);
		String rdata = "";
		try {
			rdata = readPublicKey(file);
		} catch (IOException e) {
			System.out.println((new Date()).toString() + ": Error reading key file " + keyPath);
			return;
		}
		
		String keyname = file.getName().split("\\.")[0];
		DnskeyInfo dnskeyInfo = new DnskeyInfo();
		dnskeyInfo.setName(keyname);
		dnskeyInfo.setRdata(rdata);
		dnskeyInfo.setStatus(keyStatus);

		ZoneInfo zoneInfo = zoneInfos.get(zonename);
		dnskeyInfo.setZone(zoneInfo);
		zoneInfo.addKey(dnskeyInfo);
	}
	
	private String readPublicKey(File file) throws IOException {
		FileReader fileReader = new FileReader(file);
		BufferedReader reader = new BufferedReader(fileReader);
		String line = reader.readLine();
		line = line.split(";")[0];
		reader.close();
		fileReader.close();

		String[] fields = line.split(" ");
		if (fields.length < 6) {
			throw new IOException("Invalid key file: " + file);
		}

		if (fields[1].equalsIgnoreCase("DNSKEY") == false) {
			throw new IOException("Invalid key file: " + file);
		}

		StringBuilder rdata = new StringBuilder();
		rdata.append(fields[2]).append(" ");
		rdata.append(fields[3]).append(" ");
		rdata.append(fields[4]).append(" ");
		rdata.append(fields[5]).append(" ");
		
		return rdata.toString();
	}
}
