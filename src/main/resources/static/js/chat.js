(() => {
  const $ = (id) => document.getElementById(id);

  const modalEl = $("chatModal");
  const openBtn = $("chatOpen");
  const closeBtn = $("chatClose");
  const backdropEl = $("chatBackdrop");

  const logEl = $("chatLog");
  const formEl = $("chatForm");
  const msgEl = $("chatMessage");
  const sendBtn = $("chatSend");
  const clearBtn = $("chatClear");
  const healthEl = $("chatHealth");

  // If the layout doesn't include the chat UI (ex: login page), do nothing.
  if (!modalEl || !logEl || !formEl || !msgEl || !sendBtn || !clearBtn || !healthEl) {
    return;
  }

  function esc(s) {
    return String(s)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function append(role, text) {
    const row = document.createElement("div");
    row.className = `chat-msg chat-${role}`;
    row.innerHTML = `
      <div class="chat-bubble">
        <div class="chat-role">${esc(role)}</div>
        <div class="chat-text">${esc(text)}</div>
      </div>
    `;
    logEl.appendChild(row);
    logEl.scrollTop = logEl.scrollHeight;
  }

  async function checkHealth() {
    try {
      const r = await fetch("/api/chat/health", { headers: { "Accept": "application/json" } });
      const j = await r.json();
      if (j && j.ok) {
        healthEl.textContent = "대화가 가능합니다.";
        healthEl.classList.remove("is-bad");
      } else {
        healthEl.textContent = "현재 대화가 불가능합니다. (Ollama 또는 모델을 확인해주세요)";
        healthEl.classList.add("is-bad");
      }
    } catch (e) {
      healthEl.textContent = "현재 대화 서버에 연결할 수 없습니다. (http://localhost:11434)";
      healthEl.classList.add("is-bad");
    }
  }

  async function send(message) {
    const body = { message };
    const r = await fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Accept": "application/json" },
      body: JSON.stringify(body),
    });
    if (!r.ok) {
      const t = await r.text();
      throw new Error(t || `HTTP ${r.status}`);
    }
    return r.json();
  }

  function openModal() {
    modalEl.hidden = false;
    checkHealth();
    document.body.style.overflow = "hidden";
    // Delay focus until layout settles.
    setTimeout(() => msgEl.focus(), 0);
  }

  function closeModal() {
    modalEl.hidden = true;
    document.body.style.overflow = "";
  }

  formEl.addEventListener("submit", async (e) => {
    e.preventDefault();
    const message = (msgEl.value || "").trim();
    if (!message) return;

    append("user", message);
    msgEl.value = "";

    sendBtn.disabled = true;
    sendBtn.textContent = "보내는 중...";
    try {
      const res = await send(message);
      append("assistant", res.reply || "");
    } catch (err) {
      append("error", err.message || String(err));
    } finally {
      sendBtn.disabled = false;
      sendBtn.textContent = "보내기";
    }
  });

  clearBtn.addEventListener("click", async () => {
    try {
      await fetch("/api/chat", { method: "DELETE" });
    } catch (_) {}
    logEl.innerHTML = "";
  });

  if (openBtn) {
    openBtn.addEventListener("click", () => openModal());
  }
  if (closeBtn) {
    closeBtn.addEventListener("click", () => closeModal());
  }
  if (backdropEl) {
    backdropEl.addEventListener("click", () => closeModal());
  }

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && modalEl && !modalEl.hidden) {
      closeModal();
    }
  });

  msgEl.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
      formEl.requestSubmit();
    }
  });

  // Boot
  // Only check health when modal is opened to avoid noise on every page load.
})();
