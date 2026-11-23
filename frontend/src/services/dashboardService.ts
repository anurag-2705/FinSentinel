import api from "./api";

export interface FraudDashboardSummary {
  totalAlerts: number;
  openAlerts: number;
  underReviewAlerts: number;
  resolvedAlerts: number;
  dismissedAlerts: number;
  highRiskAlerts: number;
  pendingTransactions: number;
  totalPendingAmount: number;
}

export interface AnalystWorkload {
  analystId: string;
  analystEmail: string;
  totalAssignedAlerts: number;
  openAssignedAlerts: number;
  underReviewAlerts: number;
  resolvedAlerts: number;
}

export interface FraudAlert {
  alertId: string;
  assessmentId: string;
  transactionId: string;
  transactionReference: string;
  transactionType: string;
  amount: number;
  currency: string;
  transactionStatus: string;
  riskScore: number;
  decision: string;
  assessmentReason: string;
  alertStatus: string;
  assignedAnalystId: string | null;
  assignedAnalystEmail: string | null;
  analystNotes: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
}

export interface FraudAlertReview {
  reviewId: string;
  action: string;
  analystId: string;
  analystEmail: string;
  notes: string | null;
  createdAt: string;
}

export interface FraudAlertPage {
  alerts: FraudAlert[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AlertFilters {
  status?: string;
  minimumRiskScore?: number;
  maximumRiskScore?: number;
  transactionType?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: string;
}

export const dashboardService = {
  async getSummary(): Promise<FraudDashboardSummary> {
    const response = await api.get<FraudDashboardSummary>(
      "/api/v1/fraud/dashboard/summary"
    );
    return response.data;
  },

  async getAnalystWorkload(): Promise<AnalystWorkload[]> {
    const response = await api.get<AnalystWorkload[]>(
      "/api/v1/fraud/dashboard/analyst-workload"
    );
    return response.data;
  },

  async getAlerts(filters: AlertFilters = {}): Promise<FraudAlertPage> {
    const response = await api.get<FraudAlertPage>(
      "/api/v1/fraud/dashboard/alerts",
      { params: filters }
    );
    return response.data;
  },

  async getAlert(alertId: string): Promise<FraudAlert> {
    const response = await api.get<FraudAlert>(
      `/api/v1/fraud/alerts/${alertId}`
    );
    return response.data;
  },

  async getReviewHistory(alertId: string): Promise<FraudAlertReview[]> {
    const response = await api.get<FraudAlertReview[]>(
      `/api/v1/fraud/alerts/${alertId}/reviews`
    );
    return response.data;
  },

  async assignAlert(alertId: string): Promise<FraudAlert> {
    const response = await api.post<FraudAlert>(
      `/api/v1/fraud/alerts/${alertId}/assign`
    );
    return response.data;
  },

  async approveAlert(
    alertId: string,
    notes: string
  ): Promise<FraudAlert> {
    const response = await api.post<FraudAlert>(
      `/api/v1/fraud/alerts/${alertId}/approve`,
      { notes }
    );
    return response.data;
  },

  async rejectAlert(
    alertId: string,
    notes: string
  ): Promise<FraudAlert> {
    const response = await api.post<FraudAlert>(
      `/api/v1/fraud/alerts/${alertId}/reject`,
      { notes }
    );
    return response.data;
  },
};