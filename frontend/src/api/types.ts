export type Role = "SENDER" | "RECEIVER";

export type AlgorithmType = "NORMAL" | "SHCS" | "CPHS";

export interface ApiSuccessResponse<T> {
  timestamp: string;
  success: boolean;
  message: string;
  path: string;
  data: T;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: string[];
}

export interface AuthResponse {
  username: string;
  role: Role;
  message: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  role: Role;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface MessageSendRequest {
  receiverUsername: string;
  content: string;
  algorithmType: AlgorithmType;
}

export interface MessageSendResponse {
  id: number;
  message: string;
}

export interface MessageSummaryResponse {
  id: number;
  senderUsername: string;
  receiverUsername: string;
  encryptedContent: string;
  algorithmType: AlgorithmType;
  metadata: string | null;
  createdAt: string;
}

export interface MessageDecryptResponse {
  messageId: number;
  senderUsername: string;
  receiverUsername: string;
  algorithmType: AlgorithmType;
  decryptedContent: string;
  metadata: string | null;
}

export interface SimulationRunRequest {
  numNodes: number;
  numEdges: number;
  attackBudget: number;
  defenseBudget: number;
  recoveryBudget: number;
  algorithmType?: AlgorithmType;
  messageId?: number;
}

export interface SimulationRunResponse {
  simulationRunId: number;
  initialConnectivity: number;
  afterAttackConnectivity: number;
  afterRecoveryConnectivity: number;
  nodesLost: number;
  edgesLost: number;
  recoveryRate: number;
  defenderUtility: number;
  attackerUtility: number;
  algorithmType: AlgorithmType;
  effectiveAttackSuccessProbability: number;
  createdAt: string;
}

export interface EvaluationAggregateMetricsResponse {
  // Keep generic; the backend has a nested DTO with many fields.
  [key: string]: unknown;
}

export interface EvaluationRunResponse {
  evaluationRunId: number;
  scenarioId: number;
  comparisonType: string;
  scenarioName: string;
  numNodes: number;
  numEdges: number;
  attackBudget: number;
  defenseBudget: number;
  recoveryBudget: number;
  rounds: number;
  algorithmType: AlgorithmType;
  enableMTD: boolean;
  enableDeception: boolean;
  repetitions: number;
  seedStrategy: string;
  baseSeed: number | null;
  usedSeeds: number[];
  aggregateMetrics: EvaluationAggregateMetricsResponse | null;
  createdAt: string;
}

