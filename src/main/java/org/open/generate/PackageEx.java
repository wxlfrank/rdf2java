package org.open.generate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.tranform.RDFS2Java;

/**
 * The binding class to bind a model and its corresponding package
 * 
 * @author wxlfrank
 *
 */
public class PackageEx extends Binding {

	public PackageEx(RDFS2Java rdfs2java, Model model, Package result) {
		super(rdfs2java, model, result);
	}

	private Map<String, TypeEx> annoymous = new HashMap<String, TypeEx>();

	public Model getModel() {
		return (Model) getSource();
	}

	public TypeEx getType(String id) {
		return annoymous.get(id);
	}

	public Package getPackage() {
		return (Package) getTarget();
	}

	public ClassEx getClass(String id) {
		return (ClassEx) getChildren().get(id);
	}

	public ClassEx createClass(String local, Resource resource) {
		if (resource != null)
			System.out.println("create class " + local + " " + resource.getURI());
		Class cls = new Class(getPackage(), local);
		ClassEx result = new ClassEx(this, resource, cls);
		return result;
	}

	public Collection<Binding> getClassexes() {
		return getChildren().values();
	}

	@Override
	public String getHashString() {
		return getPackage().getName();
	}

	public ClassEx toClassEx(String id, Resource resource, boolean fill) {
		ClassEx type = getClass(id);
		if (type == null) {
			type = createClass(id, resource);
			if (fill)
				fillClassEx(resource, type);
			type.setSuperClass(getSuperClass(resource, fill));
		}
		return type;
	}

	private void fillClassEx(Resource domain, ClassEx cls) {
		Model model = domain.getModel();
		for (Resource property : RDFSUtils.getProperties(model)) {
			Set<Resource> domains = RDFSUtils.getDomains(property);
			if (domains.contains(domain))
				getParent().toFieldEx(this, property, cls);
		}
	}

	private ClassEx getSuperClass(Resource resource, boolean fill) {
		Resource superClass = RDFSUtils.getSuperClass(resource);
		if (superClass != null && !superClass.equals(resource)) {
			Entry<PackageEx, Resource> real = getParent().getRealResource(this, superClass);
			Resource realSuperClass = real.getValue();
			PackageEx realPackage = real.getKey();
			return realPackage.toClassEx(realSuperClass.getLocalName(), realSuperClass, fill || this != realPackage);
		}
		return null;
	}

	public RDFS2Java getParent() {
		return (RDFS2Java) super.getParent();
	}

}
