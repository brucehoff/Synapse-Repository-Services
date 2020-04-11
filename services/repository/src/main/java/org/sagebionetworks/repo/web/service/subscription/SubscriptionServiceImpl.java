package org.sagebionetworks.repo.web.service.subscription;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
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
import org.springframework.beans.factory.annotation.Autowired;

public class SubscriptionServiceImpl implements SubscriptionService {
	@Autowired
	private SubscriptionManager subscriptionManager;

	@Override
	public Subscription create(UserInfo userInfo, Topic topic) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.create(userInfo, topic);
	}

	@Override
	public SubscriptionPagedResults getAll(UserInfo userInfo, Long limit, Long offset, SubscriptionObjectType objectType, SortByType sortByType, SortDirection sortDirection) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getAll(userInfo, limit, offset, objectType, sortByType, sortDirection);
	}

	@Override
	public SubscriptionPagedResults getList(UserInfo userInfo, SubscriptionRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getList(userInfo, request);
	}

	@Override
	public void delete(UserInfo userInfo, String subscriptionId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		subscriptionManager.delete(userInfo, subscriptionId);
	}

	@Override
	public void deleteAll(UserInfo userInfo) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		subscriptionManager.deleteAll(userInfo);
	}

	@Override
	public Subscription get(UserInfo userInfo, String id) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.get(userInfo, id);
	}

	@Override
	public SubscriberPagedResults getSubscribers(UserInfo userInfo, Topic topic, String nextPageToken) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getSubscribers(userInfo, topic, nextPageToken);
	}

	@Override
	public SubscriberCount getSubscriberCount(UserInfo userInfo, Topic topic) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.getSubscriberCount(userInfo, topic);
	}

	@Override
	public Subscription subscribeAll(UserInfo userInfo, SubscriptionObjectType objectType) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return subscriptionManager.subscribeAll(userInfo, objectType);
	}

}
