package org.sagebionetworks.web.shared;

import java.util.Date;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class Project implements IsSerializable {
	private String name;	
	private String annotations;
	private String id;
	private String description;
	private String status;
	private Date creationDate;
	private String parentId;
	private String etag;
	private String uri;
	private String accessControlList;
	private String creator;
	
	public Project() {
	}
	
	/**
	 * Default constructor is required
	 * @param obj JSONObject of the Project object
	 */		
	public Project(JSONObject object) {
		String key = null; 

		key = "name";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setName(object.get(key).isString().stringValue());		
				
		key = "annotations";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setAnnotations(object.get(key).isString().stringValue());		
		
		key = "id";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setId(object.get(key).isString().stringValue());		
				
		key = "description";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setDescription(object.get(key).isString().stringValue());		
		
		key = "status";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setStatus(object.get(key).isString().stringValue());
		
		key = "creationDate";
		if(object.containsKey(key)) 
			if(object.get(key).isNumber() != null)
				setCreationDate(new Date(new Double(object.get(key).isNumber().doubleValue()).longValue()));

		key = "parentId";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setParentId(object.get(key).isString().stringValue());
		
		key = "etag";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setEtag(object.get(key).isString().stringValue());		
		
		key = "uri";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setUri(object.get(key).isString().stringValue());

		key = "accessControlList";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setAccessControlList(object.get(key).isString().stringValue());					
		
		key = "creator";
		if(object.containsKey(key)) 
			if(object.get(key).isString() != null)
				setCreator(object.get(key).isString().stringValue());							
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUri() {
		return uri;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getAccessControlList() {
		return accessControlList;
	}

	public void setAccessControlList(String accessControlList) {
		this.accessControlList = accessControlList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessControlList == null) ? 0 : accessControlList
						.hashCode());
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		Project other = (Project) obj;
		if (accessControlList == null) {
			if (other.accessControlList != null)
				return false;
		} else if (!accessControlList.equals(other.accessControlList))
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.equals(other.creator))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Project [id=" + id + ", uri=" + uri + ", etag=" + etag
				+ ", name=" + name + ", description=" + description
				+ ", creator=" + creator + ", creationDate=" + creationDate
				+ ", status=" + status + ", annotations=" + annotations
				+ ", parentId=" + parentId + ", accessControlList="
				+ accessControlList + "]";
	}

}
