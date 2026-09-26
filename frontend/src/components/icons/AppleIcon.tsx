interface AppleIconProps {
  className?: string;
}

export default function AppleIcon({
  className,
}: AppleIconProps) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="currentColor"
      aria-hidden="true"
    >
      <path d="M17.1 12.7c0-2.3 1.9-3.4 2-3.5-1.1-1.6-2.8-1.8-3.4-1.8-1.4-.1-2.7.8-3.4.8-.7 0-1.7-.8-2.9-.8-1.5 0-2.9.9-3.7 2.2-1.6 2.8-.4 6.9 1.1 9.1.8 1.1 1.7 2.3 2.9 2.2 1.2 0 1.7-.7 3.2-.7 1.5 0 1.9.7 3.2.7 1.3 0 2.1-1.1 2.9-2.2.9-1.3 1.3-2.6 1.3-2.7-.1 0-3.2-1.2-3.2-4.3Z" />

      <path d="M15.3 6.2c.7-.8 1.2-1.9 1.1-3-.9 0-2.1.6-2.8 1.4-.6.7-1.2 1.8-1.1 2.9 1.1.1 2.2-.5 2.8-1.3Z" />
    </svg>
  );
}