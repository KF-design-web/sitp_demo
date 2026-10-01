export interface UserResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string | null;
  address: string | null;
  country: string | null;
  gender: 'MALE' | 'FEMALE' | null;
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

export interface RegisterPayload {
  email: string;
  username: string;
  password: string;
  passwordConfirm: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  address?: string;
  country?: string;
  gender?: 'MALE' | 'FEMALE' | null;
  captchaId: string;
  captchaAnswer: string;
}

export interface LoginPayload {
  username: string;
  password: string;
  captchaId: string;
  captchaAnswer: string;
}

export interface CaptchaChallenge {
  id: number;
  question: string;
}
