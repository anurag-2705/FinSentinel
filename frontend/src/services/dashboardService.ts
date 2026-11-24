import api from "./api";

export interface FraudDashboardActivity {
  day: string;
  transactions: number;
  flagged: number;
}

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

  // ML prediction details
  mlFraudProbability: number | null;
  mlRiskLevel: string | null;
  mlModelVersion: string | null;
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

export interface FraudAnalyticsSummary {
  assessedTransactions: number;
  flaggedTransactions: number;
  heldTransactions: number;
  blockedTransactions: number;
  blockedTransactionAmount: number;
  fraudDetectionRate: number;
}

export interface FraudDashboardTrend {
  day: string;
  assessed: number;
  flagged: number;
  blocked: number;
}

export interface FraudRuleFrequency {
  ruleCode: string;
  ruleName: string;
  triggerCount: number;
}

export interface FraudRiskDistribution {
  riskLevel: string;
  transactionCount: number;
}

export interface FraudAnalystPerformance {
  analystId: string;
  analystEmail: string;
  totalAssigned: number;
  approvedAlerts: number;
  rejectedAlerts: number;
  completedInvestigations: number;
  resolutionRate: number;
}

export const dashboardService = {
  // Fraud Analytics APIs with date-range filtering

  async getAnalyticsSummary(
    period: string = "7D"
  ): Promise<FraudAnalyticsSummary> {
    const response = await api.get<FraudAnalyticsSummary>(
      "/api/v1/fraud/dashboard/analytics/summary",
      { params: { period } }
    );
    return response.data;
  },

  async getDailyFraudTrends(
    period: string = "7D"
  ): Promise<FraudDashboardTrend[]> {
    const response = await api.get<FraudDashboardTrend[]>(
      "/api/v1/fraud/dashboard/analytics/daily-trends",
      { params: { period } }
    );
    return response.data;
  },

  async getTopFraudRules(
    period: string = "7D"
  ): Promise<FraudRuleFrequency[]> {
    const response = await api.get<FraudRuleFrequency[]>(
      "/api/v1/fraud/dashboard/analytics/top-rules",
      { params: { period } }
    );
    return response.data;
  },

  async getRiskDistribution(
    period: string = "7D"
  ): Promise<FraudRiskDistribution[]> {
    const response = await api.get<FraudRiskDistribution[]>(
      "/api/v1/fraud/dashboard/analytics/risk-distribution",
      { params: { period } }
    );
    return response.data;
  },

  async getAnalystPerformance(
    period: string = "7D"
  ): Promise<FraudAnalystPerformance[]> {
    const response = await api.get<FraudAnalystPerformance[]>(
      "/api/v1/fraud/dashboard/analytics/analyst-performance",
      { params: { period } }
    );
    return response.data;
  },

  // Existing Dashboard APIs

  async getSummary(): Promise<FraudDashboardSummary> {
    const response = await api.get<FraudDashboardSummary>(
      "/api/v1/fraud/dashboard/summary"
    );
    return response.data;
  },

  async getActivity(): Promise<FraudDashboardActivity[]> {
    const response = await api.get<FraudDashboardActivity[]>(
      "/api/v1/fraud/dashboard/activity"
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