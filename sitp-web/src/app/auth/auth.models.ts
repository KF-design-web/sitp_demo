export interface UserResponse {
  id: number;
  email: string;
  role: string;
  accountType: string;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  user: UserResponse;
  token: string;
  expiresAt: string;
}
