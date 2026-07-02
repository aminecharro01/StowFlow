export type ChatAction = {
  type: string;
  label: string;
  href: string;
};

export type ConfirmAction = {
  type: string;
  label: string;
  payload: Record<string, unknown>;
};

export type ChatApiResponse = {
  reply: string;
  suggestions: string[];
  actions: ChatAction[];
  confirmAction: ConfirmAction | null;
};

export type ChatMessage = {
  id: string;
  role: "user" | "bot";
  text: string;
  suggestions?: string[];
  actions?: ChatAction[];
  confirmAction?: ConfirmAction | null;
  pending?: boolean;
};

export type ChatBootstrapResponse = {
  welcome: string;
  suggestions: string[];
};
