import { apiRequest } from "./client";
import type {
  MessageDecryptResponse,
  MessageSendRequest,
  MessageSendResponse,
  MessageSummaryResponse,
  PuzzleChallengeResponse,
  PuzzleSolveResponse,
} from "./types";

export const messageApi = {
  send: (body: MessageSendRequest) =>
    apiRequest<MessageSendResponse>("/message/send", { method: "POST", body: JSON.stringify(body) }),
  received: () => apiRequest<MessageSummaryResponse[]>("/message/received", { method: "GET" }),
  decrypt: (id: number) => apiRequest<MessageDecryptResponse>(`/message/decrypt/${id}`, { method: "POST" }),
};

export const puzzleApi = {
  challenge: (id: number) => apiRequest<PuzzleChallengeResponse>(`/puzzle/${id}/challenge`, { method: "GET" }),
  solve: (id: number, nonce: number) =>
    apiRequest<PuzzleSolveResponse>(`/puzzle/${id}/solve`, {
      method: "POST",
      body: JSON.stringify({ nonce }),
    }),
};

