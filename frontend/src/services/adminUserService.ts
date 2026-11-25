import api from "./api";

export interface AdminUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string | null;
  status: string;
  emailVerified: boolean;
  phoneVerified: boolean;
  failedLoginAttempts: number;
  lockedUntil: string | null;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
  roles: string[];
}

const ADMIN_USERS_URL = "/api/v1/admin/users";

export const getAdminUsers = async (): Promise<AdminUser[]> => {
  const response = await api.get<AdminUser[]>(ADMIN_USERS_URL);
  return response.data;
};

export const getAdminUser = async (id: string): Promise<AdminUser> => {
  const response = await api.get<AdminUser>(`${ADMIN_USERS_URL}/${id}`);
  return response.data;
};

export const assignUserRole = async (
  id: string,
  roleName: string
): Promise<AdminUser> => {
  const response = await api.put<AdminUser>(
    `${ADMIN_USERS_URL}/${id}/roles`,
    null,
    { params: { roleName } }
  );

  return response.data;
};

export const removeUserRole = async (
  id: string,
  roleName: string
): Promise<AdminUser> => {
  const response = await api.delete<AdminUser>(
    `${ADMIN_USERS_URL}/${id}/roles/${encodeURIComponent(roleName)}`
  );

  return response.data;
};

export const suspendUser = async (id: string): Promise<AdminUser> => {
  const response = await api.patch<AdminUser>(
    `${ADMIN_USERS_URL}/${id}/suspend`
  );

  return response.data;
};

export const reactivateUser = async (id: string): Promise<AdminUser> => {
  const response = await api.patch<AdminUser>(
    `${ADMIN_USERS_URL}/${id}/reactivate`
  );

  return response.data;
};
