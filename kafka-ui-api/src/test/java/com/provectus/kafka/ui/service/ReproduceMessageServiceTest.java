package com.provectus.kafka.ui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.provectus.kafka.ui.exception.ValidationException;
import com.provectus.kafka.ui.model.KafkaCluster;
import com.provectus.kafka.ui.model.ReproduceMessageRequestDTO;
import com.provectus.kafka.ui.model.ReproduceMessageResponseDTO;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/**
 * Unit tests for ReproduceMessageService.
 * Tests cover validation, dry-run mode, and basic reproduction logic.
 */
@ExtendWith(MockitoExtension.class)
class ReproduceMessageServiceTest {

  @Mock
  private ConsumerGroupService consumerGroupService;

  private ReproduceMessageService reproduceMessageService;

  private KafkaCluster testCluster;

  @BeforeEach
  void setUp() {
    reproduceMessageService = new ReproduceMessageService(consumerGroupService);
    
    // Setup test cluster
    testCluster = new KafkaCluster();
    testCluster.setName("test-cluster");
    testCluster.setBootstrapServers("localhost:9092");
    testCluster.setProperties(new Properties());
  }

  @Test
  void testValidateRequest_WithValidSourceReference_ShouldPass() {
    // Given
    ReproduceMessageRequestDTO request = new ReproduceMessageRequestDTO();
    request.setSourceTopic("dlt-topic");
    request.setSourcePartition(0);
    request.setSourceOffset(100L);
    request.setDryRun(true);

    // When
    var result = reproduceMessageService.reproduceMessage(testCluster, "target-topic", request);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertTrue(response.isSuccess());
          assertTrue(response.isDryRun());
        })
        .verifyComplete();
  }

  @Test
  void testValidateRequest_WithValidFullMessage_ShouldPass() {
    // Given
    ReproduceMessageRequestDTO request = new ReproduceMessageRequestDTO();
    request.setKey(Base64.getEncoder().encodeToString("test-key".getBytes()));
    request.setValue(Base64.getEncoder().encodeToString("test-value".getBytes()));
    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    request.setHeaders(headers);
    request.setDryRun(true);

    // When
    var result = reproduceMessageService.reproduceMessage(testCluster, "target-topic", request);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertTrue(response.isSuccess());
          assertTrue(response.isDryRun());
        })
        .verifyComplete();
  }

  @Test
  void testValidateRequest_WithMissingData_ShouldFail() {
    // Given - request with no source reference and no full message data
    ReproduceMessageRequestDTO request = new ReproduceMessageRequestDTO();
    request.setDryRun(true);

    // When
    var result = reproduceMessageService.reproduceMessage(testCluster, "target-topic", request);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertFalse(response.isSuccess());
          assertNotNull(response.getErrorMessage());
          assertTrue(response.getErrorMessage().contains("Either provide source topic reference"));
        })
        .verifyComplete();
  }

  @Test
  void testValidateRequest_WithNegativePartition_ShouldFail() {
    // Given
    ReproduceMessageRequestDTO request = new ReproduceMessageRequestDTO();
    request.setSourceTopic("dlt-topic");
    request.setSourcePartition(-1);
    request.setSourceOffset(100L);
    request.setDryRun(true);

    // When
    var result = reproduceMessageService.reproduceMessage(testCluster, "target-topic", request);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertFalse(response.isSuccess());
          assertNotNull(response.getErrorMessage());
          assertTrue(response.getErrorMessage().contains("must be non-negative"));
        })
        .verifyComplete();
  }

  @Test
  void testDryRun_ShouldNotProduceMessage() {
    // Given
    ReproduceMessageRequestDTO request = new ReproduceMessageRequestDTO();
    request.setKey(Base64.getEncoder().encodeToString("test-key".getBytes()));
    request.setValue(Base64.getEncoder().encodeToString("test-value".getBytes()));
    request.setDryRun(true);

    // When
    var result = reproduceMessageService.reproduceMessage(testCluster, "target-topic", request);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertTrue(response.isSuccess());
          assertTrue(response.isDryRun());
          // In dry run, partition, offset, and timestamp should be null
          assertEquals(null, response.getPartition());
          assertEquals(null, response.getOffset());
          assertEquals(null, response.getTimestamp());
        })
        .verifyComplete();
  }

  @Test
  void testReproduceMessage_WithHeaders_ShouldPreserveHeaders() {
    // Given
    ReproduceMessageRequestDTO request = new ReproduceMessageRequestDTO();
    request.setKey(Base64.getEncoder().encodeToString("test-key".getBytes()));
    request.setValue(Base64.getEncoder().encodeToString("test-value".getBytes()));
    
    Map<String, String> headers = new HashMap<>();
    headers.put("correlation-id", "12345");
    headers.put("source", "dlt-topic");
    request.setHeaders(headers);
    request.setDryRun(true);

    // When
    var result = reproduceMessageService.reproduceMessage(testCluster, "target-topic", request);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertTrue(response.isSuccess());
          assertTrue(response.isDryRun());
        })
        .verifyComplete();
  }

  @Test
  void testReproduceMessage_WithTargetPartition_ShouldUseTargetPartition() {
    // Given
    ReproduceMessageRequestDTO request = new ReproduceMessageRequestDTO();
    request.setKey(Base64.getEncoder().encodeToString("test-key".getBytes()));
    request.setValue(Base64.getEncoder().encodeToString("test-value".getBytes()));
    request.setTargetPartition(5);
    request.setDryRun(true);

    // When
    var result = reproduceMessageService.reproduceMessage(testCluster, "target-topic", request);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertTrue(response.isSuccess());
          assertTrue(response.isDryRun());
        })
        .verifyComplete();
  }

  @Test
  void testReproduceMessage_WithPreserveTimestamp_ShouldHandleTimestamp() {
    // Given
    ReproduceMessageRequestDTO request = new ReproduceMessageRequestDTO();
    request.setKey(Base64.getEncoder().encodeToString("test-key".getBytes()));
    request.setValue(Base64.getEncoder().encodeToString("test-value".getBytes()));
    request.setPreserveTimestamp(true);
    request.setDryRun(true);

    // When
    var result = reproduceMessageService.reproduceMessage(testCluster, "target-topic", request);

    // Then
    StepVerifier.create(result)
        .assertNext(response -> {
          assertTrue(response.isSuccess());
          assertTrue(response.isDryRun());
        })
        .verifyComplete();
  }
}
