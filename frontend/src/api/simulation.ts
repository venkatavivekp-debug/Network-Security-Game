import { apiRequest } from "./client";
import type { EvaluationRunResponse, SimulationRunRequest, SimulationRunResponse } from "./types";

export const simulationApi = {
  run: (body: SimulationRunRequest) =>
    apiRequest<SimulationRunResponse>("/simulation/run", { method: "POST", body: JSON.stringify(body) }),
  history: () => apiRequest<SimulationRunResponse[]>("/simulation/history", { method: "GET" }),
  evaluations: () => apiRequest<EvaluationRunResponse[]>("/simulation/evaluations", { method: "GET" }),
};

