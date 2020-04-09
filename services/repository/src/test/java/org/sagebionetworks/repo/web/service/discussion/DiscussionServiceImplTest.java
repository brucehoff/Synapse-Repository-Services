package org.sagebionetworks.repo.web.service.discussion;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.discussion.DiscussionReplyManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionThreadManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;

@ExtendWith(MockitoExtension.class)
public class DiscussionServiceImplTest {

	@Mock
	private ForumManager mockForumManager;
	@Mock
	private DiscussionThreadManager mockThreadManager;
	@Mock
	private DiscussionReplyManager mockReplyManager;
	@InjectMocks
	private DiscussionServiceImpl discussionServices;
	
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private String projectId = "syn456";
	private CreateDiscussionThread createThread;
	private String forumId = "789";
	private String messageMarkdown = "messageMarkdown";
	private String title = "title";
	private DiscussionThreadBundle threadBundle;
	private String threadId = "321";
	private String messageKey = "messageKey";
	private DiscussionReplyBundle replyBundle;
	private String replyId = "987";
	private CreateDiscussionReply createReply;
	private MessageURL messageUrl = new MessageURL();

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		createThread = new CreateDiscussionThread();
		createThread.setForumId(forumId);
		createThread.setMessageMarkdown(messageMarkdown);
		createThread.setTitle(title);

		threadBundle = new DiscussionThreadBundle();
		threadBundle.setId(threadId);
		threadBundle.setForumId(forumId);
		threadBundle.setTitle(title);
		threadBundle.setProjectId(projectId);
		threadBundle.setMessageKey(messageKey);

		createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown(messageMarkdown);

		replyBundle = new DiscussionReplyBundle();
		replyBundle.setId(replyId);
		replyBundle.setThreadId(threadId);
		replyBundle.setMessageKey(messageKey);

		messageUrl.setMessageUrl("messageUrl");
	}

	@Test
	public void testGetForumByProjectId() {
		discussionServices.getForumByProjectId(userInfo, projectId);
		Mockito.verify(mockForumManager).getForumByProjectId(userInfo, projectId);
	}

	@Test
	public void testCreateThread() throws Exception {
		Mockito.when(mockThreadManager.createThread(userInfo, createThread)).thenReturn(threadBundle);
		assertEquals(threadBundle, discussionServices.createThread(userInfo, createThread));
	}

	@Test
	public void testGetThread() {
		Mockito.when(mockThreadManager.getThread(userInfo, threadId)).thenReturn(threadBundle);
		assertEquals(threadBundle, discussionServices.getThread(userInfo, threadId));
	}

	@Test
	public void testUpdateThreadTitle() {
		UpdateThreadTitle newTitle = new UpdateThreadTitle();
		newTitle.setTitle("newTitle");
		threadBundle.setTitle("newTitle");
		Mockito.when(mockThreadManager.updateTitle(userInfo, threadId, newTitle)).thenReturn(threadBundle);
		assertEquals(threadBundle, discussionServices.updateThreadTitle(userInfo, threadId, newTitle));
	}

	@Test
	public void testUpdateThreadMessage() throws Exception {
		UpdateThreadMessage newMessage = new UpdateThreadMessage();
		newMessage.setMessageMarkdown("newMessage");
		threadBundle.setMessageKey("newkey");
		Mockito.when(mockThreadManager.updateMessage(userInfo, threadId, newMessage)).thenReturn(threadBundle);
		assertEquals(threadBundle, discussionServices.updateThreadMessage(userInfo, threadId, newMessage));
	}

	@Test
	public void testMarkThreadAsDeleted() {
		discussionServices.markThreadAsDeleted(userInfo, threadId);
		Mockito.verify(mockThreadManager).markThreadAsDeleted(userInfo, threadId);
	}

	@Test
	public void testGetThreads() {
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		threads.setResults(Arrays.asList(threadBundle));
		Mockito.when(mockThreadManager.getThreadsForForum(userInfo, forumId, 10L, 0L, null, true, DiscussionFilter.NO_FILTER)).thenReturn(threads);
		assertEquals(threads, discussionServices.getThreadsForForum(userInfo, forumId, 10L, 0L, null, true, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testCreateReply() throws Exception {
		Mockito.when(mockReplyManager.createReply(userInfo, createReply)).thenReturn(replyBundle);
		assertEquals(replyBundle, discussionServices.createReply(userInfo, createReply));
	}

	@Test
	public void testGetReply() {
		Mockito.when(mockReplyManager.getReply(userInfo, replyId)).thenReturn(replyBundle);
		assertEquals(replyBundle, discussionServices.getReply(userInfo, replyId));
	}

	@Test
	public void testUpdateReplyMessage() throws Exception {
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("newMessage");
		replyBundle.setMessageKey("newkey");
		Mockito.when(mockReplyManager.updateReplyMessage(userInfo, replyId, newMessage)).thenReturn(replyBundle);
		assertEquals(replyBundle, discussionServices.updateReplyMessage(userInfo, replyId, newMessage));
	}

	@Test
	public void testMarkReplyAsDeleted() {
		discussionServices.markReplyAsDeleted(userInfo, replyId);
		Mockito.verify(mockReplyManager).markReplyAsDeleted(userInfo, replyId);
	}

	@Test
	public void testGetReplies() {
		PaginatedResults<DiscussionReplyBundle> replies = new PaginatedResults<DiscussionReplyBundle>();
		replies.setResults(Arrays.asList(replyBundle));
		Mockito.when(mockReplyManager.getRepliesForThread(userInfo, threadId, 10L, 0L, null, true, DiscussionFilter.NO_FILTER)).thenReturn(replies);
		assertEquals(replies, discussionServices.getReplies(userInfo, threadId, 10L, 0L, null, true, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testGetThreadUrl() {
		Mockito.when(mockThreadManager.getMessageUrl(userInfo, messageKey)).thenReturn(messageUrl);
		assertEquals(messageUrl, discussionServices.getThreadUrl(userInfo, messageKey));
	}

	@Test
	public void testGetReplyUrl() {
		Mockito.when(mockReplyManager.getMessageUrl(userInfo, messageKey)).thenReturn(messageUrl);
		assertEquals(messageUrl, discussionServices.getReplyUrl(userInfo, messageKey));
	}
}
