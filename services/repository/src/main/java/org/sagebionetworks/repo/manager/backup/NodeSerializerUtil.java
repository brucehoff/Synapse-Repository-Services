package org.sagebionetworks.repo.manager.backup;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.web.NotFoundException;

import com.thoughtworks.xstream.XStream;

/**
 * A utility to read and right node backup data.
 * @author jmhill
 *
 */
public class NodeSerializerUtil  {
	
	private static final String ALIAS_NODE_BACKUP = "node-backup";
	private static final String ALIAS_ACCESS_TYPE = "access-type";
	private static final String ALIAS_RESOURCE_ACCESS = "resource-access";
	private static final String ALIAS_NODE_REVISION = "node-revision";
	private static final String ALIAS_ANNOTATIONS = "annotations";
	private static final String ALIAS_NAME_SPACE = "name-space";


	/**
	 * Write to a stream
	 * @param node
	 * @param out
	 * @throws NotFoundException
	 */
	public static void writeNodeBackup(NodeBackup node, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		writeNodeBackup(node, writer);
	}


	/**
	 * Write to a writer
	 * @param node
	 * @param writer
	 */
	public static void writeNodeBackup(NodeBackup node,	Writer writer) {
		// For now we just let xstream do the work
		XStream xstream = createXStream();
		xstream.toXML(node, writer);
	}


	/**
	 * Read from a stream
	 * @param in
	 * @return
	 */
	public static NodeBackup readNodeBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		NodeBackup backup = readNodeBackup(reader);
		return backup;
	}


	/**
	 * Read from a writer.
	 * @param reader
	 * @return
	 */
	public static NodeBackup readNodeBackup(Reader reader) {
		XStream xstream = createXStream();
		NodeBackup backup = new NodeBackup();
		xstream.fromXML(reader, backup);
		return backup;
	}
	
	public static void writeNodeRevision(NodeRevision revision, OutputStream out){
		OutputStreamWriter writer = new OutputStreamWriter(out);
		writeNodeRevision(revision, writer);
	}
	
	public static void writeNodeRevision(NodeRevision revision, Writer writer){
		XStream xstream = createXStream();
		xstream.toXML(revision, writer);
	}
	
	public static NodeRevision readNodeRevision(InputStream in){
		InputStreamReader reader = new InputStreamReader(in);
		return readNodeRevision(reader);
	}
	
	public static NodeRevision readNodeRevision(Reader reader){
		XStream xstream = createXStream();
		NodeRevision rev = new NodeRevision();
		xstream.fromXML(reader, rev);
		return rev;
	}
	
	private static XStream createXStream(){
		XStream xstream = new XStream();
		xstream.alias(ALIAS_NODE_BACKUP, NodeBackup.class);
		xstream.alias(ALIAS_ACCESS_TYPE, AuthorizationConstants.ACCESS_TYPE.class);
		xstream.alias(ALIAS_RESOURCE_ACCESS, ResourceAccess.class);
		xstream.alias(ALIAS_NODE_REVISION, NodeRevision.class);
		xstream.alias(ALIAS_ANNOTATIONS, Annotations.class);
		xstream.alias(ALIAS_NAME_SPACE, NamedAnnotations.class);
		return xstream;
	}

}
