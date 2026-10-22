interface FacebookIconProps {
  size?: number;
  className?: string;
}

const FacebookIcon = ({
  size = 20,
  className,
}: FacebookIconProps) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="currentColor"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      <path d="M14 8h3V4.5c-.52-.07-2.3-.2-4.38-.2C8.52 4.3 6.2 6.45 6.2 10.4V13H3v4h3.2v7h4.1v-7h3.4l.55-4h-3.95v-2.3c0-1.16.32-1.95 1.95-1.95H14V8Z" />
    </svg>
  );
};

export default FacebookIcon;