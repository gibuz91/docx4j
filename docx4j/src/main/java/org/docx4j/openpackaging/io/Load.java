/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */

package org.docx4j.openpackaging.io;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.docx4j.jaxb.Context;
import org.docx4j.model.datastorage.CustomXmlDataStorage;
import org.docx4j.model.datastorage.CustomXmlDataStorageImpl;
import org.docx4j.openpackaging.contenttype.ContentTypeManager;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.exceptions.PartUnrecognisedException;
import org.docx4j.openpackaging.packages.OpcPackage;
import org.docx4j.openpackaging.parts.CustomXmlDataStoragePropertiesPart;
import org.docx4j.openpackaging.parts.ExternalTarget;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart;
import org.docx4j.openpackaging.parts.WordprocessingML.ImageGifPart;
import org.docx4j.openpackaging.parts.WordprocessingML.ImageJpegPart;
import org.docx4j.openpackaging.parts.WordprocessingML.ImagePngPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.relationships.Relationship;


public class Load {

	private static Logger log = Logger.getLogger(Load.class);
	


	public Load() {
		super();
	}

	public ContentTypeManager ctm;

	protected boolean loadExternalTargets = false; 
	public void loadExternalTargets(boolean loadExternalTargets) {
		this.loadExternalTargets = loadExternalTargets;
	}
	
	/**
	 * This HashMap is intended to prevent loops.
	 */
	protected HashMap<String, String> handled = new HashMap<String, String>();
	
	/**
	 * TODO.  I'd prefer this not to be static, but it needs to be,
	 * given that getRawPart is.  Maybe its not such a big deal,
	 * because its reasonable to assume that most people using docx4j
	 * will standardise on a single implementation of CustomXmlDataStorage? 
	 */
	static protected CustomXmlDataStorage customXmlDataStorageClass = null;
	/**
	 * Set your preferred implementation of the CustomXmlDataStorage
	 * interface.  Its factory method will be used to create new instances.
	 *  
	 * @param customXmlDataStorageClass the customXmlDataStorageClass to set
	 */
	static public void setCustomXmlDataStorageClass(
			CustomXmlDataStorage customXmlDataStorageClassVal) {
		customXmlDataStorageClass = customXmlDataStorageClassVal;
	}
	
	/**
	 * @return the customXmlDataStorageClass
	 */
	static public CustomXmlDataStorage getCustomXmlDataStorageClass() {
		try {
			if (customXmlDataStorageClass==null) {
				customXmlDataStorageClass = new CustomXmlDataStorageImpl();
			}			
			return customXmlDataStorageClass;
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Get a Part (except a relationships part), but not its relationships part
	 * or related parts.  Useful if you need quick access to just this part,
	 * or if you wish to add a foreign part (ie a part from some other package).
	 * This can be called directly from outside the library, in which case 
	 * the Part will not be owned by a Package until the calling code makes it so.  
	 * @see  To get a Part and all its related parts, and add all to a package, use
	 * getPart.
	 * @param is
	 * @param ctm the ContentTypeManager associated with the foreign package
	 * @param resolvedPartUri  the part name
	 * @return
	 * 
	 * @throws URISyntaxException
	 * @throws InvalidFormatException
	 */
	public static Part getRawPart(InputStream is, ContentTypeManager ctm, String resolvedPartUri)
			throws Docx4JException {
		
		Part part = null;

		try {
						
			try {
				
				part = ctm.getPart("/" + resolvedPartUri);
								
				
				if (part instanceof org.docx4j.openpackaging.parts.ThemePart) {

					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).setJAXBContext(Context.jcThemePart);
					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).unmarshal( is );
					
				} else if (part instanceof org.docx4j.openpackaging.parts.DocPropsCorePart ) {

					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).setJAXBContext(Context.jcDocPropsCore);
					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).unmarshal( is );
					
				} else if (part instanceof org.docx4j.openpackaging.parts.DocPropsCustomPart ) {

					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).setJAXBContext(Context.jcDocPropsCustom);
					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).unmarshal( is );
					
				} else if (part instanceof org.docx4j.openpackaging.parts.DocPropsExtendedPart ) {

					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).setJAXBContext(Context.jcDocPropsExtended);
					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).unmarshal( is );
					
				} else if (part instanceof org.docx4j.openpackaging.parts.CustomXmlDataStoragePropertiesPart ) {

					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).setJAXBContext(Context.jcCustomXmlProperties);
					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).unmarshal( is );
				
				} else if (part instanceof org.docx4j.openpackaging.parts.JaxbXmlPart) {
					
					// MainDocument part, Styles part, Font part etc

					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).setJAXBContext(Context.jc);
					((org.docx4j.openpackaging.parts.JaxbXmlPart)part).unmarshal( is );
					
				} else if (part instanceof org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart) {
					
					log.debug("Detected BinaryPart " + part.getClass().getName() );
					((BinaryPart)part).setBinaryData(is);
					
				} else if (part instanceof org.docx4j.openpackaging.parts.CustomXmlDataStoragePart ) {
					
					CustomXmlDataStorage data = getCustomXmlDataStorageClass().factory();					
					data.setDocument(is); // Not necessarily JAXB, that's just our method name
					((org.docx4j.openpackaging.parts.CustomXmlDataStoragePart)part).setData(data);																				
										
				} else {
					// Shouldn't happen, since ContentTypeManagerImpl should
					// return an instance of one of the above, or throw an
					// Exception.
					
					log.error("No suitable part found for: " + resolvedPartUri);
					return null;					
				}
			} catch (PartUnrecognisedException e) {
				
				// Try to get it as a binary part				
				part = new BinaryPart(new PartName("/" + resolvedPartUri));
				((BinaryPart) part).setBinaryData(is);
					
			}
		} catch (Exception ex) {
			// PathNotFoundException, ValueFormatException, RepositoryException, URISyntaxException
			ex.printStackTrace();
			throw new Docx4JException("Failed to getPart", ex);
		} 
		return part;
	}
		
	public static BinaryPart getExternalResource(String absoluteTarget) throws Docx4JException {

		try {
			FileObject fo = VFS.getManager().resolveFile(absoluteTarget);
			
			ExternalTarget externalTarget = new ExternalTarget(absoluteTarget);
			
			// Assume it is a binary part, though there is no reason in principle
			// that it couldn't be an XML part..			
			BinaryPart bp;
			if (absoluteTarget.toLowerCase().endsWith(".gif" )) {
				
				bp = new ImageGifPart(externalTarget); 
				
			} else if  (absoluteTarget.toLowerCase().endsWith(".jpeg" )
					|| absoluteTarget.toLowerCase().endsWith(".jpg" )) {
				
				bp = new ImageJpegPart(externalTarget); 
				
			} else if (absoluteTarget.toLowerCase().endsWith(".png" )) {
				
				bp = new ImagePngPart(externalTarget); 
				
			} else {
				log.warn("Using simple BinaryPart for " + absoluteTarget);
				bp = new BinaryPart(externalTarget);
			}
			
			FileContent fc = fo.getContent();
			bp.setBinaryData(fc.getInputStream());			
			
			return bp;
			
		} catch (FileSystemException exc) {
			exc.printStackTrace();
			throw new Docx4JException("Couldn't get FileObject", exc);			
		}
		
	}
	
	/**
	 * Find any /customXml/itemN.xml which have a props part
	 * which specifies a data store item ID.  
	 * 
	 * Register such parts.
	 * 
	 * @param p
	 */
	public static void registerCustomXmlDataStorageParts(OpcPackage pkg) {
		
		HashMap<PartName, Part> parts = pkg.getParts().getParts();
		
		// Strictly speaking, we're only interested in CustomXmlDataStorageParts
		// which are referred to in document.xml.rels ?
		// But it doesn't do much harm to register a CustomXmlDataStoragePart
		// which has a data store item ID, even if it isn't in document.xml.rels 
		
		//Iterator iterator = parts.entrySet().iterator();
		Collection col = parts.values();
		Iterator iterator = col.iterator();
		while( iterator.hasNext() ) {
			Part entry = (Part)iterator.next();
			
			if (entry instanceof org.docx4j.openpackaging.parts.CustomXmlDataStoragePart) {
				log.debug("Found a CustomXmlDataStoragePart, named " + entry.getPartName().getName() );
				if (entry.getRelationshipsPart()==null) { continue; }
				log.debug(".. it has a rels part");
				// Look in its rels for rel of @Type customXmlProps (eg @Target="itemProps1.xml")
				Relationship r = entry.getRelationshipsPart().getRelationshipByType(
						Namespaces.CUSTOM_XML_DATA_STORAGE_PROPERTIES);
				if (r==null) {
					log.debug(".. but that doesn't point to a  customXmlProps part");
					continue;
				}
				CustomXmlDataStoragePropertiesPart customXmlProps = 
					(CustomXmlDataStoragePropertiesPart)entry.getRelationshipsPart().getPart(r);
				if (customXmlProps==null) {
					log.error(".. but the target seems to be missing?");
					continue;
				}
				String itemId = customXmlProps.getItemId();
				log.info("Identified/registered ds:itemId " + itemId);
				pkg.getCustomXmlDataStorageParts().put(itemId, 
						(org.docx4j.openpackaging.parts.CustomXmlDataStoragePart)entry );
			}
		}			
		
		
		
		
	}

}