# Message Reproduction Feature

## Overview

This feature allows users to reproduce single messages from a Dead Letter Topic (DLT) to a target topic through the Kafka UI. It supports both message reference mode (fetching from source topic) and full message data mode (providing the message directly).

## Backend Components

### API Endpoint

```
POST /api/clusters/{clusterName}/topics/{targetTopicName}/reproduce
```

### DTOs

- **ReproduceMessageRequestDTO**: Request containing either:
  - Message reference: `sourceTopic`, `sourcePartition`, `sourceOffset`
  - Full message data: `key`, `value` (base64 encoded), `headers`
- **ReproduceMessageResponseDTO**: Response with success status, produced message metadata, or error details

### Service

**ReproduceMessageService** handles:
- Validation of request parameters
- Fetching messages from source topic (if using reference mode)
- Producing messages to target topic
- Dry-run mode for validation without producing
- Preservation of key, value, headers, and optionally timestamp

### Controller

**ReproduceMessageController** provides:
- REST endpoint implementation
- RBAC validation for both source (READ) and target (WRITE) topics
- Audit logging

## Frontend Components

### ReproduceMessageModal

React modal component that:
- Auto-prefills key, value, and headers from the currently viewed message
- Allows editing message data before reproduction
- Supports target partition selection
- Provides dry-run mode checkbox
- Provides preserve timestamp checkbox
- Shows permission notes for operators

### Integration

The modal is accessible via the dropdown menu on each message row in the Messages view:
1. View messages in a topic
2. Click the dropdown menu on a message row
3. Select "Reproduce to topic..."
4. The modal opens with pre-filled message data
5. Enter target topic and optionally modify message data
6. Click "Reproduce" or "Validate" (for dry-run)

## Permissions

**IMPORTANT**: The backend principal must have:
- **READ** permission on the source DLT topic
- **WRITE** permission on the target topic

These permissions are validated by the controller before processing the request.

## Usage Examples

### Using Message Reference (Fetch from Source)

```json
{
  "sourceTopic": "my-dlt-topic",
  "sourcePartition": 0,
  "sourceOffset": 12345,
  "targetPartition": null,
  "preserveTimestamp": false,
  "dryRun": false
}
```

### Using Full Message Data

```json
{
  "key": "dGVzdC1rZXk=",
  "value": "eyJkYXRhIjoidGVzdCJ9",
  "headers": {
    "correlation-id": "12345",
    "source": "dlt-topic"
  },
  "targetPartition": 2,
  "preserveTimestamp": true,
  "dryRun": false
}
```

### Dry Run Mode

Set `dryRun: true` to validate the request without actually producing the message.

## Testing

### Backend Tests

- **ReproduceMessageServiceTest**: Unit tests for service logic
  - Request validation
  - Dry-run mode
  - Error handling

### Frontend Tests

- **ReproduceMessageModal.spec.tsx**: Unit tests for modal component
  - Rendering
  - Auto-prefilling
  - Form submission
  - Dry-run mode
  - Preserve timestamp option

## Security Considerations

1. **Authentication**: Uses existing Kafka UI authentication
2. **Authorization**: RBAC checks for both source and target topics
3. **Audit**: All reproduction operations are logged
4. **Validation**: Input validation on both frontend and backend
5. **Base64 Encoding**: Key and value are base64-encoded for safe transport

## Limitations

- **Single Message Only**: No batch reproduction support
- **Synchronous**: Operations are synchronous (not designed for high-volume reproduction)
- **No Schema Evolution**: Does not handle schema evolution automatically
- **Network Access**: Backend must have network access to Kafka cluster

## Future Enhancements

Potential improvements for future versions:
- Batch message reproduction
- Schema evolution support
- Message transformation during reproduction
- Scheduled/delayed reproduction
- Reproduction history tracking
