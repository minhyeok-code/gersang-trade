'use client';

import './globals.css';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState, useRef } from 'react';
import { api, clearToken, getToken, getTokenRole, setServer, getServer, type ServerDto, type NotificationDto, type ChatRoomSummaryDto } from '@/lib/api';
import { Bell, MessageCircle, ShieldCheck, User, ChevronDown } from 'lucide-react';
import NotificationPopover from '@/components/notifications/NotificationPopover';
import ChatRoomPopover from '@/components/chat/ChatRoomPopover';
import ChatPanel from '@/components/chat/ChatPanel';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  const [servers, setServers] = useState<ServerDto[]>([]);
  const [selectedServer, setSelectedServerState] = useState<string | null>(null);
  const [serverDropOpen, setServerDropOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [chatOpen, setChatOpen] = useState(false);
  const [activeChatRoom, setActiveChatRoom] = useState<ChatRoomSummaryDto | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const serverRef = useRef<HTMLDivElement>(null);
  const notificationRef = useRef<HTMLDivElement>(null);
  const chatRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setIsLoggedIn(!!getToken());
    setIsAdmin(getTokenRole() === 'ADMIN');
    const saved = getServer();
    if (saved) setSelectedServerState(saved);
  }, []);

  useEffect(() => {
    api.getServers()
      .then((list) => {
        setServers(list);
        const saved = getServer();
        if (!saved && list.length > 0) {
          setSelectedServerState(String(list[0].id));
          setServer(String(list[0].id));
        }
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!isLoggedIn) return;
    api.getNotifications()
      .then((list: NotificationDto[]) => setUnreadCount(list.filter((n) => !n.isRead).length))
      .catch(() => {});
  }, [isLoggedIn]);

  // 바깥 클릭 시 드롭다운 닫기
  useEffect(() => {
    function close(e: MouseEvent) {
      if (serverRef.current && !serverRef.current.contains(e.target as Node)) {
        setServerDropOpen(false);
      }
      if (notificationRef.current && !notificationRef.current.contains(e.target as Node)) {
        setNotificationOpen(false);
      }
      if (chatRef.current && !chatRef.current.contains(e.target as Node)) {
        setChatOpen(false);
      }
    }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  function handleServerSelect(serverId: string) {
    setSelectedServerState(serverId);
    setServer(serverId);
    setServerDropOpen(false);
    if (isLoggedIn) {
      api.updateMyServer(Number(serverId)).catch(() => {});
    }
  }

  function handleLogout() {
    api.logout().catch(() => {});
    clearToken();
    setIsLoggedIn(false);
    setIsAdmin(false);
    router.push('/');
  }

  const selectedServerName = servers.find((s) => String(s.id) === selectedServer)?.name ?? '서버 선택';

  return (
    <html lang="ko">
      <body style={{ background: 'var(--bg)', color: 'var(--text)', minHeight: '100vh' }}>
        {/* 헤더 */}
        <header
          style={{ background: 'var(--header-bg)', borderBottom: '2px solid var(--brown)', height: '76px' }}
          className="sticky top-0 z-[200]"
        >
          <div className="max-w-[1280px] mx-auto px-6 h-full flex items-center gap-6">
          {/* 브랜드 + 서버 선택 */}
          <div className="flex items-center gap-4">
            <Link href="/" style={{ color: 'var(--beige)' }} className="font-serif text-2xl font-bold whitespace-nowrap">
              거상인
            </Link>

            {/* 서버 드롭다운 */}
            <div className="relative" ref={serverRef}>
              <button
                onClick={() => setServerDropOpen(!serverDropOpen)}
                style={{ color: selectedServer ? 'var(--brown)' : 'var(--text-muted)', borderColor: 'var(--brown)' }}
                className="flex items-center gap-1.5 border rounded px-3 py-1 text-sm font-medium"
              >
                {selectedServerName}
                <ChevronDown style={{ width: 14, height: 14 }} />
              </button>
              {serverDropOpen && (
                <div
                  style={{ background: '#1E1C1A', border: '1px solid var(--brown)', zIndex: 300 }}
                  className="absolute top-full left-0 mt-1 rounded shadow-xl min-w-[130px]"
                >
                  {servers.map((s) => (
                    <button
                      key={s.id}
                      onClick={() => handleServerSelect(String(s.id))}
                      style={{
                        color: String(s.id) === selectedServer ? 'var(--brown)' : 'var(--text-disabled)',
                        fontWeight: String(s.id) === selectedServer ? 600 : 400,
                      }}
                      className="block w-full text-left px-3 py-2 text-sm hover:bg-white/5"
                    >
                      {s.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="flex-1" />

          {/* 우측 아이콘 */}
          <div className="flex items-center gap-2">
            <div className="relative" ref={notificationRef}>
            <button
              type="button"
              onClick={() => setNotificationOpen((open) => !open)}
              style={{ color: notificationOpen ? 'var(--brown)' : 'var(--text-disabled)' }}
              className="relative p-2.5 hover:text-[var(--beige)] transition-colors"
              title="알림"
            >
              <Bell style={{ width: 22, height: 22 }} />
              {unreadCount > 0 && (
                <span
                  style={{ background: 'var(--danger)', top: 5, right: 5 }}
                  className="absolute text-white text-[10px] leading-none w-4 h-4 rounded-full flex items-center justify-center"
                >
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </button>
            {notificationOpen && isLoggedIn && (
              <NotificationPopover onUnreadChange={setUnreadCount} />
            )}
            </div>
            <div className="relative" ref={chatRef}>
            <button
              type="button"
              onClick={() => setChatOpen((open) => !open)}
              style={{ color: chatOpen ? 'var(--brown)' : 'var(--text-disabled)' }}
              className="p-2.5 hover:text-[var(--beige)] transition-colors"
              title="채팅"
            >
              <MessageCircle style={{ width: 22, height: 22 }} />
            </button>
            {chatOpen && isLoggedIn && (
              <ChatRoomPopover
                onOpenRoom={(room) => {
                  setActiveChatRoom(room);
                  setChatOpen(false);
                }}
              />
            )}
            </div>
            {isAdmin && (
              <Link
                href="/admin"
                style={{ color: pathname === '/admin' ? 'var(--brown)' : 'var(--text-disabled)' }}
                className="p-2.5 hover:text-[var(--beige)] transition-colors"
                title="관리자 페이지"
              >
                <ShieldCheck style={{ width: 22, height: 22 }} />
              </Link>
            )}
            <Link
              href="/profile"
              style={{ color: pathname === '/profile' ? 'var(--brown)' : 'var(--text-disabled)' }}
              className="p-2.5 hover:text-[var(--beige)] transition-colors"
              title="내 프로필"
            >
              <User style={{ width: 22, height: 22 }} />
            </Link>

            <span style={{ background: 'var(--border)', width: 1, height: 24, margin: '0 6px' }} />

            {isLoggedIn ? (
              <button
                onClick={handleLogout}
                style={{ color: 'var(--text-muted)' }}
                className="text-sm px-3 py-1.5 hover:text-red-400 transition-colors"
              >
                로그아웃
              </button>
            ) : (
              <Link
                href="/login"
                style={{ color: 'var(--brown)' }}
                className="text-sm px-3 py-1.5 hover:text-[var(--beige)] transition-colors font-medium"
              >
                로그인
              </Link>
            )}
          </div>
          </div>
        </header>

        <main className="flex-1">
          <div className="max-w-[1280px] mx-auto px-6">
            {children}
          </div>
        </main>
        {activeChatRoom && (
          <ChatPanel room={activeChatRoom} onClose={() => setActiveChatRoom(null)} />
        )}
      </body>
    </html>
  );
}
