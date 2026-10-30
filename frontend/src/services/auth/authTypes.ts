export interface UserResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
<<<<<<< Updated upstream
  roleDefault: string | null;
=======
  roleDefault: string;
>>>>>>> Stashed changes
  roles?: string[];
  status: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  user: UserResponse;
}