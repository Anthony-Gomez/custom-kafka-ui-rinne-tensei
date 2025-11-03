package com.provectus.kafka.ui.controller;

import com.provectus.kafka.ui.model.ReproduceMessageRequestDTO;
import com.provectus.kafka.ui.model.ReproduceMessageResponseDTO;
import com.provectus.kafka.ui.model.rbac.AccessContext;
import com.provectus.kafka.ui.model.rbac.permission.TopicAction;
import com.provectus.kafka.ui.service.ReproduceMessageService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Controller for message reproduction operations.
 * Provides endpoint to reproduce messages from a DLT to a target topic.
 */
@RestController
@RequestMapping("/api/clusters/{clusterName}/topics/{targetTopicName}")
@RequiredArgsConstructor
@Slf4j
public class ReproduceMessageController extends AbstractController {

  private final ReproduceMessageService reproduceMessageService;

  /**
   * Reproduces a single message from a DLT (dead-letter topic) to a target topic.
   * 
   * <p>Supports two modes:</p>
   * <ul>
   *   <li>Message reference mode: Provide sourceTopic, sourcePartition, sourceOffset to fetch the message</li>
   *   <li>Full message data mode: Provide key, value, headers directly (base64 encoded for key/value)</li>
   * </ul>
   * 
   * <p>Preserves key, value, headers, and optionally timestamp/partition.</p>
   * 
   * <p><b>Security Note:</b> The backend principal must have READ permission on the source DLT topic 
   * and WRITE permission on the target topic.</p>
   * 
   * @param clusterName Name of the Kafka cluster
   * @param targetTopicName Name of the target topic to reproduce the message to
   * @param request The reproduction request
   * @param exchange Server web exchange
   * @return Response containing the result of the reproduction operation
   */
  @PostMapping("/reproduce")
  public Mono<ResponseEntity<ReproduceMessageResponseDTO>> reproduceMessage(
      @PathVariable String clusterName,
      @PathVariable String targetTopicName,
      @Valid @RequestBody ReproduceMessageRequestDTO request,
      ServerWebExchange exchange) {

    log.info("Reproduce message request for cluster: {}, target topic: {}", clusterName, targetTopicName);

    // Build access context for target topic (WRITE permission required)
    var targetContext = AccessContext.builder()
        .cluster(clusterName)
        .topic(targetTopicName)
        .topicActions(TopicAction.MESSAGES_PRODUCE)
        .operationName("reproduceMessage")
        .build();

    // Build access context for source topic (READ permission required) if source topic is provided
    var contextValidation = request.getSourceTopic() != null
        ? validateAccess(
            AccessContext.builder()
                .cluster(clusterName)
                .topic(request.getSourceTopic())
                .topicActions(TopicAction.MESSAGES_READ)
                .operationName("reproduceMessage")
                .build()
        ).then(validateAccess(targetContext))
        : validateAccess(targetContext);

    return contextValidation
        .then(reproduceMessageService.reproduceMessage(
            getCluster(clusterName),
            targetTopicName,
            request
        ))
        .map(ResponseEntity::ok)
        .doOnEach(sig -> audit(targetContext, sig));
  }
}
