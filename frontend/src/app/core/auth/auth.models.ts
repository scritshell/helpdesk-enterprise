export type RoleName = 'ADMIN' | 'TECHNICIAN' | 'USER';

export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: RoleName[];
}

export interface LoginResponse {
  token: string;
  tokenType: 'Bearer';
  user: UserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}
