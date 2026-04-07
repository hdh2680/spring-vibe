import { Outlet } from "react-router-dom";
import SvHeader from "../../shared/components/SvHeader";
import ChatModalHost from "../../shared/components/ChatModalHost";

export default function AppLayout() {
  return (
    <div className="layout">
      <SvHeader />
      <main className="sv-main" aria-label="Content">
        <section className="content">
          <section aria-label="/app content">
            <Outlet />
          </section>
        </section>
      </main>
      <ChatModalHost />
    </div>
  );
}
