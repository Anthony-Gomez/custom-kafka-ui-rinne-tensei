import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ReproduceMessageRequest } from 'lib/types/reproduceMessage';
import ReproduceMessageModal from '../ReproduceMessageModal';

describe('ReproduceMessageModal', () => {
  const defaultProps = {
    clusterName: 'test-cluster',
    sourceTopic: 'dlt-topic',
    partition: 0,
    offset: 100,
    messageKey: 'test-key',
    messageValue: '{"data": "test"}',
    headers: { 'header1': 'value1' },
    isOpen: true,
    onClose: jest.fn(),
    onReproduce: jest.fn(),
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders the modal when isOpen is true', () => {
    render(<ReproduceMessageModal {...defaultProps} />);
    
    expect(screen.getByText('Reproduce Message to Target Topic')).toBeInTheDocument();
    expect(screen.getByLabelText(/Target Topic/i)).toBeInTheDocument();
  });

  it('does not render the modal when isOpen is false', () => {
    render(<ReproduceMessageModal {...defaultProps} isOpen={false} />);
    
    expect(screen.queryByText('Reproduce Message to Target Topic')).not.toBeInTheDocument();
  });

  it('auto-prefills key, value, and headers from props', () => {
    render(<ReproduceMessageModal {...defaultProps} />);
    
    const keyTextarea = screen.getByLabelText('Message Key') as HTMLTextAreaElement;
    const valueTextarea = screen.getByLabelText('Message Value') as HTMLTextAreaElement;
    const headersTextarea = screen.getByLabelText(/Headers/i) as HTMLTextAreaElement;
    
    expect(keyTextarea.value).toBe('test-key');
    expect(valueTextarea.value).toBe('{"data": "test"}');
    expect(headersTextarea.value).toContain('header1');
  });

  it('displays source information when provided', () => {
    render(<ReproduceMessageModal {...defaultProps} />);
    
    expect(screen.getByText(/Source:/i)).toBeInTheDocument();
    expect(screen.getByText(/dlt-topic/i)).toBeInTheDocument();
    expect(screen.getByText(/partition: 0/i)).toBeInTheDocument();
    expect(screen.getByText(/offset: 100/i)).toBeInTheDocument();
  });

  it('calls onClose when Cancel button is clicked', () => {
    render(<ReproduceMessageModal {...defaultProps} />);
    
    const cancelButton = screen.getByText('Cancel');
    fireEvent.click(cancelButton);
    
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onReproduce with correct data when form is submitted', async () => {
    const mockOnReproduce = jest.fn().mockResolvedValue(undefined);
    
    render(
      <ReproduceMessageModal
        {...defaultProps}
        onReproduce={mockOnReproduce}
      />
    );
    
    // Fill in target topic
    const targetTopicInput = screen.getByLabelText(/Target Topic/i);
    fireEvent.change(targetTopicInput, { target: { value: 'target-topic' } });
    
    // Submit form
    const reproduceButton = screen.getByText('Reproduce');
    fireEvent.click(reproduceButton);
    
    await waitFor(() => {
      expect(mockOnReproduce).toHaveBeenCalledWith(
        expect.objectContaining({
          targetTopic: 'target-topic',
          sourceTopic: 'dlt-topic',
          sourcePartition: 0,
          sourceOffset: 100,
          dryRun: false,
        })
      );
    });
  });

  it('shows error toast when target topic is missing', async () => {
    render(<ReproduceMessageModal {...defaultProps} />);
    
    const reproduceButton = screen.getByText('Reproduce');
    fireEvent.click(reproduceButton);
    
    // onReproduce should not be called when validation fails
    await waitFor(() => {
      expect(defaultProps.onReproduce).not.toHaveBeenCalled();
    });
  });

  it('enables dry run mode when checkbox is checked', async () => {
    const mockOnReproduce = jest.fn().mockResolvedValue(undefined);
    
    render(
      <ReproduceMessageModal
        {...defaultProps}
        onReproduce={mockOnReproduce}
      />
    );
    
    // Fill in target topic
    const targetTopicInput = screen.getByLabelText(/Target Topic/i);
    fireEvent.change(targetTopicInput, { target: { value: 'target-topic' } });
    
    // Enable dry run
    const dryRunCheckbox = screen.getByLabelText(/Dry run/i);
    fireEvent.click(dryRunCheckbox);
    
    // Submit form
    const validateButton = screen.getByText('Validate');
    fireEvent.click(validateButton);
    
    await waitFor(() => {
      expect(mockOnReproduce).toHaveBeenCalledWith(
        expect.objectContaining({
          dryRun: true,
        })
      );
    });
  });

  it('enables preserve timestamp when checkbox is checked', async () => {
    const mockOnReproduce = jest.fn().mockResolvedValue(undefined);
    
    render(
      <ReproduceMessageModal
        {...defaultProps}
        onReproduce={mockOnReproduce}
      />
    );
    
    const targetTopicInput = screen.getByLabelText(/Target Topic/i);
    fireEvent.change(targetTopicInput, { target: { value: 'target-topic' } });
    
    const preserveTimestampCheckbox = screen.getByLabelText(/Preserve original timestamp/i);
    fireEvent.click(preserveTimestampCheckbox);
    
    const reproduceButton = screen.getByText('Reproduce');
    fireEvent.click(reproduceButton);
    
    await waitFor(() => {
      expect(mockOnReproduce).toHaveBeenCalledWith(
        expect.objectContaining({
          preserveTimestamp: true,
        })
      );
    });
  });

  it('allows editing message key and value', () => {
    render(<ReproduceMessageModal {...defaultProps} />);
    
    const keyTextarea = screen.getByLabelText('Message Key') as HTMLTextAreaElement;
    const valueTextarea = screen.getByLabelText('Message Value') as HTMLTextAreaElement;
    
    fireEvent.change(keyTextarea, { target: { value: 'new-key' } });
    fireEvent.change(valueTextarea, { target: { value: '{"updated": "data"}' } });
    
    expect(keyTextarea.value).toBe('new-key');
    expect(valueTextarea.value).toBe('{"updated": "data"}');
  });

  it('displays permission note', () => {
    render(<ReproduceMessageModal {...defaultProps} />);
    
    expect(screen.getByText(/backend principal must have READ permission/i)).toBeInTheDocument();
  });
});
