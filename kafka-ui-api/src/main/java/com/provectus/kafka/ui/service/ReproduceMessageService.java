package com.provectus.kafka.ui.service;

import com.provectus.kafka.ui.exception.ValidationException;
import com.provectus.kafka.ui.model.KafkaCluster;
import com.provectus.kafka.ui.model.ReproduceMessageRequestDTO;
import com.provectus.kafka.ui.model.ReproduceMessageResponseDTO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service for reproducing messages from a DLT (dead-letter topic) to a target topic.
 * Supports single-message reproduction with preservation of key, value, headers, and optionally timestamp.
 * 
 * <p><b>IMPORTANT FOR OPERATORS:</b>
 * The backend principal must have READ permission on the source DLT topic and WRITE permission 
 * on the target topic for this feature to work correctly.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReproduceMessageService {

  private final ConsumerGroupService consumerGroupService;

  /**
   * Reproduces a single message from a DLT to a target topic.
   * 
   * @param cluster The Kafka cluster
   * @param targetTopic The target topic name
   * @param request The reproduction request containing either message reference or full message data
   * @return Response containing the result of the reproduction operation
   */
  public Mono<ReproduceMessageResponseDTO> reproduceMessage(
      KafkaCluster cluster,
      String targetTopic,
      ReproduceMessageRequestDTO request) {
    
    return Mono.fromCallable(() -> {
      log.info("Reproducing message to target topic: {}, dryRun: {}", targetTopic, request.isDryRun());
      
      // Validate request
      validateRequest(request);
      
      byte[] keyBytes;
      byte[] valueBytes;
      Map<String, String> headers;
      Long timestamp = null;
      
      // Determine if we need to fetch the message or if it's provided
      if (request.getSourceTopic() != null) {
        // Fetch message from source topic
        log.info("Fetching message from source topic: {}, partition: {}, offset: {}",
            request.getSourceTopic(), request.getSourcePartition(), request.getSourceOffset());
        
        ConsumerRecord<byte[], byte[]> sourceRecord = fetchMessage(
            cluster,
            request.getSourceTopic(),
            request.getSourcePartition(),
            request.getSourceOffset()
        );
        
        keyBytes = sourceRecord.key();
        valueBytes = sourceRecord.value();
        headers = extractHeaders(sourceRecord);
        
        if (request.isPreserveTimestamp()) {
          timestamp = sourceRecord.timestamp();
        }
      } else {
        // Use provided message data
        log.info("Using provided message data");
        keyBytes = request.getKey() != null ? Base64.getDecoder().decode(request.getKey()) : null;
        valueBytes = request.getValue() != null ? Base64.getDecoder().decode(request.getValue()) : null;
        headers = request.getHeaders() != null ? request.getHeaders() : new HashMap<>();
      }
      
      // If dry run, just validate and return
      if (request.isDryRun()) {
        log.info("Dry run completed successfully");
        return ReproduceMessageResponseDTO.builder()
            .success(true)
            .dryRun(true)
            .build();
      }
      
      // Produce message to target topic
      RecordMetadata metadata = produceMessage(
          cluster,
          targetTopic,
          request.getTargetPartition(),
          keyBytes,
          valueBytes,
          headers,
          timestamp
      );
      
      log.info("Message reproduced successfully to partition: {}, offset: {}", 
          metadata.partition(), metadata.offset());
      
      return ReproduceMessageResponseDTO.builder()
          .success(true)
          .partition(metadata.partition())
          .offset(metadata.offset())
          .timestamp(metadata.timestamp())
          .dryRun(false)
          .build();
    })
    .subscribeOn(Schedulers.boundedElastic())
    .onErrorResume(e -> {
      log.error("Error reproducing message", e);
      return Mono.just(ReproduceMessageResponseDTO.builder()
          .success(false)
          .errorMessage(e.getMessage())
          .dryRun(request.isDryRun())
          .build());
    });
  }

  /**
   * Validates the reproduction request.
   */
  private void validateRequest(ReproduceMessageRequestDTO request) {
    boolean hasSourceReference = request.getSourceTopic() != null 
        && request.getSourcePartition() != null 
        && request.getSourceOffset() != null;
    
    boolean hasFullMessage = request.getKey() != null || request.getValue() != null;
    
    if (!hasSourceReference && !hasFullMessage) {
      throw new ValidationException(
          "Either provide source topic reference (sourceTopic, sourcePartition, sourceOffset) " +
          "or full message data (key, value)"
      );
    }
    
    if (hasSourceReference && (request.getSourcePartition() < 0 || request.getSourceOffset() < 0)) {
      throw new ValidationException("Source partition and offset must be non-negative");
    }
  }

  /**
   * Fetches a message from the source topic at the specified partition and offset.
   */
  private ConsumerRecord<byte[], byte[]> fetchMessage(
      KafkaCluster cluster,
      String sourceTopic,
      int partition,
      long offset) {
    
    Properties consumerProps = new Properties();
    consumerProps.putAll(cluster.getProperties());
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-ui-reproduce-" + System.currentTimeMillis());
    consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    
    try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(consumerProps)) {
      TopicPartition topicPartition = new TopicPartition(sourceTopic, partition);
      consumer.assign(Collections.singletonList(topicPartition));
      consumer.seek(topicPartition, offset);
      
      var records = consumer.poll(Duration.ofSeconds(10));
      
      for (ConsumerRecord<byte[], byte[]> record : records) {
        if (record.partition() == partition && record.offset() == offset) {
          return record;
        }
      }
      
      throw new ValidationException(
          String.format("Message not found at topic: %s, partition: %d, offset: %d",
              sourceTopic, partition, offset)
      );
    }
  }

  /**
   * Extracts headers from a consumer record.
   */
  private Map<String, String> extractHeaders(ConsumerRecord<byte[], byte[]> record) {
    Map<String, String> headers = new HashMap<>();
    for (Header header : record.headers()) {
      headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
    }
    return headers;
  }

  /**
   * Produces a message to the target topic.
   */
  private RecordMetadata produceMessage(
      KafkaCluster cluster,
      String targetTopic,
      Integer targetPartition,
      byte[] key,
      byte[] value,
      Map<String, String> headers,
      Long timestamp) {
    
    try (KafkaProducer<byte[], byte[]> producer = MessagesService.createProducer(cluster, Map.of())) {
      ProducerRecord<byte[], byte[]> record;
      
      if (timestamp != null) {
        record = new ProducerRecord<>(
            targetTopic,
            targetPartition,
            timestamp,
            key,
            value
        );
      } else {
        record = new ProducerRecord<>(
            targetTopic,
            targetPartition,
            key,
            value
        );
      }
      
      // Add headers
      if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          record.headers().add(new RecordHeader(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8)));
        }
      }
      
      return producer.send(record).get();
    } catch (Exception e) {
      throw new RuntimeException("Failed to produce message to target topic", e);
    }
  }
}
