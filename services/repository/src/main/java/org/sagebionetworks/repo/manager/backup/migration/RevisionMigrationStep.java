package org.sagebionetworks.repo.manager.backup.migration;

import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.ObjectType;

/**
 * Represents a single step in the migration of a NodeRevision.
 * @author John
 *
 */
public interface RevisionMigrationStep {

	/**
	 * Migrate a single step, from one version to the next.
	 * @param toMigrate
	 * @param type
	 * @return
	 */
	public NodeRevision migrateOneStep(NodeRevision toMigrate,	ObjectType type);
}
