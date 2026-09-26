import type { UserResponse } from './authTypes';

const ACCESS_TOKEN_KEY = 'accessToken';
const USER_KEY = 'authUser';

export const authStorage = {
  getAccessToken(): string | null {
    return (
      localStorage.getItem(ACCESS_TOKEN_KEY) ||
      sessionStorage.getItem(ACCESS_TOKEN_KEY)
    );
  },

  getUser(): UserResponse | null {
    const user =
      localStorage.getItem(USER_KEY) ||
      sessionStorage.getItem(USER_KEY);

    if (!user) {
      return null;
    }

    try {
      return JSON.parse(user) as UserResponse;
    } catch {
      return null;
    }
  },

  save(
    accessToken: string,
    user: UserResponse,
    rememberMe: boolean
  ): void {
    this.clear();

    const storage = rememberMe
      ? localStorage
      : sessionStorage;

    storage.setItem(ACCESS_TOKEN_KEY, accessToken);
    storage.setItem(USER_KEY, JSON.stringify(user));
  },

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);

    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  },
};