/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Processes API problem filter files into a collection of reference filters. The
 * filters can later be applied to a set of references to exclude references with
 * matching to/from types and members.
 * 
 * @since 1.0.500
 */
public class ReferenceFilterStore {

	/**
	 * Describes a single reference filter. Contents can be accessed via fields.
	 */
	class ReferenceFilter{
		/**
		 * The type being referenced, possibly <code>null</code>
		 */
		public String referencedTypeName = null;
		/**
		 * The local type referencing the {@link #referencedTypeName}, possibly <code>null</code>
		 */
		public String localTypeName = null;
		/**
		 * The field or method from {@link #localTypeName} that is referencing {@link #referencedTypeName}, possibly <code>null</code> 
		 */
		public String fieldOrMethodName = null; 
		
		/**
		 * Constructs a new reference filter with available information. 
		 * @param referencedTypeName
		 * @param localTypeName
		 * @param fieldOrMethodName
		 */
		public ReferenceFilter(String referencedTypeName, String localTypeName, String fieldOrMethodName){
			this.referencedTypeName = referencedTypeName;
			this.localTypeName = localTypeName;
			this.fieldOrMethodName = fieldOrMethodName;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof ReferenceFilter){
				ReferenceFilter filter = (ReferenceFilter)obj;
				if ((filter.referencedTypeName == null && referencedTypeName == null) || (filter.referencedTypeName != null && filter.referencedTypeName.equals(referencedTypeName))){
					if ((filter.localTypeName == null && localTypeName == null) || (filter.localTypeName != null && filter.localTypeName.equals(localTypeName))){
						if ((filter.fieldOrMethodName == null && fieldOrMethodName == null) || (filter.fieldOrMethodName != null && filter.fieldOrMethodName.equals(fieldOrMethodName))){
							return true;
						}
					}
				}
			}
			return false;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			int hashCode = 1;
			if (referencedTypeName != null){
				hashCode += referencedTypeName.hashCode();
			}
			if (localTypeName != null){
				hashCode += localTypeName.hashCode();
			}
			if (fieldOrMethodName != null){
				hashCode += fieldOrMethodName.hashCode();
			}
			return hashCode;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Reference Filter: " + referencedTypeName + " | " + localTypeName + " | " + fieldOrMethodName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	/**
	 * Fully qualified type name to a IApiProblem
	 */
	private Map/*<String, IApiProblem>*/ fFilterMap;
	private int fFilterCount;
	private boolean debug = false;
	
	/**
	 * Constructs a new filter store for the given componentID, processing all appropriate
	 * api filters files found in the directory specified by the filtersRoot string path.
	 * 
	 * @param filterRoot path to folder containing api problem filters files
	 * @param componentID string id of the component this filter store is for
	 * @param debug whether to print out debug statements to the system out stream
	 */
	public ReferenceFilterStore(String filterRoot, String componentID, boolean debug){
		this.debug = debug;
		this.initialize(filterRoot, componentID);
	}
	
	/**
	 * Disposes of any resources this filter store is using
	 */
	public void dispose(){
		// Map should be handled by GC, but clear it in case something is holding on to a filter
		fFilterMap.clear();
		fFilterMap = null;
		fFilterCount = 0;
	}
	
	/**
	 * Returns whether to filter the given reference because it matches one of 
	 * the filters in this filter store.
	 *  
	 * @param reference the reference to compare against filters, the member's type must be available
	 * @return <code>true</code> if the reference matches a filter
	 */
	public boolean isFiltered(IReference reference){
		IApiElement member = reference.getMember();
		while (member.getType() != IApiElement.TYPE){
			member = member.getParent();
			if (member == null){
				// No parent type could be found
				return false;
			}
		}
		
		// member.getName gives qualified name for types
		Set filters = (Set)fFilterMap.get(member.getName());
		
		
		// XXX Remove
		System.out.println("Search filters for " + member.getName());
		if (filters != null){
			System.out.println(filters.size() + " filters available for " + member.getName());
		}
		
		
		if (filters != null){
			// Referencing types match, check referenced type names and method/field names
			// TODO
			
		}
		
		
		return false;
	}
	
	/**
	 * Returns the number of filters in this filter store
	 * 
	 * @return number of filters in this filter store
	 */
	public int getFilterCount(){
		return fFilterCount;
	}
	
	/**
	 * Initialize the filter store using the given component id
	 */
	private void initialize(String filtersRoot, String componentID) {
		if(fFilterMap != null) {
			return;
		}
		fFilterCount = 0;
		fFilterMap = new HashMap(5);
		String xml = null;
		InputStream contents = null;
		try {
			File filterFileParent = new File(filtersRoot, componentID);
			if (!filterFileParent.exists()) {
				if(this.debug) {
					System.out.println("No filters found for component " + componentID); //$NON-NLS-1$
				}
				return;
			}
			contents = new BufferedInputStream(new FileInputStream(new File(filterFileParent, IApiCoreConstants.API_FILTERS_XML_NAME)));
			xml = new String(Util.getInputStreamAsCharArray(contents, -1, IApiCoreConstants.UTF_8));
		}
		catch(IOException ioe) {}
		finally {
			if (contents != null) {
				try {
					contents.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
		if(xml == null) {
			return;
		}
		Element root = null;
		try {
			root = Util.parseDocument(xml);
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
		if (root == null) {
			return;
		}
		if (!root.getNodeName().equals(IApiXmlConstants.ELEMENT_COMPONENT)) {
			return;
		}
		String component = root.getAttribute(IApiXmlConstants.ATTR_ID);
		if(component.length() == 0) {
			return;
		}

		int version = loadIntegerAttribute(root, IApiXmlConstants.ATTR_VERSION);
		if (version < 2) {
			if(this.debug) {
				System.out.println("All filters of versions earlier than 2 are discarded because there is no way to retrieve the type name"); //$NON-NLS-1$
			}
			// we discard all filters since there is no way to retrieve the type name
			return;
		}
		
		NodeList resources = root.getElementsByTagName(IApiXmlConstants.ELEMENT_RESOURCE);
		for(int i = 0; i < resources.getLength(); i++) {
			Element element = (Element) resources.item(i);
			String typeName = element.getAttribute(IApiXmlConstants.ATTR_TYPE);
			if(typeName == null || typeName.length() == 0) {
				// Only problems with types are used to filter references
				continue;
			}
			NodeList filters = element.getElementsByTagName(IApiXmlConstants.ELEMENT_FILTER);
			for(int j = 0; j < filters.getLength(); j++) {
				element = (Element) filters.item(j);
				int id = loadIntegerAttribute(element, IApiXmlConstants.ATTR_ID);
				if(id <= 0) {
					continue;
				}
				String[] messageargs = null;
				NodeList elements = element.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
				if (elements.getLength() != 1) continue;
				Element messageArguments = (Element) elements.item(0);
				NodeList arguments = messageArguments.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
				int length = arguments.getLength();
				messageargs = new String[length];
				for (int k = 0; k < length; k++) {
					Element messageArgument = (Element) arguments.item(k);
					messageargs[k] = messageArgument.getAttribute(IApiXmlConstants.ATTR_VALUE);
				}

				ReferenceFilter reference = recoverReference(ApiProblemFactory.newApiProblem(null, typeName, messageargs, null, null, -1, -1, -1, id));
				
				// XXX Remove
				System.out.println(reference);
				
				
				if (reference != null){
					Set filterSet = (Set) fFilterMap.get(typeName);
					if(filterSet == null) {
						filterSet = new HashSet();
						fFilterMap.put(typeName, filterSet);
					}
					filterSet.add(reference);
					fFilterCount++;
				}
			}
		}
		
		if (debug){
			System.out.println(fFilterCount + " reference filters found for component " + componentID); //$NON-NLS-1$
		}
	}
	
	private int loadIntegerAttribute(Element element, String name) {
		String value = element.getAttribute(name);
		if(value.length() == 0) {
			return -1;
		}
		try {
			int number = Integer.parseInt(value);
			return number;
		}
		catch(NumberFormatException nfe) {}
		return -1;
	}
	
	
	/**
	 * Returns a reference object created by parsing the message arguments, type and id of the given problem or
	 * <code>null</code> if the problem does not reflect a reference.  The returned reference may be missing information
	 * and the type/method names may not be fully qualified.
	 * 
	 * @param problem the problem to recover a reference from
	 * @return a reference containing all information collected from the problem or <code>null</code> if a reference cannot be recovered
	 */
	private ReferenceFilter recoverReference(IApiProblem problem) {
		if (problem.getCategory() == IApiProblem.CATEGORY_USAGE){
			/*
			#api usage problems
			#{0} = referenced type or member name
			#{1} = local type name
			#{2} = field or method name
			#{3} = required execution environment
			*/
			String referencedTypeName = null;
			String localTypeName = null;
			String fieldOrMethodName = null; 
			String[] arguments = problem.getMessageArguments();
			if (arguments.length > 0){
				referencedTypeName = arguments[0];
				if (arguments.length > 1){
					localTypeName = arguments[1];
					if (arguments.length > 2){
						fieldOrMethodName = arguments[2];
					}
				}
				return new ReferenceFilter(referencedTypeName, localTypeName, fieldOrMethodName);
			}
			
		}
		return null;
	}

}
