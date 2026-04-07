import { Link, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import type { ReactNode } from "react";

function isActive(pathname: string, href: string) {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

const SPRING_ORIGIN = import.meta.env.DEV ? "http://localhost:8080" : "";
const toSpring = (path: string) => `${SPRING_ORIGIN}${path}`;

type Me = {
  authenticated: boolean;
  username: string | null;
  authorities: string[];
};

type Menu = {
  id: number;
  parentId: number | null;
  menuKey: string | null;
  menuName: string | null;
  path: string | null;
  children: Menu[];
};

function menuLabel(m: Menu) {
  return (m.menuName && m.menuName.trim()) || (m.menuKey && m.menuKey.trim()) || "Menu";
}

function toAppRoutePath(menuPath: string) {
  // Menu paths are stored as absolute server paths (e.g. "/app/report" or "/users/devSearch/list").
  // For SPA navigation, convert "/app/xyz" to "/xyz" (BrowserRouter basename="/app").
  if (menuPath === "/app") return "/";
  if (menuPath.startsWith("/app/")) return menuPath.substring("/app".length);
  return null;
}

function normalizeMenu(m: Menu): Menu {
  const children = Array.isArray(m.children) ? m.children.map(normalizeMenu) : [];
  return { ...m, children };
}

function NavA(props: { href: string | null; className: string; children: ReactNode }) {
  const href = props.href && props.href.trim() ? props.href : null;
  return (
    <a
      className={props.className}
      href={href ?? "#"}
      onClick={href ? undefined : (e) => e.preventDefault()}
    >
      {props.children}
    </a>
  );
}

function MenuLink(props: { menu: Menu; className: string; children: ReactNode; activePathname: string }) {
  const p = props.menu.path ? props.menu.path.trim() : "";
  const appRoute = p ? toAppRoutePath(p) : null;

  if (appRoute) {
    const isOn = isActive(props.activePathname, appRoute);
    return (
      <Link className={`${props.className}${isOn ? " is-active" : ""}`} to={appRoute}>
        {props.children}
      </Link>
    );
  }

  return <NavA className={props.className} href={p ? toSpring(p) : null}>{props.children}</NavA>;
}

/**
 * React header that reuses the same DOM structure + class names as Thymeleaf layout/app.html
 * so we can share Spring CSS (brand.css, layout_app.css).
 */
export default function SvHeader() {
  const loc = useLocation();
  const [me, setMe] = useState<Me>({ authenticated: false, username: null, authorities: [] });
  const [menus, setMenus] = useState<Menu[]>([]);

  useEffect(() => {
    let alive = true;

    const load = async () => {
      try {
        const [meRes, menusRes] = await Promise.all([
          fetch("/api/auth/me", { credentials: "include" }),
          fetch("/api/menus/left", { credentials: "include" })
        ]);

        if (!alive) return;

        if (meRes.ok) {
          const nextMe = (await meRes.json()) as Me;
          setMe(nextMe);
        }

        if (menusRes.ok) {
          const nextMenus = (await menusRes.json()) as Menu[];
          setMenus(Array.isArray(nextMenus) ? nextMenus.map(normalizeMenu) : []);
        }
      } catch {
        if (!alive) return;
        setMe({ authenticated: false, username: null, authorities: [] });
        setMenus([]);
      }
    };

    load();
    return () => {
      alive = false;
    };
  }, []);

  return (
    <header className="sv-header" role="banner">
      <div className="sv-header__inner">
        <a className="sv-brand" href={toSpring("/")}>
          <span className="sv-mark" aria-hidden="true" />
          <span className="sv-brand__t">Spring-vibe</span>
        </a>

        <nav className="sv-nav" aria-label="Menu">
          <ul className="sv-nav__list" role="list">
            {me.authenticated
              ? menus.map((m) => {
                  const label = menuLabel(m);
                  const hasMega = m.children && m.children.length > 0;
                  return (
                    <li key={m.id} className={`sv-nav__item${hasMega ? " sv-nav__item--has-mega" : ""}`}>
                      <MenuLink
                        menu={m}
                        className="sv-nav__link"
                        activePathname={loc.pathname}
                      >
                        <span>{label}</span>
                      </MenuLink>

                      {hasMega ? (
                        <div className="sv-mega" role="region" aria-label={`${label} submenu`}>
                          <div className="sv-mega__panel">
                            <div className="sv-dd" role="list">
                              {m.children.map((c) => {
                                const cLabel = menuLabel(c);
                                const hasGroup = c.children && c.children.length > 0;

                                if (!hasGroup) {
                                  return (
                                    <MenuLink
                                      key={c.id}
                                      menu={c}
                                      className="sv-dd__a"
                                      activePathname={loc.pathname}
                                    >
                                      <span>{cLabel}</span>
                                    </MenuLink>
                                  );
                                }

                                return (
                                  <div key={c.id} className="sv-dd__group" role="listitem">
                                    <div className="sv-dd__head">{cLabel}</div>
                                    {c.children.map((g) => (
                                      <MenuLink
                                        key={g.id}
                                        menu={g}
                                        className="sv-dd__a sv-dd__a--sub"
                                        activePathname={loc.pathname}
                                      >
                                        <span>{menuLabel(g)}</span>
                                      </MenuLink>
                                    ))}
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        </div>
                      ) : null}
                    </li>
                  );
                })
              : null}
          </ul>
        </nav>

        {me.authenticated && menus.length === 0 ? (
          <div className="sv-nav-empty">No menus assigned</div>
        ) : null}

        <div className="sv-auth" aria-label="Auth actions">
          {!me.authenticated ? (
            <button className="sv-btn sv-btn--ghost" type="button" onClick={() => window.location.assign(toSpring("/login"))}>
              Log in
            </button>
          ) : (
            <>
              <button
                id="chatOpen"
                className="sv-btn sv-btn--pill sv-btn--icon"
                type="button"
                aria-haspopup="dialog"
                aria-controls="chatModal"
              >
                <svg className="sv-btn__icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                  <path d="M10 4h4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  <path d="M12 4v2" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  <rect x="5" y="7" width="14" height="12" rx="4" fill="none" stroke="currentColor" strokeWidth="2" />
                  <path d="M9 19v2M15 19v2" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  <path d="M8 12h.01M16 12h.01" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
                  <path d="M9.5 15.5h5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
                <span>AI Chat</span>
              </button>

              <form action={toSpring("/logout")} method="post">
                <button className="sv-btn sv-btn--ghost" type="submit">
                  Logout
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
