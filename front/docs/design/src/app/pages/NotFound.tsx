import { Link } from "react-router";

export function NotFound() {
  return (
    <div className="min-h-[calc(100vh-52px)] flex items-center justify-center px-6">
      <div className="text-center">
        <h1 className="font-serif text-6xl font-bold text-[#8B6B4A] mb-4">404</h1>
        <p className="text-xl text-[#3E3A36] mb-2">페이지를 찾을 수 없습니다</p>
        <p className="text-sm text-[#7A7368] mb-8">요청하신 페이지가 존재하지 않거나 이동되었습니다.</p>
        <Link
          to="/"
          className="inline-block bg-[#8B6B4A] text-[#E8DCCB] px-6 py-3 rounded font-serif text-sm tracking-wide hover:bg-[#6A4F35] transition-colors"
        >
          메인으로 돌아가기
        </Link>
      </div>
    </div>
  );
}
