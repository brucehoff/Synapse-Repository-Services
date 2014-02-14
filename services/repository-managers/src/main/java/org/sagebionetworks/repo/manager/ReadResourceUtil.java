package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ReadResourceUtil {
	/**
	 * Helper for sending templated emails
	 * 
	 * Reads a resource into a string
	 */
	public static String readMailTemplate(String filename) {
		try {
			InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			try {
				String s = br.readLine();
				while (s != null) {
					sb.append(s + "\r\n");
					s = br.readLine();
				}
				return sb.toString();
			} finally {
				br.close();
				is.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
