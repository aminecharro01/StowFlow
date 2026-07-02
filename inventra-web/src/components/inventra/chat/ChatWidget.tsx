"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { T } from "@/lib/theme";
import { useT } from "@/lib/i18n";
import { apiGet, apiSend } from "@/lib/api";
import { loadSession } from "@/lib/auth";
import {
  filterChatAction,
  filterConfirmAction,
  getQuickChipsForRole,
  welcomeForRole,
} from "@/lib/chatRbac";
import { parseAppRole } from "@/lib/rolePrivileges";
import { useConfirm } from "@/components/inventra/ui/useConfirm";
import { useChat } from "./useChat";
import type { ChatAction, ChatBootstrapResponse, ConfirmAction } from "./chatTypes";

function ChatIcon({ size = 22 }: { size?: number }) {
  return (
    <svg width={size} height={size} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SuggestionChip({
  label,
  disabled,
  onPick,
}: {
  label: string;
  disabled: boolean;
  onPick: (text: string) => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onPick(label)}
      className="rounded-full border px-3 py-1 text-xs font-medium transition-colors hover:bg-blue-50 disabled:opacity-50"
      style={{ borderColor: T.border, color: T.primary }}
    >
      {label}
    </button>
  );
}

export function ChatWidget() {
  const { t } = useT();
  const router = useRouter();
  const { confirm, dialog } = useConfirm();
  const { messages, loading, sendMessage, reset } = useChat();
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [toast, setToast] = useState<string | null>(null);
  const [bootstrap, setBootstrap] = useState<ChatBootstrapResponse | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const session = loadSession();
  const role = session?.role;
  const parsedRole = parseAppRole(role);

  const quickChips =
    bootstrap?.suggestions?.length
      ? bootstrap.suggestions
      : getQuickChipsForRole(role);

  const welcomeText =
    bootstrap?.welcome ??
    welcomeForRole(parsedRole) ??
    t.chat.welcome(role ?? "utilisateur");

  useEffect(() => {
    if (!open || bootstrap) return;
    apiGet<ChatBootstrapResponse>("/api/chat/bootstrap")
      .then(setBootstrap)
      .catch(() => setBootstrap(null));
  }, [open, bootstrap]);

  useEffect(() => {
    if (!scrollRef.current) return;
    scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, open]);

  const handleSend = useCallback(async (text?: string) => {
    const msg = (text ?? input).trim();
    if (!msg) return;
    setInput("");
    await sendMessage(msg);
  }, [input, sendMessage]);

  const handleNavigate = useCallback(
    (action: ChatAction) => {
      if (action.type !== "navigate" || !action.href) return;
      if (!filterChatAction(role, action)) return;
      setOpen(false);
      router.push(action.href);
    },
    [router, role],
  );

  const handleConfirm = useCallback(
    async (confirmAction: ConfirmAction) => {
      const allowed = filterConfirmAction(role, confirmAction);
      if (!allowed || allowed.type !== "create_replenishment") return;

      const p = allowed.payload;
      const articleId = Number(p.articleId);
      const supplierId = Number(p.supplierId);
      const quantity = Number(p.quantity);
      const note = typeof p.note === "string" ? p.note : "Via assistant StowFlow";

      if (!articleId || !supplierId || !quantity) return;

      const ok = await confirm({
        title: t.chat.confirmReplenishment.title,
        message: t.chat.confirmReplenishment.message(quantity, articleId),
        confirmLabel: t.chat.confirmReplenishment.confirm,
        variant: "default",
      });
      if (!ok) return;

      try {
        await apiSend("/api/replenishment-requests", "POST", {
          articleId,
          supplierId,
          quantity,
          note,
        });
        setToast(t.chat.confirmReplenishment.success);
        setTimeout(() => setToast(null), 4000);
        if (canAccessReplenishmentSummary(role)) {
          await sendMessage("demandes réappro en attente");
        }
      } catch (e) {
        const err = e instanceof Error ? e.message : t.chat.error;
        setToast(err);
        setTimeout(() => setToast(null), 5000);
      }
    },
    [confirm, role, sendMessage, t],
  );

  return (
    <>
      {dialog}

      {!open && (
        <button
          type="button"
          onClick={() => setOpen(true)}
          className="fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full shadow-lg transition-transform hover:scale-105"
          style={{ background: T.primary, color: "#fff" }}
          title={t.chat.openTitle}
          aria-label={t.chat.openTitle}
        >
          <ChatIcon size={26} />
        </button>
      )}

      {open && (
        <div
          className="fixed bottom-6 right-6 z-50 flex w-[min(100vw-2rem,380px)] flex-col overflow-hidden rounded-2xl shadow-2xl border"
          style={{ background: T.surface, borderColor: T.border, maxHeight: "min(520px, calc(100vh - 3rem))" }}
        >
          <header
            className="flex items-center justify-between px-4 py-3 text-white"
            style={{ background: T.sidebar }}
          >
            <div className="flex items-center gap-2 min-w-0">
              <ChatIcon size={20} />
              <div className="min-w-0">
                <span className="font-semibold text-sm block">{t.chat.title}</span>
                {parsedRole && (
                  <span className="text-[10px] opacity-75 truncate block">{roleLabel(parsedRole)}</span>
                )}
              </div>
            </div>
            <div className="flex items-center gap-1 shrink-0">
              {messages.length > 0 && (
                <button
                  type="button"
                  onClick={reset}
                  className="rounded px-2 py-1 text-xs opacity-80 hover:opacity-100"
                >
                  {t.chat.clear}
                </button>
              )}
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="rounded p-1 hover:bg-white/10"
                aria-label={t.common.cancel}
              >
                ✕
              </button>
            </div>
          </header>

          <div ref={scrollRef} className="flex-1 overflow-y-auto px-3 py-3 space-y-3 min-h-[200px] max-h-[340px]">
            {messages.length === 0 && (
              <div
                className="rounded-xl px-3 py-2 text-sm whitespace-pre-wrap"
                style={{ background: T.bg, color: T.text }}
              >
                {welcomeText}
              </div>
            )}

            {messages.length === 0 && (
              <div className="flex flex-wrap gap-2">
                {quickChips.map((chip) => (
                  <SuggestionChip
                    key={chip}
                    label={chip}
                    disabled={loading}
                    onPick={(t) => void handleSend(t)}
                  />
                ))}
              </div>
            )}

            {messages.map((m) => (
              <div
                key={m.id}
                className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}
              >
                <div
                  className="max-w-[92%] rounded-xl px-3 py-2 text-sm whitespace-pre-wrap"
                  style={
                    m.role === "user"
                      ? { background: T.primary, color: "#fff" }
                      : { background: T.bg, color: T.text }
                  }
                >
                  {m.pending ? t.chat.thinking : m.text}

                  {m.role === "bot" && !m.pending && m.suggestions && m.suggestions.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {m.suggestions.map((s) => (
                        <SuggestionChip
                          key={s}
                          label={s}
                          disabled={loading}
                          onPick={(t) => void handleSend(t)}
                        />
                      ))}
                    </div>
                  )}

                  {m.role === "bot" && !m.pending && m.actions && m.actions.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {m.actions
                        .filter((a) => filterChatAction(role, a))
                        .map((a) => (
                          <button
                            key={`${a.href}-${a.label}`}
                            type="button"
                            onClick={() => handleNavigate(a)}
                            className="rounded-lg px-2 py-1 text-xs font-medium text-white"
                            style={{ background: T.accent }}
                          >
                            {a.label}
                          </button>
                        ))}
                    </div>
                  )}

                  {m.role === "bot" && !m.pending && filterConfirmAction(role, m.confirmAction) && (
                    <div className="mt-2">
                      <button
                        type="button"
                        onClick={() => void handleConfirm(m.confirmAction!)}
                        className="rounded-lg px-3 py-1.5 text-xs font-semibold text-white"
                        style={{ background: T.success }}
                      >
                        {m.confirmAction!.label}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>

          {toast && (
            <div
              className="mx-3 mb-1 rounded-lg px-3 py-2 text-xs"
              style={{ background: "rgba(34,197,94,0.12)", color: T.success }}
            >
              {toast}
            </div>
          )}

          <form
            className="flex gap-2 border-t p-3"
            style={{ borderColor: T.border }}
            onSubmit={(e) => {
              e.preventDefault();
              void handleSend();
            }}
          >
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={t.chat.placeholder}
              disabled={loading}
              className="flex-1 rounded-lg border px-3 py-2 text-sm outline-none focus:ring-2"
              style={{ borderColor: T.border }}
              maxLength={2000}
            />
            <button
              type="submit"
              disabled={loading || !input.trim()}
              className="rounded-lg px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
              style={{ background: T.primary }}
            >
              →
            </button>
          </form>
        </div>
      )}
    </>
  );
}

function roleLabel(role: NonNullable<ReturnType<typeof parseAppRole>>): string {
  switch (role) {
    case "SUPER_ADMIN":
      return "Super administrateur";
    case "TENANT_ADMIN":
      return "Administrateur";
    case "STOCK_MANAGER":
      return "Gestionnaire de stock";
    case "MANAGER":
      return "Manager";
    case "SALES":
      return "Commercial";
  }
}

function canAccessReplenishmentSummary(role: string | null | undefined): boolean {
  const p = parseAppRole(role);
  return p === "STOCK_MANAGER" || p === "MANAGER" || p === "TENANT_ADMIN" || p === "SUPER_ADMIN";
}
