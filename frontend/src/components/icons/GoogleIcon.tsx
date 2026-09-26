interface GoogleIconProps {
  className?: string;
}

export default function GoogleIcon({
  className,
}: GoogleIconProps) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <path
        fill="#4285F4"
        d="M21.8 12.2c0-.7-.1-1.5-.2-2.2H12v4.2h5.5a4.7 4.7 0 0 1-2 3.1v2.6h3.3c1.9-1.8 3-4.4 3-7.7Z"
      />

      <path
        fill="#34A853"
        d="M12 22c2.7 0 5-.9 6.7-2.5l-3.3-2.6c-.9.6-2 .9-3.4.9-2.6 0-4.8-1.8-5.6-4.2H3v2.7A10.1 10.1 0 0 0 12 22Z"
      />

      <path
        fill="#FBBC05"
        d="M6.4 13.6A6 6 0 0 1 6.1 12c0-.6.1-1.1.3-1.6V7.7H3A10 10 0 0 0 2 12c0 1.6.4 3 1 4.3l3.4-2.7Z"
      />

      <path
        fill="#EA4335"
        d="M12 6.2c1.5 0 2.8.5 3.8 1.5l2.9-2.9C17 3.2 14.7 2 12 2A10.1 10.1 0 0 0 3 7.7l3.4 2.7c.8-2.4 3-4.2 5.6-4.2Z"
      />
    </svg>
  );
}