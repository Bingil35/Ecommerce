import { useState } from 'react';
import { Maximize2, MessageCircle, Minimize2, X } from 'lucide-react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import ChatPage from '../../../pages/chat/ChatPage';

type ChatWidgetProps = { role?: 'customer' | 'shop-owner' };

export default function ChatWidget({ role = 'customer' }: ChatWidgetProps) {
  const [open, setOpen] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const reduceMotion = useReducedMotion();

  return <AnimatePresence initial={false} mode="wait">
    {open ? (
      <motion.section
        key="chat-panel"
        layout
        initial={{ opacity: 0, scale: reduceMotion ? 1 : 0.94, y: reduceMotion ? 0 : 18 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: reduceMotion ? 1 : 0.96, y: reduceMotion ? 0 : 12 }}
        transition={{
          layout: { duration: reduceMotion ? 0 : 0.42, ease: [0.22, 1, 0.36, 1] },
          opacity: { duration: reduceMotion ? 0 : 0.18 },
          scale: { duration: reduceMotion ? 0 : 0.24, ease: [0.22, 1, 0.36, 1] },
          y: { duration: reduceMotion ? 0 : 0.24, ease: [0.22, 1, 0.36, 1] },
        }}
        className={`fixed bottom-5 right-5 z-[80] flex flex-col overflow-hidden rounded-2xl border border-[#E1E7F2] bg-white shadow-[0_18px_60px_rgba(31,45,82,.22)] ${expanded ? 'h-[min(760px,calc(100dvh-40px))] w-[min(880px,calc(100vw-40px))]' : 'h-[min(600px,calc(100dvh-88px))] w-[min(380px,calc(100vw-24px))]'}`}
      >
        <header className="flex h-12 shrink-0 items-center gap-2 border-b border-[#EDF0F6] bg-white px-4"><span className="grid h-8 w-8 place-items-center rounded-lg bg-[#EEF3FF] text-[#5367C9]"><MessageCircle size={17}/></span><b className="min-w-0 flex-1 truncate text-sm text-[#202642]">{role === 'shop-owner' ? 'Tin nhắn khách hàng' : 'Tin nhắn Kala'}</b><button type="button" onClick={() => setExpanded(value => !value)} aria-label={expanded ? 'Thu nhỏ khung chat' : 'Mở rộng khung chat'} title={expanded ? 'Thu nhỏ' : 'Mở rộng'} className="hidden h-8 w-8 place-items-center rounded-lg text-slate-500 hover:bg-slate-100 sm:grid">{expanded ? <Minimize2 size={16}/> : <Maximize2 size={16}/>}</button><button type="button" onClick={() => { setOpen(false); setExpanded(false); }} aria-label="Đóng khung chat" className="grid h-8 w-8 place-items-center rounded-lg text-slate-500 hover:bg-slate-100"><X size={17}/></button></header>
        <div className="min-h-0 flex-1"><ChatPage role={role} presentation="widget"/></div>
      </motion.section>
    ) : (
      <motion.button
        key="chat-launcher"
        type="button"
        aria-label="Mở khung chat"
        title={role === 'shop-owner' ? 'Chat với khách hàng' : 'Chat với cửa hàng'}
        onClick={() => setOpen(true)}
        initial={{ opacity: 0, scale: reduceMotion ? 1 : 0.75, y: reduceMotion ? 0 : 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: reduceMotion ? 1 : 0.75, y: reduceMotion ? 0 : 10 }}
        transition={{ duration: reduceMotion ? 0 : 0.2, ease: [0.22, 1, 0.36, 1] }}
        className="fixed bottom-5 right-5 z-[80] grid h-14 w-14 place-items-center rounded-full bg-[#5367C9] text-white shadow-[0_8px_28px_rgba(57,75,155,.35)] transition-colors hover:bg-[#4559B7] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#5367C9]/25"
      >
        <MessageCircle size={23} strokeWidth={2}/>
      </motion.button>
    )}
  </AnimatePresence>;
}
