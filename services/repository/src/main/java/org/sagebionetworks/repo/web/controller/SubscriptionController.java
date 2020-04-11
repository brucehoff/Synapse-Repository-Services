package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ServiceConstants;
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
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>While working in Synapse, users may want to subscribe to different topics
 * to receive notifications about changes in those topics.</p>
 * <br>
 * <p>These services provide the APIs for Synapse users to manage their subscriptions.</p>
 * <br>
 */
@ControllerInfo(displayName = "Subscription Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SubscriptionController{

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * This API is used to subscribe to a topic.
	 * <br/>
	 * Target users: anyone who has READ permission on the object.
	 * 
	 * @param topic - Topic to subscribe to
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION, method = RequestMethod.POST)
	public @ResponseBody Subscription create(
			UserInfo userInfo,
			@RequestBody Topic topic) {
		return serviceProvider.getSubscriptionService().create(userId, topic);
	}

	/**
	 * This API is used to subscribe to all topic of the same SubscriptionObjectType.
	 * <br/>
	 * Only the following SubscriptionObjectType are allowed in this API:
	 * <ul><li>DATA_ACCESS_SUBMISSION</li></ul>
	 * 
	 * @param objectType - SubscriptionObjectType to subscribe to
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ALL, method = RequestMethod.POST)
	public @ResponseBody Subscription subscribeAll(
			UserInfo userInfo,
			@RequestParam(value = ServiceConstants.SUBSCRIPTION_OBJECT_TYPE_PARAM) SubscriptionObjectType objectType){
		return serviceProvider.getSubscriptionService().subscribeAll(userId, objectType);
	}

	/**
	 * This API is used to retrieve subscriptions one has based on a list of provided topics.
	 * These topics must have the same objectType.
	 * <br/>
	 * Target users: all Synapse users.
	 * 
	 * @param request - This object defines what topics the user is asking for
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_LIST, method = RequestMethod.POST)
	public @ResponseBody SubscriptionPagedResults getList(
			UserInfo userInfo,
			@RequestBody SubscriptionRequest request) {
		return serviceProvider.getSubscriptionService().getList(userId, request);
	}

	/**
	 * This API is used to retrieve a subscription given its ID
	 * <br/>
	 * Target users: Synapse user who created this subscription.
	 * 
	 * @param id - the ID of the subscription that is created when the user subscribed to the topic
	  * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ID, method = RequestMethod.GET)
	public @ResponseBody Subscription get(
			UserInfo userInfo,
			@PathVariable String id) {
		return serviceProvider.getSubscriptionService().get(userId, id);
	}

	/**
	 * This API is used to retrieve all subscriptions one has.
	 * <br/>
	 * Target users: all Synapse users.
	 * 
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum Limit for this call is 100.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @param objectType - User can use this param to filter the results by the type of object they subscribed to.
	 * @param sortByType - When provided, the results will be sorted by this type.
	 * @param sortDirection- When provided, the results will be sorted in this direction.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ALL, method = RequestMethod.GET)
	public @ResponseBody SubscriptionPagedResults getAll(
			UserInfo userInfo,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(value = ServiceConstants.SUBSCRIPTION_OBJECT_TYPE_PARAM) SubscriptionObjectType objectType,
			@RequestParam(value = ServiceConstants.SUBSCRIPTION_SORT_TYPE_PARAM, required=false) SortByType sortByType,
			@RequestParam(value = ServiceConstants.SUBSCRIPTION_SORT_DIRECTION_PARAM, required=false) SortDirection sortDirection) {
		return serviceProvider.getSubscriptionService().getAll(userId, limit, offset, objectType, sortByType, sortDirection);
	}

	/**
	 * This API is used to unsubscribe to a topic.
	 * <br/>
	 * Target users: Synapse user who created this subscription.
	 * 
	 * @param id - the ID of the subscription that is created when the user subscribed to the topic
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ID, method = RequestMethod.DELETE)
	public void delete(
			UserInfo userInfo,
			@PathVariable String id) {
		serviceProvider.getSubscriptionService().delete(userId, id);
	}

	/**
	 * This API is used to unsubscribe all topics one followed.
	 * <br/>
	 * Target users: Synapse users
	 * 
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_ALL, method = RequestMethod.DELETE)
	public void deleteAll(
			UserInfo userInfo) {
		serviceProvider.getSubscriptionService().deleteAll(userId);
	}

	/**
	 * Retrieve subscribers for a given topic.
	 * 
	 * @param topic
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_SUBSCRIBERS, method = RequestMethod.POST)
	public @ResponseBody SubscriberPagedResults getSubscribers(
			UserInfo userInfo,
			@RequestBody Topic topic,
			@RequestParam(value = UrlHelpers.NEXT_PAGE_TOKEN_PARAM, required = false) String nextPageToken) {
		return serviceProvider.getSubscriptionService().getSubscribers(userId, topic, nextPageToken);
	}

	/**
	 * Retrieve number of subscribers for a given topic.
	 * 
	 * @param topic
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBSCRIPTION_SUBSCRIBER_COUNT, method = RequestMethod.POST)
	public @ResponseBody SubscriberCount getSubscriberCount(
			UserInfo userInfo,
			@RequestBody Topic topic) {
		return serviceProvider.getSubscriptionService().getSubscriberCount(userId, topic);
	}
}
