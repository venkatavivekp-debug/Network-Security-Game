import { apiRequest } from "./client";
import type { MessageDecryptResponse, MessageSendRequest, MessageSendResponse, MessageSummaryResponse } from "./types";

export const messageApi = {
  send: (body: MessageSendRequest) =>
    apiRequest<MessageSendResponse>("/message/send", { method: "POST", body: JSON.stringify(body) }),
  received: () => apiRequest<MessageSummaryResponse[]>("/message/received", { method: "GET" }),
  decrypt: (id: number) => apiRequest<MessageDecryptResponse>(`/message/decrypt/${id}`, { method: "POST" }),
};

