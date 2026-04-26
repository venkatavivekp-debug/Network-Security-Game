export type Role = "SENDER" | "RECEIVER" | "ADMIN";

export type AlgorithmType = "NORMAL" | "SHCS" | "CPHS";

export type RecoveryState =
  | "NORMAL"
  | "CHALLENGE_REQUIRED"
  | "ESCALATED"
  | "HELD"
  | "ADMIN_REVIEW_REQUIRED"
  | "RECOVERY_IN_PROGRESS"
  | "RECOVERED"
  | "FAILED";

export type RiskLevel = "LOW" | "ELEVATED" | "HIGH" | "CRITICAL";

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

export type PuzzleType = "POW_SHA256" | "ARITHMETIC" | "ENCODED" | "PATTERN";

export interface MessageSendRequest {
  receiverUsername: string;
  content: string;
  algorithmType: AlgorithmType;
  puzzleType?: PuzzleType;
}

export interface MessageSendResponse {
  messageId: number;
  senderUsername: string;
  receiverUsername: string;
  requestedAlgorithmType: AlgorithmType;
  effectiveAlgorithmType: AlgorithmType;
  escalated: boolean;
  communicationHold: boolean;
  riskScore: number | null;
  riskLevel: RiskLevel | null;
  riskReasons: string[] | null;
  escalationReason: string | null;
  recoveryState: RecoveryState | null;
  adminReviewRequired: boolean;
  recoverySummary: string | null;
  recoveryNextSteps: string[] | null;
  connectionSecurityState: ConnectionSecurityState | null;
  connectionShiftedSignals: string[] | null;
  warningMessage: string | null;
  createdAt: string;
  status: string;
}

export type ConnectionSecurityState = "STABLE" | "FIRST_SEEN" | "SHIFTED" | "ANOMALOUS";

export interface AdminConfirmationStatus {
  active: boolean;
  expiresAt?: string | null;
  ttlSeconds?: number;
}

export interface AdminRiskPolicySignal {
  name: string;
  description: string;
  weight?: number | string;
}

export interface AdminRiskPolicyLevelAction {
  level: RiskLevel;
  action: string;
}

export interface AdminRiskPolicyResponse {
  model: string;
  description: string;
  thresholds: Record<string, number>;
  signals: AdminRiskPolicySignal[];
  levelActions: AdminRiskPolicyLevelAction[];
  connectionStateContribution: Record<string, string>;
  limitations: string[];
}

export interface ExternalThreatEvent {
  id: number;
  eventType: string;
  actor: string | null;
  createdAt: string;
}

export interface ExternalThreatSummary {
  windowMinutes: number;
  blockedRequests: number;
  counters: {
    rateLimitBlocked: number;
    forbiddenAccess: number;
    validationRejected: number;
    sessionAnomaly: number;
    puzzleSolveFailure: number;
    loginFailure: number;
  };
  recent: ExternalThreatEvent[];
}

export interface MessageSummaryResponse {
  id: number;
  senderUsername: string;
  receiverUsername: string;
  encryptedContent: string;
  algorithmType: AlgorithmType;
  requestedAlgorithmType: AlgorithmType | null;
  status: string | null;
  riskScore: number | null;
  riskLevel: RiskLevel | null;
  warning: string | null;
  warningMessage: string | null;
  recoveryState: RecoveryState | null;
  adminReviewRequired: boolean;
  recoverySummary: string | null;
  recoveryNextSteps: string[] | null;
  metadata: string | null;
  createdAt: string;
}

export interface PuzzleChallengeResponse {
  messageId: number;
  puzzleType: PuzzleType;
  question: string;
  challenge: string;
  targetHash: string | null;
  maxIterations: number;
  attemptsAllowed: number;
  attemptsUsed: number;
  expiresAt: string | null;
  solved: boolean;
}

export interface SystemPressureResponse {
  pressure: number;
  level: "CALM" | "WATCH" | "ELEVATED" | "CRITICAL";
  details: {
    threatLevel: number;
    recentPuzzleAttempts: number;
    recentPuzzleFailures: number;
    recentEscalations: number;
    recentAdminActions: number;
    usersAtRisk: number;
    puzzleFailureRate: number;
  };
}

export interface PuzzleSolveResponse {
  messageId: number;
  solved: boolean;
  attemptsAllowed: number;
  attemptsUsed: number;
  solvedAt: string | null;
  status: string;
}

export interface HeldMessageView {
  messageId: number;
  senderUsername: string;
  receiverUsername: string;
  requestedMode: AlgorithmType | null;
  enforcedMode: AlgorithmType | null;
  riskScore: number | null;
  riskLevel: RiskLevel | null;
  holdReason: string | null;
  recoveryState: RecoveryState | null;
  createdAt: string | null;
}

export interface UserAtRiskView {
  username: string;
  puzzleAttempts: number;
  puzzleSuccesses: number;
  puzzleFailures: number;
  consecutiveFailures: number;
  avgSolveTimeMs: number;
  recoveryEvents: number;
  lastFailureAt: string | null;
  lastSuccessAt: string | null;
}

export interface RecoveryPolicyEntry {
  state: RecoveryState;
  terminalGood: boolean;
  summary: string;
  nextSteps: string[];
}

export interface AuditEventView {
  id: number;
  eventType: string;
  actorUsername: string | null;
  subjectUsername: string | null;
  ipHash: string | null;
  fingerprintHash: string | null;
  riskScore: number | null;
  details: string | null;
  createdAt: string;
}

export interface MessageDecryptResponse {
  messageId: number;
  algorithmType: AlgorithmType;
  decryptedContent: string;
  puzzleSolveTimeMs: number;
  status: string;
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

