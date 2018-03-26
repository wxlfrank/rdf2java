package org.open.rdfs.java;

import java.util.ArrayList;
import java.util.List;

import org.open.rdfs.Type;

public class JavaType extends Type {

	public static final JavaType LIST = new JavaType(List.class.getName());
	public static final JavaType ARRAYLIST = new JavaType(ArrayList.class.getName());

	public JavaType(String fullname) {
		this.setContainer(null);
		this.setName(fullname);
	}
	public static void main(String[] args){
		System.out.println(LIST.getName());
	}
}
