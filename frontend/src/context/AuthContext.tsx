import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

import { authStorage } from '../services/auth/authStorage';
import type { UserResponse } from '../services/auth/authTypes';

interface AuthContextType {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (
    accessToken: string,
    user: UserResponse,
    rememberMe: boolean
  ) => void;
  logout: () => void;
}

const AuthContext =
  createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({
  children,
}: AuthProviderProps) {
  const [user, setUser] =
    useState<UserResponse | null>(null);

  const [isLoading, setIsLoading] =
    useState(true);

  useEffect(() => {
    const storedUser = authStorage.getUser();
    const accessToken = authStorage.getAccessToken();

    if (storedUser && accessToken) {
      setUser(storedUser);
    }

    setIsLoading(false);
  }, []);

  const login = (
    accessToken: string,
    user: UserResponse,
    rememberMe: boolean
  ) => {
    authStorage.save(
      accessToken,
      user,
      rememberMe
    );

    setUser(user);
  };

  const logout = () => {
    authStorage.clear();
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: user !== null,
    isLoading,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error(
      'useAuth must be used within an AuthProvider'
    );
  }

  return context;
}