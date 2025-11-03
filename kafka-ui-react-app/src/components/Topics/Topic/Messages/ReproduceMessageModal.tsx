import React, { useState } from 'react';
import { Button } from 'components/common/Button/Button';
import Input from 'components/common/Input/Input';
import { InputLabel } from 'components/common/Input/InputLabel.styled';
import toast from 'react-hot-toast';
import { ReproduceMessageRequest } from 'lib/types/reproduceMessage';
import * as S from './ReproduceMessageModal.styled';

interface ReproduceMessageModalProps {
  clusterName: string;
  sourceTopic?: string;
  partition?: number;
  offset?: number;
  messageKey?: string;
  messageValue?: string;
  headers?: { [key: string]: string };
  isOpen: boolean;
  onClose: () => void;
  onReproduce: (request: ReproduceMessageRequest) => Promise<void>;
}

const ReproduceMessageModal: React.FC<ReproduceMessageModalProps> = ({
  clusterName,
  sourceTopic,
  partition,
  offset,
  messageKey,
  messageValue,
  headers,
  isOpen,
  onClose,
  onReproduce,
}) => {
  const [targetTopic, setTargetTopic] = useState('');
  const [targetPartition, setTargetPartition] = useState<string>('');
  const [editableKey, setEditableKey] = useState(messageKey || '');
  const [editableValue, setEditableValue] = useState(messageValue || '');
  const [editableHeaders, setEditableHeaders] = useState(
    headers ? JSON.stringify(headers, null, 2) : '{}'
  );
  const [preserveTimestamp, setPreserveTimestamp] = useState(false);
  const [dryRun, setDryRun] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!targetTopic) {
      toast.error('Target topic is required');
      return;
    }

    // Parse headers
    let parsedHeaders: { [key: string]: string } = {};
    try {
      parsedHeaders = JSON.parse(editableHeaders);
    } catch (error) {
      toast.error('Invalid JSON for headers');
      return;
    }

    setIsSubmitting(true);

    try {
      // Encode key and value to base64
      const keyBase64 = editableKey
        ? btoa(new TextEncoder().encode(editableKey).reduce((data, byte) => data + String.fromCharCode(byte), ''))
        : undefined;
      const valueBase64 = editableValue
        ? btoa(new TextEncoder().encode(editableValue).reduce((data, byte) => data + String.fromCharCode(byte), ''))
        : undefined;

      const request: ReproduceMessageRequest = {
        targetTopic,
        sourceTopic,
        sourcePartition: partition,
        sourceOffset: offset,
        key: keyBase64,
        value: valueBase64,
        headers: parsedHeaders,
        targetPartition: targetPartition ? parseInt(targetPartition, 10) : undefined,
        preserveTimestamp,
        dryRun,
      };

      await onReproduce(request);
      
      if (dryRun) {
        toast.success('Dry run successful - message validated');
      } else {
        toast.success('Message reproduced successfully');
      }
      
      onClose();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : 'Failed to reproduce message'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <S.Wrapper role="dialog" aria-label="Reproduce Message Dialog">
      <S.Overlay onClick={onClose} aria-hidden="true" />
      <S.Modal>
        <S.Header>Reproduce Message to Target Topic</S.Header>
        <S.Form onSubmit={handleSubmit}>
          <S.Content>
            {sourceTopic && (
              <S.InfoSection>
                <p>
                  <strong>Source:</strong> {sourceTopic} (partition: {partition}, offset: {offset})
                </p>
              </S.InfoSection>
            )}
            
            <div>
              <InputLabel htmlFor="targetTopic">
                Target Topic <span style={{ color: 'red' }}>*</span>
              </InputLabel>
              <Input
                id="targetTopic"
                type="text"
                value={targetTopic}
                onChange={(e) => setTargetTopic(e.target.value)}
                placeholder="Enter target topic name"
                required
              />
            </div>

            <div>
              <InputLabel htmlFor="targetPartition">
                Target Partition (optional)
              </InputLabel>
              <Input
                id="targetPartition"
                type="number"
                value={targetPartition}
                onChange={(e) => setTargetPartition(e.target.value)}
                placeholder="Leave empty for default partitioner"
                min="0"
              />
            </div>

            <div>
              <InputLabel htmlFor="messageKey">Message Key</InputLabel>
              <S.TextArea
                id="messageKey"
                value={editableKey}
                onChange={(e) => setEditableKey(e.target.value)}
                placeholder="Message key"
                rows={3}
              />
            </div>

            <div>
              <InputLabel htmlFor="messageValue">Message Value</InputLabel>
              <S.TextArea
                id="messageValue"
                value={editableValue}
                onChange={(e) => setEditableValue(e.target.value)}
                placeholder="Message value"
                rows={8}
              />
            </div>

            <div>
              <InputLabel htmlFor="headers">Headers (JSON)</InputLabel>
              <S.TextArea
                id="headers"
                value={editableHeaders}
                onChange={(e) => setEditableHeaders(e.target.value)}
                placeholder='{"header1": "value1"}'
                rows={4}
              />
            </div>

            <S.CheckboxWrapper>
              <input
                type="checkbox"
                id="preserveTimestamp"
                checked={preserveTimestamp}
                onChange={(e) => setPreserveTimestamp(e.target.checked)}
              />
              <label htmlFor="preserveTimestamp">Preserve original timestamp</label>
            </S.CheckboxWrapper>

            <S.CheckboxWrapper>
              <input
                type="checkbox"
                id="dryRun"
                checked={dryRun}
                onChange={(e) => setDryRun(e.target.checked)}
              />
              <label htmlFor="dryRun">Dry run (validate without producing)</label>
            </S.CheckboxWrapper>

            <S.PermissionNote>
              <strong>Note:</strong> The backend principal must have READ permission on the source 
              topic and WRITE permission on the target topic.
            </S.PermissionNote>
          </S.Content>

          <S.Footer>
            <Button
              buttonType="secondary"
              buttonSize="M"
              onClick={onClose}
              type="button"
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              buttonType="primary"
              buttonSize="M"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Processing...' : dryRun ? 'Validate' : 'Reproduce'}
            </Button>
          </S.Footer>
        </S.Form>
      </S.Modal>
    </S.Wrapper>
  );
};

export default ReproduceMessageModal;
