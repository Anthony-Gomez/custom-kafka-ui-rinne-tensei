import { BASE_PARAMS } from 'lib/constants';
import { ClusterName } from 'redux/interfaces';
import { showServerError } from 'lib/errorHandling';
import { ReproduceMessageRequest, ReproduceMessageResponse } from 'lib/types/reproduceMessage';

export const reproduceMessage = async (
  clusterName: ClusterName,
  targetTopicName: string,
  request: ReproduceMessageRequest
): Promise<ReproduceMessageResponse> => {
  const url = `${BASE_PARAMS.basePath}/api/clusters/${encodeURIComponent(
    clusterName
  )}/topics/${encodeURIComponent(targetTopicName)}/reproduce`;

  // Remove targetTopic from request body as it's in the path
  const { targetTopic, ...requestBody } = request;

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `HTTP ${response.status}: ${response.statusText}`);
    }

    const result: ReproduceMessageResponse = await response.json();
    
    if (!result.success && result.errorMessage) {
      throw new Error(result.errorMessage);
    }

    return result;
  } catch (error) {
    showServerError(error as Error);
    throw error;
  }
};
