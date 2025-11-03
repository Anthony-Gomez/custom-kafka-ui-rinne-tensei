/**
 * Shared types for message reproduction feature
 */

export interface ReproduceMessageRequest {
  targetTopic?: string;
  sourceTopic?: string;
  sourcePartition?: number;
  sourceOffset?: number;
  key?: string;
  value?: string;
  headers?: { [key: string]: string };
  targetPartition?: number;
  preserveTimestamp?: boolean;
  dryRun?: boolean;
}

export interface ReproduceMessageResponse {
  success: boolean;
  partition?: number;
  offset?: number;
  timestamp?: number;
  dryRun: boolean;
  errorMessage?: string;
}
