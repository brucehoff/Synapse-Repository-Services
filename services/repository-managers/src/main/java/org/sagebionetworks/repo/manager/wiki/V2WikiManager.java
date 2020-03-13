package org.sagebionetworks.repo.manager.wiki;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserAuthorization;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for the V2 Wiki manager.
 * (Derived from org.sagebionetworks.repo.manager.wiki.WikiManager) 
 * @author hso
 *
 */
public interface V2WikiManager {

	/**
	 * Create a Wiki page for a given object.
	 * @param userAuthorization
	 * @param objectId
	 * @param objectType
	 * @param toCreate
	 * @return
	 * @throws NotFoundException 
	 */
	V2WikiPage createWikiPage(UserAuthorization userAuthorization, String objectId,	ObjectType objectType, V2WikiPage toCreate) throws NotFoundException, UnauthorizedException;

	/**
	 * Get a wiki page for a given object.
	 * @param userAuthorization
	 * @param version TODO
	 * @param objectId
	 * @param objectType
	 * @param wikiId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	V2WikiPage getWikiPage(UserAuthorization userAuthorization, WikiPageKey key, Long version) throws NotFoundException, UnauthorizedException;
	
	/**
	 * Get the root wiki page for an object.
	 * @param userAuthorization
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	V2WikiPage getRootWikiPage(UserAuthorization userAuthorization, String objectId, ObjectType objectType) throws NotFoundException, UnauthorizedException;

	/**
	 * Delete a wiki page.
	 * @param userAuthorization
	 * @param wikiPageKey
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteWiki(UserAuthorization userAuthorization, WikiPageKey wikiPageKey) throws UnauthorizedException, DatastoreException, NotFoundException;

	/**
	 * Update a wiki page if allowed.
	 * @param userAuthorization
	 * @param objectId
	 * @param objectType
	 * @param toUpdate
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	V2WikiPage updateWikiPage(UserAuthorization userAuthorization, String objectId, ObjectType objectType, V2WikiPage toUpdate) throws NotFoundException, UnauthorizedException;

	/**
	 * 
	 * @param userAuthorization
	 * @param objectId
	 * @param objectType
	 * @param version
	 * @param wikiId
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	V2WikiPage restoreWikiPage(UserAuthorization userAuthorization, String objectId, ObjectType objectType, Long version, String wikiId) throws NotFoundException, UnauthorizedException;

	/**
	 * 
	 * @param userAuthorization
	 * @param ownerId
	 * @param type
	 * @param limit
	 * @param offest
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	PaginatedResults<V2WikiHeader> getWikiHeaderTree(UserAuthorization userAuthorization, String ownerId, ObjectType type,	Long limit, Long offest) throws DatastoreException, NotFoundException;

	/**
	 * Get the attachment file handles for a give wiki page.
	 * @param userAuthorization
	 * @param wikiPageKey
	 * @param version TODO
	 * @return
	 * @throws NotFoundException 
	 */
	FileHandleResults getAttachmentFileHandles(UserAuthorization userAuthorization, WikiPageKey wikiPageKey, Long version) throws NotFoundException;
	
	/**
	 * Get the FileHandle ID for a given WikiPage and file name.
	 * @param wikiPageKey
	 * @param fileName
	 * @param version TODO
	 * 
	 * @return
	 */
	String getFileHandleIdForFileName(UserAuthorization userAuthorization, WikiPageKey wikiPageKey, String fileName, Long version) throws NotFoundException, UnauthorizedException;

	/**
	 * Get the markdown file handle for a wiki page.
	 * @param userAuthorization
	 * @param wikiPageKey
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	String getMarkdownFileHandleId(UserAuthorization userAuthorization, WikiPageKey wikiPageKey, Long version) throws NotFoundException, UnauthorizedException;
	
	/**
	 * @param userAuthorization
	 * @param ownerId
	 * @param type
	 * @param wikiPageKey
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException
	 */
	PaginatedResults<V2WikiHistorySnapshot> getWikiHistory(UserAuthorization userAuthorization, String ownerId, ObjectType type, WikiPageKey wikiPageKey, Long limit, Long offset) throws NotFoundException, DatastoreException;

	/**
	 * Gets the order hint associated with the given object id and object type.
	 * @param userAuthorization
	 * @param objectId
	 * @param objectType
	 * @return The order hint associated with the given object id and object type.
	 * @throws NotFoundException
	 */
	V2WikiOrderHint getOrderHint(UserAuthorization userAuthorization, String objectId, ObjectType objectType) throws NotFoundException;
	
	/**
	 * Updates the given order hint.
	 * @param userAuthorization
	 * @param orderHint
	 * @return The updated order hint.
	 * @throws NotFoundException
	 */
	V2WikiOrderHint updateOrderHint(UserAuthorization userAuthorization, V2WikiOrderHint orderHint) throws NotFoundException;

	/**
	 * Get the root wiki page key.
	 * @param userAuthorization
	 * @param ownerId
	 * @param type
	 * @return
	 * @throws NotFoundException 
	 */
	WikiPageKey getRootWikiKey(UserAuthorization userAuthorization, String ownerId, ObjectType type) throws NotFoundException;
	
}
