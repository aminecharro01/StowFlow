import { AuthGate } from "@/components/inventra/AuthGate";
import { RouteRoleGuard } from "@/components/inventra/RouteRoleGuard";
import { Sidebar } from "@/components/inventra/Sidebar";
import { ChatWidget } from "@/components/inventra/chat/ChatWidget";
import { QueryProvider } from "@/components/providers/QueryProvider";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <QueryProvider>
      <AuthGate>
        <RouteRoleGuard>
          <Sidebar />
          {children}
          <ChatWidget />
        </RouteRoleGuard>
      </AuthGate>
    </QueryProvider>
  );
}
