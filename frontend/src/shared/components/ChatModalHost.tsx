export default function ChatModalHost() {
  // This markup matches the ids/classes used by Spring's /js/chat.js so the same script can drive the modal.
  // Keep ids stable: chatModal, chatOpen, chatClose, chatBackdrop, chatLog, chatForm, chatMessage, chatSend, chatClear, chatHealth.
  return (
    <div id="chatModal" className="chat-modal" hidden>
      <div id="chatBackdrop" className="chat-backdrop" aria-hidden="true"></div>
      <div className="chat-dialog chat-phone" role="dialog" aria-modal="true" aria-label="Chatbot dialog">
        <div className="chat-dialog-head">
          <div>
            <div className="chat-dialog-title">AI Chat</div>
            <div id="chatHealth" className="chat-health muted">
              Checking Ollama...
            </div>
          </div>
          <div className="chat-dialog-actions">
            <button id="chatClear" className="btn btn-inline" type="button">
              Clear
            </button>
            <button id="chatClose" className="btn btn-inline" type="button">
              Close
            </button>
          </div>
        </div>

        <div id="chatLog" className="chat-log" aria-live="polite"></div>

        <form id="chatForm" className="chat-form">
          <div className="field chat-input">
            <div className="label">Message</div>
            <div className="chat-compose-row">
              <textarea
                id="chatMessage"
                className="input"
                rows={3}
                placeholder="Type a message... (Ctrl+Enter to send)"
                autoComplete="off"
              ></textarea>
              <button id="chatSend" className="btn btn-inline btn-primary" type="submit">
                Send
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}

