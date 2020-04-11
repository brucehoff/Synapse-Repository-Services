package org.sagebionetworks.repo.web.service.subscription;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;

public interface SubscriptionService {

	/**
	 * Subscribe to a topic
	 * 
	 * @param topic
	 * @return
	 */
	public Subscription create(UserInfo userInfo, Topic topic);

	/**
	 * retrieve all subscriptions one has
	 * 
	 * @param limit
	 * @param offset
	 * @param objectType
	 * @param sortDirection 
	 * @param sortByType 
	 * @param sortDirection 
	 * @param sortByType 
	 * @return
	 */
	public SubscriptionPagedResults getAll(UserInfo userInfo, Long limit, Long offset, SubscriptionObjectType objectType, SortByType sortByType, SortDirection sortDirection);

	/**
	 * retrieve subscriptions one has based on a list of provided topics
	 * 
	 * @param request
	 * @return
	 */
	public SubscriptionPagedResults getList(UserInfo userInfo, SubscriptionRequest request);

	/**
	 * unsubscribe to a topic
	 * 
	 * @param subscriptionId
	 */
	public void delete(UserInfo userInfo, String subscriptionId);

	/**
	 * unsubscribe to all topic
	 * 
	 */
	public void deleteAll(UserInfo userInfo);

	/**
	 * retrieve a subscription given its ID
	 * 
	 * @param id
	 * @return
	 */
	public Subscription get(UserInfo userInfo, String id);

	/**
	 * retrieve a page of subscribers for a given topic
	 * 
	 * @param topic
	 * @param nextPageToken
	 * @return
	 */
	public SubscriberPagedResults getSubscribers(UserInfo userInfo, Topic topic, String nextPageToken);

	/**
	 * retrieve number of subscribers for a given topic
	 * 
	 * @param topic
	 * @return
	 */
	public SubscriberCount getSubscriberCount(UserInfo userInfo, Topic topic);

	/**
	 * Subscribe to all topic of the same SubscriptionObjectType
	 * 
	 * @param objectType
	 * @return
	 */
	public Subscription subscribeAll(UserInfo userInfo, SubscriptionObjectType objectType);

}
