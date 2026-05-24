import { useState, useRef, useEffect } from "react";
import { Link, useLocation } from "react-router";
import { Bell, MessageCircle } from "lucide-react";

const servers = ["한양", "금강", "압록", "두만", "낙동", "대동"];

interface Notification {
  id: string;
  type: 'alert' | 'chat';
  title: string;
  content: string;
  time: string;
}

export function Header() {
  const location = useLocation();
  const [selectedServer, setSelectedServer] = useState("한양");
  const [serverOpen, setServerOpen] = useState(false);
  const [alertOpen, setAlertOpen] = useState(false);
  const [chatOpen, setChatOpen] = useState(false);
  const [selectedNotif, setSelectedNotif] = useState<Notification | null>(null);
  
  const serverRef = useRef<HTMLDivElement>(null);
  const alertRef = useRef<HTMLDivElement>(null);
  const chatRef = useRef<HTMLDivElement>(null);

  const notifications: Notification[] = [
    { id: '1', type: 'alert', title: '가격 알림', content: '5북두 풀지국 가격이 650억으로 하락했습니다.', time: '5분 전' },
    { id: '2', type: 'alert', title: '관심 매물', content: '관심목록에 추가한 아이템이 새로 등록되었습니다.', time: '1시간 전' },
  ];

  const chats: Notification[] = [
    { id: '1', type: 'chat', title: '장사꾼홍길동', content: '안녕하세요, 거래 가능하신가요?', time: '10분 전' },
  ];

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (serverRef.current && !serverRef.current.contains(event.target as Node)) {
        setServerOpen(false);
      }
      if (alertRef.current && !alertRef.current.contains(event.target as Node)) {
        setAlertOpen(false);
      }
      if (chatRef.current && !chatRef.current.contains(event.target as Node)) {
        setChatOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  return (
    <>
      <header className="bg-[#2A2724] border-b-2 border-[#8B6B4A] h-[52px] flex items-center px-6 gap-5 sticky top-0 z-[200]">
        <Link to="/" className="font-serif text-lg font-bold text-[#E8DCCB] tracking-wider whitespace-nowrap flex items-center gap-2 flex-shrink-0">
          <div className="w-[5px] h-[5px] rounded-full bg-[#8B6B4A]"></div>
          거상인
        </Link>

        {/* 서버 토글 */}
        <div className="relative flex-shrink-0" ref={serverRef}>
          <button
            onClick={() => setServerOpen(!serverOpen)}
            className="flex items-center gap-1.5 bg-white/[0.06] border border-white/[0.25] rounded px-2.5 py-1 cursor-pointer"
          >
            <span className="text-[10px] text-[#D8D0C5] tracking-wider mr-0.5">서버</span>
            <span className="text-sm font-medium text-[#E8DCCB] tracking-wide">{selectedServer}</span>
            <span className={`text-[9px] text-[#D8D0C5] ml-0.5 transition-transform ${serverOpen ? 'rotate-180' : ''}`}>▼</span>
          </button>
          {serverOpen && (
            <div className="absolute top-[calc(100%+6px)] left-0 bg-[#1E1C1A] border border-[#8B6B4A] rounded min-w-[140px] shadow-[0_8px_24px_rgba(0,0,0,0.4)] z-[300]">
              {servers.map((server) => (
                <div
                  key={server}
                  onClick={() => {
                    setSelectedServer(server);
                    setServerOpen(false);
                  }}
                  className={`px-3.5 py-2 text-sm cursor-pointer flex items-center justify-between border-b border-white/[0.08] last:border-b-0 transition-colors hover:bg-[#8B6B4A]/15 hover:text-[#E8DCCB] ${
                    selectedServer === server ? 'text-[#8B6B4A] font-medium' : 'text-[#D8D0C5]'
                  }`}
                >
                  {server}
                  {selectedServer === server && <span className="text-[11px]">✓</span>}
                </div>
              ))}
            </div>
          )}
        </div>

        <nav className="flex gap-0 flex-1">
          <Link
            to="/"
            className={`px-4 h-[52px] flex items-center text-sm tracking-wide border-b-2 -mb-0.5 transition-colors ${
              isActive('/') && location.pathname === '/' ? 'text-[#E8DCCB] border-[#8B6B4A]' : 'text-white/60 border-transparent hover:text-[#E8DCCB]'
            }`}
          >
            메인
          </Link>
          <Link
            to="/trade"
            className={`px-4 h-[52px] flex items-center text-sm tracking-wide border-b-2 -mb-0.5 transition-colors ${
              isActive('/trade') ? 'text-[#E8DCCB] border-[#8B6B4A]' : 'text-white/60 border-transparent hover:text-[#E8DCCB]'
            }`}
          >
            거래
          </Link>
          <Link
            to="/deck"
            className={`px-4 h-[52px] flex items-center text-sm tracking-wide border-b-2 -mb-0.5 transition-colors ${
              isActive('/deck') ? 'text-[#E8DCCB] border-[#8B6B4A]' : 'text-white/60 border-transparent hover:text-[#E8DCCB]'
            }`}
          >
            전투 계산기
          </Link>
        </nav>

        <div className="flex items-center gap-2 ml-auto flex-shrink-0">
          {/* 알림 */}
          <div className="relative" ref={alertRef}>
            <button
              onClick={() => setAlertOpen(!alertOpen)}
              className="w-[34px] h-[34px] border border-white/20 bg-transparent rounded flex items-center justify-center text-base relative transition-colors hover:border-[#8B6B4A]"
            >
              <Bell className="w-4 h-4 text-[#E8DCCB]" />
              {notifications.length > 0 && (
                <span className="absolute -top-1 -right-1 bg-[#C24A4A] text-white text-[9px] w-3.5 h-3.5 rounded-full flex items-center justify-center font-bold">
                  {notifications.length}
                </span>
              )}
            </button>
            {alertOpen && (
              <div className="absolute top-[calc(100%+8px)] right-0 bg-[#FAF8F2] border border-[#DDD6CA] rounded w-[320px] shadow-lg z-[300]">
                <div className="p-3 border-b border-[#DDD6CA]">
                  <h3 className="font-serif text-sm font-semibold text-[#3E3A36]">알림</h3>
                </div>
                {notifications.map((notif) => (
                  <div
                    key={notif.id}
                    onClick={() => setSelectedNotif(notif)}
                    className="p-3 border-b border-[#DDD6CA] last:border-b-0 cursor-pointer hover:bg-[#F5F1E8] transition-colors"
                  >
                    <div className="flex justify-between items-start mb-1">
                      <h4 className="text-sm font-medium text-[#3E3A36]">{notif.title}</h4>
                      <span className="text-[10px] text-[#7A7368]">{notif.time}</span>
                    </div>
                    <p className="text-xs text-[#7A7368] line-clamp-2">{notif.content}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 채팅 */}
          <div className="relative" ref={chatRef}>
            <button
              onClick={() => setChatOpen(!chatOpen)}
              className="w-[34px] h-[34px] border border-white/20 bg-transparent rounded flex items-center justify-center text-base relative transition-colors hover:border-[#8B6B4A]"
            >
              <MessageCircle className="w-4 h-4 text-[#E8DCCB]" />
              {chats.length > 0 && (
                <span className="absolute -top-1 -right-1 bg-[#C24A4A] text-white text-[9px] w-3.5 h-3.5 rounded-full flex items-center justify-center font-bold">
                  {chats.length}
                </span>
              )}
            </button>
            {chatOpen && (
              <div className="absolute top-[calc(100%+8px)] right-0 bg-[#FAF8F2] border border-[#DDD6CA] rounded w-[320px] shadow-lg z-[300]">
                <div className="p-3 border-b border-[#DDD6CA]">
                  <h3 className="font-serif text-sm font-semibold text-[#3E3A36]">채팅</h3>
                </div>
                {chats.map((chat) => (
                  <div
                    key={chat.id}
                    onClick={() => setSelectedNotif(chat)}
                    className="p-3 border-b border-[#DDD6CA] last:border-b-0 cursor-pointer hover:bg-[#F5F1E8] transition-colors"
                  >
                    <div className="flex justify-between items-start mb-1">
                      <h4 className="text-sm font-medium text-[#3E3A36]">{chat.title}</h4>
                      <span className="text-[10px] text-[#7A7368]">{chat.time}</span>
                    </div>
                    <p className="text-xs text-[#7A7368] line-clamp-2">{chat.content}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          <Link
            to="/profile"
            className="border border-white/30 bg-transparent text-[#D8D0C5] px-3.5 py-1 rounded text-xs cursor-pointer tracking-wide transition-colors hover:border-[#8B6B4A] hover:text-[#E8DCCB]"
          >
            내정보
          </Link>
        </div>
      </header>

      {/* 알림/채팅 상세 모달 */}
      {selectedNotif && (
        <div
          className="fixed inset-0 bg-black/65 z-[500] flex items-center justify-center"
          onClick={() => setSelectedNotif(null)}
        >
          <div
            className="bg-[#FAF8F2] border border-[#DDD6CA] rounded w-[500px] max-w-[90vw] shadow-[0_24px_60px_rgba(0,0,0,0.3)]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between p-4 border-b border-[#DDD6CA] bg-[#F5F1E8]">
              <h3 className="font-serif text-sm font-semibold text-[#3E3A36]">{selectedNotif.title}</h3>
              <button
                onClick={() => setSelectedNotif(null)}
                className="text-[#7A7368] text-lg hover:text-[#3E3A36] transition-colors px-1.5"
              >
                ✕
              </button>
            </div>
            <div className="p-5">
              <p className="text-sm text-[#3E3A36] mb-3">{selectedNotif.content}</p>
              <p className="text-xs text-[#7A7368]">{selectedNotif.time}</p>
            </div>
            <div className="p-3 border-t border-[#DDD6CA] bg-[#F5F1E8] flex justify-end">
              <button
                onClick={() => setSelectedNotif(null)}
                className="bg-[#8B6B4A] text-[#E8DCCB] px-5 py-1.5 rounded text-xs font-serif tracking-wide hover:bg-[#6A4F35] transition-colors"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
