package org.open.rdfs;

import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.open.Configuration;

public class RDFSExConfig extends Configuration {

	/*
	 * Local Path to model
	 */
	private Map<String, PackageEx> path2package = new HashMap<String, PackageEx>();

	public PackageEx getLocal(String path) {
		return path2package.get(path);
	}

	/**
	 * register the package for the local path {@code local_path}
	 * 
	 * @param local_path
	 *            the local path
	 * @param pack
	 *            the package to be registered
	 */
	public void registerLocal(String local_path, PackageEx pack) {
		if (!local_path.equals(pack.getPackage().getName())) {
			path2package.put(local_path, pack);
		}

	}

	/**
	 * unify a namespace if a namespace has not "#" at the end
	 * 
	 * @param namespace
	 * @return the unified namespace
	 */
	public String unifyNS(String namespace) {
		if (namespace == null)
			return namespace;
		return namespace.endsWith("#") ? namespace : namespace + "#";
	}

	/**
	 * <ol>
	 * <li>If the {@code model} contains a resource whose type is
	 * {@link org.apache.jena.vocabulary.OWL#Ontology OWL.Ontology}, return the
	 * namespace of the resource</li>
	 * <li>If the {@code model} contains a resource whose type is
	 * {@link org.apache.jena.vocabulary.RDFS#uri RDFS.uri}, return the namespace of
	 * the resource</li>
	 * <li>find a resource whose type is
	 * {@link org.apache.jena.vocabulary.RDFS#Class RDFS.Class}, return the
	 * namespace of the resource</li>
	 * </ol>
	 * 
	 * @param model
	 *            the model to be analyse
	 * @return the real namespace of a {@code model}
	 */
	public String getRealNamespace(Model model) {
		if (model != null) {
			List<String> uris = new ArrayList<String>();
			RDFSUtils.getOntology(model).forEach(resource -> {
				uris.add(resource.getURI());
			});
			if (uris.isEmpty()) {
				for (Resource subject : RDFSUtils.getClasses(model)) {
					if (subject.getURI() != null) {
						uris.add(subject.getNameSpace());
						break;
					}
				}
			}
			if (uris.isEmpty()) {
				for (Resource subject : RDFSUtils.getProperties(model)) {
					if (subject.getURI() != null) {
						uris.add(subject.getNameSpace());
						break;
					}
				}
			}
			return unifyNS(uris.get(0));
		}
		return null;
	}

	/**
	 * Read a model corresponding to a url<br>
	 * If the url is not <a href="http://www.w3.org/2001/XMLSchema#">XMLSchema</a>
	 * and not <a href="http://schema.org/#">schema.org</a> (Temporary
	 * handling, @TODO handle these two cases in the future)<br>
	 * <ol>
	 * <li>read the model using the {@code url}</li>
	 * <li>find the real namespace of the model</li>
	 * <li>write the model in TURTLE format locally</li>
	 * </ol>
	 * 
	 * @param url
	 *            the url for the model to be read
	 * @param format
	 *            the format of the model to be read
	 * @return an {@link Entry} entry where the key is the real namespace of the
	 *         model and the value is the newly read model
	 */
	public Map.Entry<String, Model> readWriteModel(String url, String format) {
		Model model = ModelFactory.createDefaultModel();
		String ns = url;
		if (!url.equals("http://www.w3.org/2001/XMLSchema#") && !url.equals("http://schema.org/#")
				&& !url.equals("http://spdx.org/rdf/terms#")) {
			System.out.println(url);
			model = model.read(url, format);
			// base = (OntModel) base.union(model);
			RDFSUtils.analyseModel(model);
			ns = getRealNamespace(model);
			if (url.equals(ns)) {
				try {
					model.write(new FileWriter("ontology/" + getPackageName(ns)), "TURTLE");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new AbstractMap.SimpleEntry<String, Model>(ns, model);
	}
}
