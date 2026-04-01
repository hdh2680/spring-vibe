(() => {
  const modal = document.getElementById("loginModal");
  const openBtn = document.getElementById("openLogin");

  if (!modal) return;

  const backdrop = modal.querySelector("#loginBackdrop");
  const closeBtn = modal.querySelector("#closeLogin");
  const username = modal.querySelector("#username");
  const togglePw = modal.querySelector("#togglePw");
  const password = modal.querySelector("#password");

  function openModal() {
    modal.hidden = false;
    document.body.style.overflow = "hidden";
    setTimeout(() => username && username.focus(), 0);
  }

  function closeModal() {
    modal.hidden = true;
    document.body.style.overflow = "";
    openBtn && openBtn.focus();
  }

  if (openBtn) openBtn.addEventListener("click", openModal);
  if (closeBtn) closeBtn.addEventListener("click", closeModal);
  if (backdrop) backdrop.addEventListener("click", closeModal);

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && !modal.hidden) closeModal();
  });

  if (togglePw && password) {
    togglePw.addEventListener("click", () => {
      const showing = password.type === "text";
      password.type = showing ? "password" : "text";
      togglePw.textContent = showing ? "보기" : "숨김";
      togglePw.setAttribute("aria-pressed", String(!showing));
    });
  }

  const qs = new URLSearchParams(window.location.search);
  if (qs.has("modal") || qs.has("error")) openModal();
})();
