package org.open.rdf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
	private Map<String, String> uri2pac = new HashMap<String, String>();

	public String getPackageName(String ns) {
		if (ns.isEmpty())
			return ns;

		String result = uri2pac.get(ns);
		if (result != null)
			return result;
		int index = ns.indexOf("//");
		boolean isPackage = index == -1;
		String newNS = index == -1 ? ns : ns.substring(index + 2);
		index = newNS.indexOf("www.");
		isPackage = isPackage && index == -1;
		newNS = index == -1 ? newNS : newNS.substring(index + 4);
		newNS = newNS.replaceAll("-", "_").replaceAll("#", "");
		String domain = null, last = null;
		index = newNS.indexOf("/");
		if (index == -1) {
			domain = newNS;
		} else {
			isPackage = false;
			domain = newNS.substring(0, index);
			last = newNS.substring(index + 1);
		}
		if (!isPackage) {
			List<String> domains = Arrays.asList(domain.split("\\."));
			Collections.reverse(domains);
			domain = String.join(".", domains);
		}
		if (last != null) {
			if (last.endsWith("/"))
				last = last.substring(0, last.length() - 1);
			last = last.replaceAll("/", "_").replaceAll("\\.", "_");
			if (!last.isEmpty()) {
				if (Character.isDigit(last.charAt(0)))
					last = "_" + last;
				domain += "." + last;
			}
		}
		uri2pac.put(ns, domain);
		return domain;
	}
}
