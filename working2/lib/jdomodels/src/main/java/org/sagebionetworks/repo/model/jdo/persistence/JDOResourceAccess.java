package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_RESOURCE_ACCESS)
public class JDOResourceAccess {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	@Column (name=SqlConstants.COL_RESOURCE_ACCESS_OWNER)
	@ForeignKey(name="RESOURCE_ACCESS_OWNER_FK", deleteAction=ForeignKeyAction.CASCADE)
	private JDOAccessControlList owner;
	
	@Persistent
	@Column(name=SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID)
//	@ForeignKey(name="RESOURCE_ACCESS_USER_GROUP_FK", deleteAction=ForeignKeyAction.NONE)
	private long userGroupId;
				
	// e.g. read, write, delete, as defined in AuthorizationConstants.ACCESS_TYPE
	@Persistent(serialized="false")
	@Join(table=SqlConstants.TABLE_RESOURCE_ACCESS_TYPE, column=SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID, deleteAction=ForeignKeyAction.CASCADE)
	@ForeignKey(name="ACCESS_TYPE_RESOURCE_ACCESS_FK", deleteAction=ForeignKeyAction.CASCADE)
	private Set<String> accessType = new HashSet<String>();

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}


	/**
	 * @return the userGroupId
	 */
	public long getUserGroupId() {
		return userGroupId;
	}

	/**
	 * @param userGroupId the userGroupId to set
	 */
	public void setUserGroupId(long userGroupId) {
		this.userGroupId = userGroupId;
	}

	/**
	 * @return the accessType
	 */
	public Set<String> getAccessType() {
		return accessType;
	}

	/**
	 * @param accessType the accessType to set
	 */
	public void setAccessType(Set<String> accessType) {
		this.accessType = accessType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessType == null) ? 0 : accessType.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + (int) (userGroupId ^ (userGroupId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JDOResourceAccess other = (JDOResourceAccess) obj;
		if (accessType == null) {
			if (other.accessType != null)
				return false;
		} else if (!accessType.equals(other.accessType))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (userGroupId != other.userGroupId)
			return false;
		return true;
	}
	

}
