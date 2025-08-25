package com.lovablecline.repository;

import com.lovablecline.entity.ChatMessage;
import com.lovablecline.entity.Project;
import com.lovablecline.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ChatMessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private User testUser;
    private Project testProject;
    private ChatMessage testMessage1;
    private ChatMessage testMessage2;
    private ChatMessage testMessage3;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        
        testUser = entityManager.persist(testUser);

        // Create test project
        testProject = new Project();
        testProject.setName("Test Project");
        testProject.setAddress("123 Test St");
        testProject.setStatus("active");
        testProject.setOwner(testUser);
        testProject = entityManager.persist(testProject);

        // Create test chat messages
        testMessage1 = new ChatMessage();
        testMessage1.setMessage("Hello, this is the first message");
        testMessage1.setIsFromClient(true);
        testMessage1.setProjectId(testProject.getId());
        testMessage1.setUserId(testUser.getId());
        testMessage1.setCreatedAt(LocalDateTime.now().minusHours(3));
        testMessage1 = entityManager.persist(testMessage1);

        testMessage2 = new ChatMessage();
        testMessage2.setMessage("This is the second message from the contractor");
        testMessage2.setIsFromClient(false);
        testMessage2.setProjectId(testProject.getId());
        testMessage2.setUserId(testUser.getId());
        testMessage2.setCreatedAt(LocalDateTime.now().minusHours(2));
        testMessage2 = entityManager.persist(testMessage2);

        testMessage3 = new ChatMessage();
        testMessage3.setMessage("Another client message here");
        testMessage3.setIsFromClient(true);
        testMessage3.setProjectId(testProject.getId());
        testMessage3.setUserId(testUser.getId());
        testMessage3.setCreatedAt(LocalDateTime.now().minusHours(1));
        testMessage3 = entityManager.persist(testMessage3);

        entityManager.flush();
    }

    @Test
    void findByProject_shouldReturnMessagesForProject() {
        // When
        List<ChatMessage> messages = chatMessageRepository.findByProject(testProject);

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages).extracting(ChatMessage::getMessage)
                .containsExactlyInAnyOrder(
                        "Hello, this is the first message",
                        "This is the second message from the contractor",
                        "Another client message here"
                );
    }

    @Test
    void findByProjectId_shouldReturnMessagesForProjectId() {
        // When
        List<ChatMessage> messages = chatMessageRepository.findByProjectId(testProject.getId());

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages).extracting(ChatMessage::getMessage)
                .containsExactlyInAnyOrder(
                        "Hello, this is the first message",
                        "This is the second message from the contractor",
                        "Another client message here"
                );
    }

    @Test
    void findByProjectIdOrderByCreatedAtDesc_shouldReturnMessagesOrderedByCreatedAtDesc() {
        // When
        List<ChatMessage> messages = chatMessageRepository.findByProjectIdOrderByCreatedAtDesc(testProject.getId());

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getMessage()).isEqualTo("Another client message here"); // Newest first
        assertThat(messages.get(1).getMessage()).isEqualTo("This is the second message from the contractor");
        assertThat(messages.get(2).getMessage()).isEqualTo("Hello, this is the first message"); // Oldest last
        assertThat(messages.get(0).getCreatedAt()).isAfter(messages.get(1).getCreatedAt());
        assertThat(messages.get(1).getCreatedAt()).isAfter(messages.get(2).getCreatedAt());
    }

    @Test
    void findByUserIdAndProjectId_shouldReturnMessagesForUserAndProject() {
        // When
        List<ChatMessage> messages = chatMessageRepository.findByUserIdAndProjectId(testUser.getId(), testProject.getId());

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages).extracting(ChatMessage::getMessage)
                .containsExactlyInAnyOrder(
                        "Hello, this is the first message",
                        "This is the second message from the contractor",
                        "Another client message here"
                );
    }

    @Test
    void findAllByUserId_shouldReturnAllMessagesForUser() {
        // Create another project for the same user
        Project anotherProject = new Project();
        anotherProject.setName("Another Project");
        anotherProject.setAddress("456 Another St");
        anotherProject.setStatus("active");
        anotherProject.setOwner(testUser);
        anotherProject = entityManager.persist(anotherProject);

        // Create message for another project
        ChatMessage anotherMessage = new ChatMessage();
        anotherMessage.setMessage("Message from another project");
        anotherMessage.setIsFromClient(true);
        anotherMessage.setProjectId(anotherProject.getId());
        anotherMessage.setUserId(testUser.getId());
        anotherMessage = entityManager.persist(anotherMessage);

        entityManager.flush();

        // When
        List<ChatMessage> messages = chatMessageRepository.findAllByUserId(testUser.getId());

        // Then
        assertThat(messages).hasSize(4);
        assertThat(messages).extracting(ChatMessage::getMessage)
                .containsExactlyInAnyOrder(
                        "Hello, this is the first message",
                        "This is the second message from the contractor",
                        "Another client message here",
                        "Message from another project"
                );
    }

    @Test
    void findByIdAndProjectId_shouldReturnMessageWhenExists() {
        // When
        Optional<ChatMessage> foundMessage = chatMessageRepository.findByIdAndProjectId(
                testMessage1.getId(), testProject.getId());

        // Then
        assertThat(foundMessage).isPresent();
        assertThat(foundMessage.get().getMessage()).isEqualTo("Hello, this is the first message");
    }

    @Test
    void findByIdAndProjectId_shouldReturnEmptyWhenIdDoesNotMatchProject() {
        // Create another project
        Project anotherProject = new Project();
        anotherProject.setName("Another Project");
        anotherProject.setAddress("456 Another St");
        anotherProject.setStatus("active");
        anotherProject.setOwner(testUser);
        anotherProject = entityManager.persist(anotherProject);

        entityManager.flush();

        // When
        Optional<ChatMessage> foundMessage = chatMessageRepository.findByIdAndProjectId(
                testMessage1.getId(), anotherProject.getId());

        // Then
        assertThat(foundMessage).isEmpty();
    }

    @Test
    void findByProjectIdAndIsFromClientOrderByCreatedAtDesc_shouldReturnClientMessagesOrdered() {
        // When
        List<ChatMessage> clientMessages = chatMessageRepository.findByProjectIdAndIsFromClientOrderByCreatedAtDesc(
                testProject.getId(), true);

        // Then
        assertThat(clientMessages).hasSize(2);
        assertThat(clientMessages).extracting(ChatMessage::getMessage)
                .containsExactlyInAnyOrder(
                        "Hello, this is the first message",
                        "Another client message here"
                );
        assertThat(clientMessages.get(0).getCreatedAt()).isAfter(clientMessages.get(1).getCreatedAt());
    }

    @Test
    void findByProjectIdAndIsFromClientOrderByCreatedAtDesc_shouldReturnContractorMessagesOrdered() {
        // When
        List<ChatMessage> contractorMessages = chatMessageRepository.findByProjectIdAndIsFromClientOrderByCreatedAtDesc(
                testProject.getId(), false);

        // Then
        assertThat(contractorMessages).hasSize(1);
        assertThat(contractorMessages.get(0).getMessage()).isEqualTo("This is the second message from the contractor");
    }

    @Test
    void countByProjectIdAndIsFromClient_shouldReturnClientMessageCount() {
        // When
        Long clientCount = chatMessageRepository.countByProjectIdAndIsFromClient(testProject.getId(), true);

        // Then
        assertThat(clientCount).isEqualTo(2L);
    }

    @Test
    void countByProjectIdAndIsFromClient_shouldReturnContractorMessageCount() {
        // When
        Long contractorCount = chatMessageRepository.countByProjectIdAndIsFromClient(testProject.getId(), false);

        // Then
        assertThat(contractorCount).isEqualTo(1L);
    }

    @Test
    void save_shouldPersistNewChatMessage() {
        // Given
        ChatMessage newMessage = new ChatMessage();
        newMessage.setMessage("New chat message");
        newMessage.setIsFromClient(true);
        newMessage.setProjectId(testProject.getId());
        newMessage.setUserId(testUser.getId());

        // When
        ChatMessage savedMessage = chatMessageRepository.save(newMessage);
        entityManager.flush();

        // Then
        assertThat(savedMessage.getId()).isNotNull();
        assertThat(savedMessage.getMessage()).isEqualTo("New chat message");
        assertThat(chatMessageRepository.count()).isEqualTo(4);
    }

    @Test
    void delete_shouldRemoveChatMessage() {
        // When
        chatMessageRepository.delete(testMessage1);
        entityManager.flush();

        // Then
        assertThat(chatMessageRepository.count()).isEqualTo(2);
        assertThat(chatMessageRepository.findById(testMessage1.getId())).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllChatMessages() {
        // When
        List<ChatMessage> allMessages = chatMessageRepository.findAll();

        // Then
        assertThat(allMessages).hasSize(3);
        assertThat(allMessages).extracting(ChatMessage::getMessage)
                .containsExactlyInAnyOrder(
                        "Hello, this is the first message",
                        "This is the second message from the contractor",
                        "Another client message here"
                );
    }

    @Test
    void findById_shouldReturnMessageWhenExists() {
        // When
        Optional<ChatMessage> foundMessage = chatMessageRepository.findById(testMessage1.getId());

        // Then
        assertThat(foundMessage).isPresent();
        assertThat(foundMessage.get().getMessage()).isEqualTo("Hello, this is the first message");
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        // When
        Optional<ChatMessage> foundMessage = chatMessageRepository.findById("non-existent-id");

        // Then
        assertThat(foundMessage).isEmpty();
    }
}
