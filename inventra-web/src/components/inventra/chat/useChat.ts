"use client";

import { useCallback, useState } from "react";
import { apiSend } from "@/lib/api";
import { filterChatResponse } from "@/lib/chatRbac";
import { loadSession } from "@/lib/auth";
import type { ChatApiResponse, ChatMessage } from "./chatTypes";

function newId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

export function useChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);

  const sendMessage = useCallback(async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || loading) return;

    const role = loadSession()?.role;
    const userMsg: ChatMessage = { id: newId(), role: "user", text: trimmed };
    const pendingId = newId();
    setMessages((prev) => [
      ...prev,
      userMsg,
      { id: pendingId, role: "bot", text: "…", pending: true },
    ]);
    setLoading(true);

    try {
      const res = await apiSend<ChatApiResponse>("/api/chat", "POST", { message: trimmed });
      if (!res) throw new Error("Réponse vide");
      const safe = filterChatResponse(role, res);
      setMessages((prev) =>
        prev
          .filter((m) => m.id !== pendingId)
          .concat({
            id: newId(),
            role: "bot",
            text: safe.reply,
            suggestions: safe.suggestions,
            actions: safe.actions,
            confirmAction: safe.confirmAction,
          }),
      );
      return safe;
    } catch (e) {
      const err = e instanceof Error ? e.message : "Erreur assistant";
      setMessages((prev) =>
        prev
          .filter((m) => m.id !== pendingId)
          .concat({ id: newId(), role: "bot", text: err }),
      );
      return null;
    } finally {
      setLoading(false);
    }
  }, [loading]);

  const reset = useCallback(() => setMessages([]), []);

  return { messages, loading, sendMessage, reset };
}
