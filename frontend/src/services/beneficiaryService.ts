import api from './api'

export type BeneficiaryStatus = 'ACTIVE' | 'BLOCKED'

export interface Beneficiary {
  id: string
  beneficiaryName: string
  nickname: string | null
  bankName: string
  accountNumber: string
  ifscCode: string
  status: BeneficiaryStatus
  createdAt: string
}

export interface AddBeneficiaryRequest {
  beneficiaryName: string
  nickname?: string
  bankName: string
  accountNumber: string
  ifscCode: string
}

export const beneficiaryService = {
  async getBeneficiaries(): Promise<Beneficiary[]> {
    const response = await api.get<Beneficiary[]>(
      '/api/v1/beneficiaries'
    )

    return response.data
  },

  async addBeneficiary(
    request: AddBeneficiaryRequest
  ): Promise<Beneficiary> {
    const response = await api.post<Beneficiary>(
      '/api/v1/beneficiaries',
      request
    )

    return response.data
  },

  async blockBeneficiary(id: string): Promise<Beneficiary> {
    const response = await api.patch<Beneficiary>(
      `/api/v1/beneficiaries/${id}/block`
    )

    return response.data
  },

  async unblockBeneficiary(id: string): Promise<Beneficiary> {
    const response = await api.patch<Beneficiary>(
      `/api/v1/beneficiaries/${id}/unblock`
    )

    return response.data
  },

  async deleteBeneficiary(id: string): Promise<void> {
    await api.delete(`/api/v1/beneficiaries/${id}`)
  },
}