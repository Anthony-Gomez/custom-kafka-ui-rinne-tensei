package com.provectus.kafka.ui.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Data;

/**
 * Request DTO for reproducing a message from a DLT to a target topic.
 * Supports two modes:
 * 1. Message reference mode: provide sourceTopic, sourcePartition, sourceOffset
 * 2. Full message data mode: provide key, value, headers directly
 */
@Data
public class ReproduceMessageRequestDTO {

  /**
   * Source topic name (DLT) - required if fetching message by reference.
   */
  @JsonProperty("sourceTopic")
  @Nullable
  private String sourceTopic;

  /**
   * Source partition - required if fetching message by reference.
   */
  @JsonProperty("sourcePartition")
  @Nullable
  private Integer sourcePartition;

  /**
   * Source offset - required if fetching message by reference.
   */
  @JsonProperty("sourceOffset")
  @Nullable
  private Long sourceOffset;

  /**
   * Base64-encoded message key - required if providing full message data.
   */
  @JsonProperty("key")
  @Nullable
  private String key;

  /**
   * Base64-encoded message value - required if providing full message data.
   */
  @JsonProperty("value")
  @Nullable
  private String value;

  /**
   * Message headers to preserve.
   */
  @JsonProperty("headers")
  @Nullable
  private Map<String, String> headers;

  /**
   * Target partition (optional, will use default partitioner if not specified).
   */
  @JsonProperty("targetPartition")
  @Nullable
  private Integer targetPartition;

  /**
   * Whether to preserve the original timestamp.
   */
  @JsonProperty("preserveTimestamp")
  private boolean preserveTimestamp = false;

  /**
   * If true, validate the request without actually producing the message.
   */
  @JsonProperty("dryRun")
  private boolean dryRun = false;
}
