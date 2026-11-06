import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

export type ThemeName = 'indigo' | 'ocean' | 'forest' | 'rose' | 'amber';
type ThemeContextValue = { theme: ThemeName; setTheme: (theme: ThemeName) => void };
const ThemeContext = createContext<ThemeContextValue | null>(null);
const STORAGE_KEY = 'kala-ui-theme';
const themes: ThemeName[] = ['indigo', 'ocean', 'forest', 'rose', 'amber'];

function readTheme(): ThemeName {
  try {
    const saved = localStorage.getItem(STORAGE_KEY) as ThemeName | null;
    return saved && themes.includes(saved) ? saved : 'indigo';
  } catch { return 'indigo'; }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<ThemeName>(readTheme);
  useEffect(() => {
    document.documentElement.dataset.kalaTheme = theme;
    try { localStorage.setItem(STORAGE_KEY, theme); } catch { /* Theme still applies for this session. */ }
  }, [theme]);
  const value = useMemo(() => ({ theme, setTheme }), [theme]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const value = useContext(ThemeContext);
  if (!value) throw new Error('useTheme must be used inside ThemeProvider');
  return value;
}
