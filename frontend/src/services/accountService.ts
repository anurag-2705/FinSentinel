import api from './api'

export type AccountType = 'SAVINGS' | 'CURRENT'

export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED'

export type AccountStatusAction = 'FREEZE' | 'UNFREEZE'

export interface Account {
  id: string
  accountNumber: string
  accountType: AccountType
  currency: string
  status: AccountStatus
  createdAt: string
}

export interface AccountBalance {
  accountId: string
  accountNumber: string
  currency: string
  availableBalance: number
}

export interface AccountStatusHistory {
  id: string
  action: AccountStatusAction
  previousStatus: AccountStatus
  newStatus: AccountStatus
  createdAt: string
}

export interface CreateAccountRequest {
  accountType: AccountType
}

export const accountService = {
  async getMyAccounts(): Promise<Account[]> {
    const response = await api.get<Account[]>('/api/v1/accounts')
    return response.data
  },

  async getBalance(accountId: string): Promise<AccountBalance> {
    const response = await api.get<AccountBalance>(
      `/api/v1/accounts/${accountId}/balance`
    )
    return response.data
  },

  async createAccount(
    request: CreateAccountRequest
  ): Promise<Account> {
    const response = await api.post<Account>(
      '/api/v1/accounts',
      request
    )
    return response.data
  },

  async freezeAccount(accountId: string): Promise<Account> {
    const response = await api.patch<Account>(
      `/api/v1/accounts/${accountId}/freeze`
    )
    return response.data
  },

  async unfreezeAccount(accountId: string): Promise<Account> {
    const response = await api.patch<Account>(
      `/api/v1/accounts/${accountId}/unfreeze`
    )
    return response.data
  },

  async getStatusHistory(
    accountId: string
  ): Promise<AccountStatusHistory[]> {
    const response = await api.get<AccountStatusHistory[]>(
      `/api/v1/accounts/${accountId}/status-history`
    )
    return response.data
  },
}