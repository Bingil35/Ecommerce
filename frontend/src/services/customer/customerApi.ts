import axiosClient from '../api/axiosClient';
import type { UserResponse } from '../auth/authTypes';
interface ApiResponse<T> { success?: boolean; message?: string; data: T }
export interface UpdateCustomerProfileInput { fullName: string; email: string; phone: string | null }
export const customerApi = {
  async updateProfile(input: UpdateCustomerProfileInput): Promise<UserResponse> {
    const response = await axiosClient.put<ApiResponse<UserResponse> | UserResponse>('/customer/profile', input);
    const payload = response.data;
    return payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload as UserResponse;
  },
  async changePassword(input: { currentPassword: string; newPassword: string }): Promise<void> {
    await axiosClient.put<ApiResponse<null>>('/customer/password', input);
  },
  async getProfile(): Promise<UserResponse> {
    const response = await axiosClient.get<ApiResponse<UserResponse> | UserResponse>('/customer/profile');
    const payload = response.data;
    return payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload as UserResponse;
  },
};
