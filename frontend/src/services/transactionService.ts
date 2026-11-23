import api from './api'

export type TransactionType =
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'TRANSFER'
  | 'BENEFICIARY_TRANSFER'

export type TransactionStatus =
  | 'PENDING'
  | 'COMPLETED'
  | 'FAILED'

export interface Transaction {
  id: string
  referenceNumber: string
  transactionType: TransactionType
  amount: number
  currency: string
  status: TransactionStatus
  sourceAccountId: string | null
  destinationAccountId: string | null
  description: string | null
  createdAt: string
  completedAt: string | null
}

export interface DepositRequest {
  accountId: string
  amount: number
  description?: string
}

export interface WithdrawalRequest {
  accountId: string
  amount: number
  description?: string
}

export interface TransferRequest {
  sourceAccountId: string
  destinationAccountId: string
  amount: number
  description?: string
}

export interface BeneficiaryTransferRequest {
  sourceAccountId: string
  beneficiaryId: string
  amount: number
  description?: string
}

// Generate a unique key for every transaction request.
const generateIdempotencyKey = (): string => {
  return crypto.randomUUID()
}

export const transactionService = {

  async deposit(request: DepositRequest): Promise<Transaction> {
    const response = await api.post<Transaction>(
      '/api/v1/transactions/deposit',
      request,
      {
        headers: {
          'Idempotency-Key': generateIdempotencyKey(),
        },
      }
    )

    return response.data
  },

  async withdraw(request: WithdrawalRequest): Promise<Transaction> {
    const response = await api.post<Transaction>(
      '/api/v1/transactions/withdraw',
      request,
      {
        headers: {
          'Idempotency-Key': generateIdempotencyKey(),
        },
      }
    )

    return response.data
  },

  async transfer(request: TransferRequest): Promise<Transaction> {
    const response = await api.post<Transaction>(
      '/api/v1/transactions/transfer',
      request,
      {
        headers: {
          'Idempotency-Key': generateIdempotencyKey(),
        },
      }
    )

    return response.data
  },

  async beneficiaryTransfer(
    request: BeneficiaryTransferRequest
  ): Promise<Transaction> {
    const response = await api.post<Transaction>(
      '/api/v1/transactions/beneficiary-transfer',
      request,
      {
        headers: {
          'Idempotency-Key': generateIdempotencyKey(),
        },
      }
    )

    return response.data
  },

  async getTransactionHistory(): Promise<Transaction[]> {
    const response = await api.get<Transaction[]>(
      '/api/v1/transactions'
    )

    return response.data
  },
}