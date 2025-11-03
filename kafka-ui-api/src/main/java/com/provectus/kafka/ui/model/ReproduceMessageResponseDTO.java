package com.provectus.kafka.ui.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for message reproduction operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReproduceMessageResponseDTO {

  /**
   * Whether the message reproduction was successful.
   */
  @JsonProperty("success")
  private boolean success;

  /**
   * Partition where the message was produced.
   */
  @JsonProperty("partition")
  @Nullable
  private Integer partition;

  /**
   * Offset of the produced message.
   */
  @JsonProperty("offset")
  @Nullable
  private Long offset;

  /**
   * Timestamp of the produced message.
   */
  @JsonProperty("timestamp")
  @Nullable
  private Long timestamp;

  /**
   * Whether this was a dry run.
   */
  @JsonProperty("dryRun")
  private boolean dryRun;

  /**
   * Error message if reproduction failed.
   */
  @JsonProperty("errorMessage")
  @Nullable
  private String errorMessage;
}
