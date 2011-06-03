package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * <p>
 * Allows each DTO type to have its own special metadata.
 * Note: Reflection will be used to load each implementations as needed.  Therefore, 
 * the following class naming conventions should be used:</p>
 *  <code>		{DTO-Class_name}'MetadataProvider' 		</code>
 *  <p>
 * For example, the DTO for datasets has a classname of 'Dataset', so the TypeSpecificMetadataProvider
 * implentaion for datasts must be named:</p>
 * <code> 'DatasetMetadataProvider' </code>
 *  
 * @author jmhill
 *
 * @param <T>
 */
public interface TypeSpecificMetadataProvider<T extends Base> {

	/**
	 * Validate that the passed entity.
	 * @param entity
	 * @throws InvalidModelException 
	 */
	public void validateEntity(T entity) throws InvalidModelException;
	
	/**
	 * This method will be called before the given entity is returned to the client.
	 * Any type specific metadata should be added here.
	 * @param entity
	 * @param request
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void addTypeSpecificMetadata(T entity, HttpServletRequest request) throws DatastoreException, NotFoundException;

}
