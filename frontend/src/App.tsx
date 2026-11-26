import { useEffect, useState } from 'react'
import Login from './Login'
import CustomerDashboard from './CustomerDashboard'
import FraudAnalytics from './FraudAnalytics'
import CustomerTransactions from './CustomerTransactions'
import Analysts from './Analysts'
import SettingsPage from './Settings'
import AdminUsers from './AdminUsers'

import {
  ShieldCheck,
  LayoutDashboard,
  AlertTriangle,
  ArrowLeftRight,
  Users,
  Settings,
  Search,
  Bell,
  ChevronDown,
  TrendingUp,
  Activity,
  CircleDollarSign,
  Clock,
  CheckCircle2,
  XCircle,
  Eye,
  Download,
  Menu,
  RefreshCw,
} from 'lucide-react'

import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'

import {
  dashboardService,
  type FraudDashboardSummary,
  type FraudDashboardActivity,
  type FraudAlert,
  type FraudAlertPage,
  type FraudAlertReview,
  type MLExplanationResponse,
  type AnalystWorkload,
} from './services/dashboardService'
import './App.css'

function getStoredRoles(): string[] {
  try {
    const roles = JSON.parse(
      localStorage.getItem('finsentinel_user_roles') || '[]'
    )

    return Array.isArray(roles)
      ? roles.filter((role): role is string => typeof role === 'string')
      : []
  } catch {
    return []
  }
}


function App() {
  const [activeNav, setActiveNav] = useState('Dashboard')

  const [isAuthenticated, setIsAuthenticated] = useState(
    Boolean(localStorage.getItem('finsentinel_access_token'))
  )

  const [userRoles, setUserRoles] = useState<string[]>(getStoredRoles)

const [search, setSearch] = useState('')
const [statusFilter, setStatusFilter] = useState('ALL')
const [minimumRiskScore, setMinimumRiskScore] = useState('')
const [maximumRiskScore, setMaximumRiskScore] = useState('')
const [transactionType, setTransactionType] = useState('ALL')
const [alertPageNumber, setAlertPageNumber] = useState(0)

const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const [summary, setSummary] =
    useState<FraudDashboardSummary | null>(null)
const [chartData, setChartData] =
  useState<FraudDashboardActivity[]>([])

  const [dashboardAlerts, setDashboardAlerts] = useState<FraudAlert[]>([])

  const [selectedAlert, setSelectedAlert] =
    useState<FraudAlert | null>(null)

const [mlExplanation, setMlExplanation] =
  useState<MLExplanationResponse | null>(null)

const [explanationLoading, setExplanationLoading] =
  useState(false)

const [explanationError, setExplanationError] =
  useState('')

  const [reviewHistory, setReviewHistory] =
    useState<FraudAlertReview[]>([])

  const [reviewNotes, setReviewNotes] = useState('')

  const [actionLoading, setActionLoading] = useState(false)
  const [actionMessage, setActionMessage] = useState('')

  const [detailLoading, setDetailLoading] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [detailError, setDetailError] = useState('')

  const [alertPage, setAlertPage] =
    useState<FraudAlertPage | null>(null)

  const [analystWorkload, setAnalystWorkload] =
    useState<AnalystWorkload[]>([])

  const [loading, setLoading] = useState(true)
  const [apiError, setApiError] = useState('')

  const [refreshKey, setRefreshKey] = useState(0)

  // Load dashboard information from backend.
  useEffect(() => {
    const isAnalyst =
      userRoles.includes('FRAUD_ANALYST') ||
      userRoles.includes('ADMIN')

    if (!isAuthenticated || !isAnalyst) {
      setLoading(false)
      return
    }

    const loadDashboard = async () => {
      try {
        setLoading(true)
        setApiError('')

       const [summaryData, workloadData, activityData] =
  await Promise.all([
    dashboardService.getSummary(),
    dashboardService.getAnalystWorkload(),
    dashboardService.getActivity(),
  ])

        setSummary(summaryData)
        setAnalystWorkload(workloadData)
setChartData(activityData)
        

      } catch (error) {
        console.error('Dashboard API error:', error)

        setApiError(
          'Unable to load dashboard data. Please check your login.'
        )
      } finally {
        setLoading(false)
      }
    }

    loadDashboard()
  }, [refreshKey, isAuthenticated, userRoles])
  // Load fraud alerts with server-side filtering and pagination.
  useEffect(() => {
    const isAnalyst =
      userRoles.includes('FRAUD_ANALYST') ||
      userRoles.includes('ADMIN')

    if (!isAuthenticated || !isAnalyst) return

    const loadAlerts = async () => {
      try {
        const alertsData = await dashboardService.getAlerts({
          status: statusFilter === 'ALL' ? undefined : statusFilter,
          minimumRiskScore:
            minimumRiskScore === '' ? undefined : Number(minimumRiskScore),
          maximumRiskScore:
            maximumRiskScore === '' ? undefined : Number(maximumRiskScore),
          transactionType:
            transactionType === 'ALL' ? undefined : transactionType,
          page: alertPageNumber,
          size: 10,
          sortBy: 'createdAt',
          direction: 'DESC',
        })

        setAlertPage(alertsData)
        setDashboardAlerts(alertsData.alerts)
      } catch (error) {
        console.error('Unable to load fraud alerts:', error)
      }
    }

    loadAlerts()
  }, [
    isAuthenticated,
    userRoles,
    refreshKey,
    statusFilter,
    minimumRiskScore,
    maximumRiskScore,
    transactionType,
    alertPageNumber,
  ])

  // Fetch complete alert details and review history.
  const handleViewAlert = async (alert: FraudAlert) => {
    setSelectedAlert(alert)
setReviewNotes('')
setReviewHistory([])
setActionMessage('')
setDetailError('')
setMlExplanation(null)
setExplanationError('')
setExplanationLoading(true)

    setDetailLoading(true)
    setHistoryLoading(true)

    try {
      const [alertDetails, history] = await Promise.all([
        dashboardService.getAlert(alert.alertId),
        dashboardService.getReviewHistory(alert.alertId),
      ])

      setSelectedAlert(alertDetails)
      setReviewHistory(history)
try {
  const explanation =
    await dashboardService.getAlertExplanation(alert.alertId)

  setMlExplanation(explanation)
} catch (error) {
  console.error('Unable to load SHAP explanation:', error)
  setExplanationError(
    'Unable to load AI explanation for this transaction.'
  )
} finally {
  setExplanationLoading(false)
}


    } catch (error) {
      console.error('Unable to load alert details:', error)

      setDetailError(
        'Unable to load complete alert details or review history.'
      )
    } finally {
  setDetailLoading(false)
  setHistoryLoading(false)
  setExplanationLoading(false)
}
  }

  // Assign, approve, reject, or confirm a blocked alert.
const handleAlertAction = async (
  action: 'assign' | 'approve' | 'reject' | 'confirm-block'
) => {
  if (!selectedAlert) return

  // Assignment does not require notes.
  // All review actions require investigation notes.
  if (action !== 'assign' && !reviewNotes.trim()) {
    setActionMessage('Please enter review notes first.')
    return
  }

  try {
    setActionLoading(true)
    setActionMessage('')
    setDetailError('')

    if (action === 'assign') {
      await dashboardService.assignAlert(selectedAlert.alertId)

    } else if (action === 'approve') {
      await dashboardService.approveAlert(
        selectedAlert.alertId,
        reviewNotes.trim()
      )

    } else if (action === 'reject') {
      await dashboardService.rejectAlert(
        selectedAlert.alertId,
        reviewNotes.trim()
      )

    } else if (action === 'confirm-block') {
      await dashboardService.confirmBlockedAlert(
        selectedAlert.alertId,
        reviewNotes.trim()
      )
    }

    const successMessages = {
      assign: 'Alert assigned successfully.',
      approve: 'Transaction approved successfully.',
      reject: 'Transaction rejected successfully.',
      'confirm-block': 'Blocked transaction confirmed. Alert resolved successfully.',
    }

    setActionMessage(successMessages[action])

    setReviewNotes('')
    setSelectedAlert(null)
    setReviewHistory([])

    setRefreshKey((current) => current + 1)

  } catch (error) {
    console.error('Alert action failed:', error)

    setActionMessage(
      'Action failed. Check the alert status, assignment, and review notes.'
    )
  } finally {
    setActionLoading(false)
  }
}

  // Search within the currently loaded server-side page.
const filteredAlerts = dashboardAlerts.filter((alert) => {
  return alert.transactionReference
    .toLowerCase()
    .includes(search.toLowerCase())
})
// Export all matching fraud alerts to CSV.
const handleExportAlerts = async () => {
  try {
    if (
      minimumRiskScore !== '' &&
      maximumRiskScore !== '' &&
      Number(minimumRiskScore) > Number(maximumRiskScore)
    ) {
      window.alert('Minimum risk score cannot exceed maximum risk score.')
      return
    }

    const filters = {
      status: statusFilter === 'ALL' ? undefined : statusFilter,
      minimumRiskScore:
        minimumRiskScore === '' ? undefined : Number(minimumRiskScore),
      maximumRiskScore:
        maximumRiskScore === '' ? undefined : Number(maximumRiskScore),
      transactionType:
        transactionType === 'ALL' ? undefined : transactionType,
      size: 100,
      sortBy: 'createdAt',
      direction: 'DESC',
    }

    // Fetch the first page.
    const firstPage = await dashboardService.getAlerts({
      ...filters,
      page: 0,
    })

    let allAlerts = [...firstPage.alerts]

    // Fetch remaining pages.
    for (let page = 1; page < firstPage.totalPages; page++) {
      const nextPage = await dashboardService.getAlerts({
        ...filters,
        page,
      })

      allAlerts.push(...nextPage.alerts)
    }

    // Apply transaction reference search across all fetched alerts.
    const exportAlerts = allAlerts.filter((alert) =>
      alert.transactionReference
        .toLowerCase()
        .includes(search.toLowerCase())
    )

    if (exportAlerts.length === 0) {
      window.alert('No fraud alerts found to export.')
      return
    }

    const headers = [
      'Transaction Reference',
      'Transaction Type',
      'Amount',
      'Currency',
      'Risk Score',
      'Decision',
      'Transaction Status',
      'Alert Status',
      'Assigned Analyst',
      'Assessment Reason',
      'Created At',
      'Updated At',
      'Resolved At',
    ]

    const escapeCSV = (value: unknown) => {
      let text = String(value ?? '')

      // Protect spreadsheet users from CSV formula injection.
      if (/^[=+\-@\t\r]/.test(text)) {
        text = "'" + text
      }

      return `"${text.replace(/"/g, '""')}"`
    }

    const rows = exportAlerts.map((alert) => [
      alert.transactionReference,
      alert.transactionType,
      alert.amount,
      alert.currency,
      alert.riskScore,
      alert.decision,
      alert.transactionStatus,
      alert.alertStatus,
      alert.assignedAnalystEmail ?? 'Unassigned',
      alert.assessmentReason,
      alert.createdAt,
      alert.updatedAt,
      alert.resolvedAt ?? '',
    ])

    const csvContent = [
      headers.map(escapeCSV).join(','),
      ...rows.map((row) => row.map(escapeCSV).join(',')),
    ].join('\r\n')

    const blob = new Blob(
      ['\uFEFF' + csvContent],
      { type: 'text/csv;charset=utf-8;' }
    )

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')

    link.href = url
    link.download = `FinSentinel_Fraud_Report_${new Date()
      .toISOString()
      .slice(0, 10)}.csv`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    URL.revokeObjectURL(url)

    window.alert(`${exportAlerts.length} fraud alerts exported successfully.`)
  } catch (error) {
    console.error('Fraud report export failed:', error)
    window.alert('Unable to export fraud alerts. Please try again.')
  }
}

  const navigation = [
  { label: 'Dashboard', icon: LayoutDashboard },
  { label: 'Fraud Analytics', icon: TrendingUp },
  { label: 'Fraud Alerts', icon: AlertTriangle },
  { label: 'Transactions', icon: ArrowLeftRight },
  { label: 'Analysts', icon: Users },
  ...(userRoles.includes('ADMIN')
    ? [{ label: 'Admin Users', icon: ShieldCheck }]
    : []),
  { label: 'Settings', icon: Settings },
]

  // Logout.
  const handleLogout = () => {
    localStorage.removeItem('finsentinel_access_token')
    localStorage.removeItem('finsentinel_refresh_token')
    localStorage.removeItem('finsentinel_user_roles')
    localStorage.removeItem('finsentinel_user_name')

    setUserRoles([])
    setIsAuthenticated(false)
  }

  // Authentication routing.
  if (!isAuthenticated) {
    return (
      <Login
        onLogin={() => {
          setUserRoles(getStoredRoles())
          setIsAuthenticated(true)
          setRefreshKey((current) => current + 1)
        }}
      />
    )
  }

  // Customer dashboard.
// Analyst and Admin roles take priority over Customer.
if (
  userRoles.includes('CUSTOMER') &&
  !userRoles.includes('FRAUD_ANALYST') &&
  !userRoles.includes('ADMIN')
) {
  return <CustomerDashboard onLogout={handleLogout} />
}

  // Analyst authorization.
  if (
    !userRoles.includes('FRAUD_ANALYST') &&
    !userRoles.includes('ADMIN')
  ) {
    return (
      <div style={{ padding: '40px', textAlign: 'center' }}>
        <h2>Access Denied</h2>

        <p>
          Your account does not have permission to access this dashboard.
        </p>

        <button onClick={handleLogout}>Logout</button>
      </div>
    )
  }

  return (
    <div className="app-container">

      {/* Sidebar */}
      <aside className={`sidebar ${mobileMenuOpen ? 'sidebar-open' : ''}`}>

        <div className="brand">
          <div className="brand-icon">
            <ShieldCheck size={25} />
          </div>

          <div>
            <h2>FinSentinel</h2>
            <span>INTELLIGENT BANKING</span>
          </div>
        </div>

        <div className="workspace-label">WORKSPACE</div>

        <nav className="sidebar-nav">
          {navigation.map((item) => {
            const Icon = item.icon

            return (
              <button
                key={item.label}
                className={`nav-item ${
                  activeNav === item.label ? 'active' : ''
                }`}
                onClick={() => {
                  setActiveNav(item.label)
                  setMobileMenuOpen(false)
                }}
              >
                <Icon size={19} />

                <span>{item.label}</span>

                {item.label === 'Fraud Alerts' && (
                  <span className="nav-badge">
                    {summary?.openAlerts ?? 0}
                  </span>
                )}
              </button>
            )
          })}
        </nav>

        <div className="sidebar-bottom">

          <div className="system-status">
            <span className="online-dot"></span>

            <div>
              <strong>System operational</strong>
              <span>All services running</span>
            </div>
          </div>

          <div className="profile">
            <div className="avatar">FA</div>

            <div className="profile-info">
              <strong>Fraud Analyst</strong>
              <span>Administrator</span>
            </div>

            <ChevronDown size={17} />
          </div>

        </div>
      </aside>

      {/* Main content */}
      <main className="main-content">

        {/* Topbar */}
        <header className="topbar">

          <div className="topbar-left">

            <button
              className="mobile-menu-button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              <Menu size={22} />
            </button>

            <div>
              <div className="breadcrumb">
                Workspace <span>/</span> {activeNav}
              </div>

              <h1>{activeNav}</h1>
            </div>

          </div>

          <div className="topbar-actions">

            <div className="global-search">
              <Search size={17} />

              <input placeholder="Search anything..." />

              <span>&#8984; K</span>
            </div>

            <button className="icon-button notification-button">
              <Bell size={19} />
              <span></span>
            </button>

            <div className="topbar-user">
              <div className="avatar">FA</div>
              <ChevronDown size={16} />
            </div>
<button
  onClick={handleLogout}
  style={{
    padding: '9px 16px',
    borderRadius: '8px',
    border: '1px solid #374151',
    background: '#1f2937',
    color: '#ffffff',
    cursor: 'pointer',
    fontWeight: 600,
  }}
>
  Logout
</button>

          </div>
        </header>

        {/* Dashboard content */}
        <div className="dashboard-content">

          {activeNav === 'Fraud Analytics' ? (
  <FraudAnalytics />
) : activeNav === 'Transactions' ? (
  <CustomerTransactions />
) : activeNav === 'Analysts' ? (
  <Analysts />
) : activeNav === 'Admin Users' ? (
  <AdminUsers />
) : activeNav === 'Settings' ? (
  <SettingsPage />
) : (
            <>

          {apiError && (
            <div role="alert" className="api-error">
              {apiError}
            </div>
          )}

          {actionMessage && (
            <div role="status" className="api-error">
              {actionMessage}
            </div>
          )}

          {/* Analyst workload */}
          {analystWorkload.length > 0 && (
            <div
              className="panel"
              style={{ marginBottom: '20px', padding: '16px' }}
            >
              <h3>Analyst Workload</h3>

              {analystWorkload.map((analyst) => (
                <p key={analyst.analystId}>
                  {analyst.analystEmail} &mdash;{' '}
                  {analyst.totalAssignedAlerts} assigned alerts,{' '}
                  {analyst.openAssignedAlerts} open
                </p>
              ))}
            </div>
          )}

          {/* Selected alert details */}
          {selectedAlert && (
            <div
              className="panel"
              style={{
                padding: '20px',
                marginBottom: '20px',
              }}
            >

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '10px',
                }}
              >
                <h3>Fraud Alert Details</h3>

                <button
                  className="secondary-button"
                  onClick={() => {
                    setSelectedAlert(null)
                    setReviewHistory([])
                    setDetailError('')
                    setActionMessage('')
                  }}
                >
                  Close
                </button>
              </div>

              {detailLoading ? (
                <p>Loading alert details...</p>
              ) : (
                <>

                  <p>
                    <strong>Transaction Reference:</strong>{' '}
                    {selectedAlert.transactionReference}
                  </p>

                  <p>
                    <strong>Transaction ID:</strong>{' '}
                    {selectedAlert.transactionId}
                  </p>

                  <p>
                    <strong>Transaction Type:</strong>{' '}
                    {selectedAlert.transactionType}
                  </p>

                  <p>
                    <strong>Amount:</strong>{' '}
                    {selectedAlert.currency}{' '}
                    {selectedAlert.amount.toLocaleString('en-IN', {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </p>

                  <p>
                    <strong>Risk Score:</strong>{' '}
                    {selectedAlert.riskScore}/100
                  </p>

                  <p>
                    <strong>Decision:</strong>{' '}
                    {selectedAlert.decision}
                  </p>

                  <p>
                    <strong>Transaction Status:</strong>{' '}
                    {selectedAlert.transactionStatus}
                  </p>

                  <p>
                    <strong>Alert Status:</strong>{' '}
                    {selectedAlert.alertStatus}
                  </p>

                  <p>
                    <strong>Reason:</strong>{' '}
                    {selectedAlert.assessmentReason}
                  </p>

                                     <div className="ml-analysis-panel">
  <h4>ML Fraud Analysis</h4>

  <p>
    <strong>Fraud Probability:</strong>{' '}
    {selectedAlert.mlFraudProbability != null
      ? `${(selectedAlert.mlFraudProbability * 100).toFixed(2)}%`
      : 'Not Available'}
  </p>

  <p>
    <strong>ML Risk Level:</strong>{' '}
    {selectedAlert.mlRiskLevel ?? 'Not Available'}
  </p>

  <p>
    <strong>ML Model:</strong>{' '}
    {selectedAlert.mlModelVersion ?? 'Not Available'}
  </p>

  <hr />

  <h4>Explainable AI – SHAP Analysis</h4>

  {explanationLoading && (
    <p>Generating AI explanation...</p>
  )}

  {explanationError && (
    <div role="alert" className="api-error">
      {explanationError}
    </div>
  )}

  {mlExplanation && (
    <>
      <p>
        <strong>Explained Fraud Probability:</strong>{' '}
        {mlExplanation.fraud_percentage.toFixed(2)}%
      </p>

      <p>
        <strong>Explanation Model:</strong>{' '}
        {mlExplanation.model}
      </p>

      <h5>Top Influencing Factors</h5>

      {mlExplanation.top_factors.length > 0 ? (
        mlExplanation.top_factors.map((factor) => (
          <div key={factor.feature} style={{ marginBottom: '12px' }}>
            <strong>{factor.label}</strong>
            <br />
            <small>
              Value: {factor.value.toFixed(2)}
              {' | '}
              SHAP: {factor.shap_value.toFixed(5)}
            </small>
            <br />
            <span>
              {factor.impact === 'INCREASES_RISK'
                ? '↑ Increases fraud risk'
                : '↓ Decreases fraud risk'}
            </span>
          </div>
        ))
      ) : (
        <p>No influencing factors available.</p>
      )}

      <h5>Risk-Increasing Factors</h5>

      {mlExplanation.risk_increasing_factors.length > 0 ? (
        mlExplanation.risk_increasing_factors.map((factor) => (
          <p key={factor.feature}>
            ↑ {factor.label} — SHAP: {factor.shap_value.toFixed(5)}
          </p>
        ))
      ) : (
        <p>No risk-increasing factors identified.</p>
      )}

      <h5>Risk-Decreasing Factors</h5>

      {mlExplanation.risk_decreasing_factors.length > 0 ? (
        mlExplanation.risk_decreasing_factors.map((factor) => (
          <p key={factor.feature}>
            ↓ {factor.label} — SHAP: {factor.shap_value.toFixed(5)}
          </p>
        ))
      ) : (
        <p>No risk-decreasing factors identified.</p>
      )}

      <small>{mlExplanation.disclaimer}</small>
    </>
  )}

  <hr />

  <small>
    Experimental ML prediction. Final transaction decisions
    are based on the existing fraud rules. Some input features
    currently use neutral default values because device and
    location data are not collected.
  </small>
</div>

                  <p>
                    <strong>Assigned Analyst:</strong>{' '}
                    {selectedAlert.assignedAnalystEmail ?? 'Unassigned'}
                  </p>

                  <p>
                    <strong>Created At:</strong>{' '}
                    {new Date(selectedAlert.createdAt).toLocaleString('en-IN')}
                  </p>

                  {selectedAlert.resolvedAt && (
                    <p>
                      <strong>Resolved At:</strong>{' '}
                      {new Date(selectedAlert.resolvedAt).toLocaleString('en-IN')}
                    </p>
                  )}

                </>
              )}

              {detailError && (
                <div role="alert" className="api-error">
                  {detailError}
                </div>
              )}

              <hr
                style={{
                  margin: '20px 0',
                  borderColor: '#334155',
                }}
              />

              {/* Review history */}
              <h4>Analyst Review History</h4>

              {historyLoading ? (
                <p>Loading review history...</p>
              ) : reviewHistory.length === 0 ? (
                <p>No review history available for this alert.</p>
              ) : (
                <div style={{ marginBottom: '20px' }}>

                  {reviewHistory.map((review) => (
                    <div
                      key={review.reviewId}
                      style={{
                        padding: '12px',
                        marginBottom: '10px',
                        border: '1px solid #334155',
                        borderRadius: '8px',
                      }}
                    >
                      <p>
                        <strong>Action:</strong>{' '}
                        {review.action}
                      </p>

                      <p>
                        <strong>Analyst:</strong>{' '}
                        {review.analystEmail}
                      </p>

                      <p>
                        <strong>Notes:</strong>{' '}
                        {review.notes || 'No notes provided'}
                      </p>

                      <p>
                        <strong>Date:</strong>{' '}
                        {new Date(review.createdAt).toLocaleString('en-IN')}
                      </p>
                    </div>
                  ))}

                </div>
              )}

              <hr
                style={{
                  margin: '20px 0',
                  borderColor: '#334155',
                }}
              />

              {/* Analyst actions */}
              <h4>Analyst Review</h4>

              <textarea
                value={reviewNotes}
                onChange={(event) => setReviewNotes(event.target.value)}
                placeholder="Enter your investigation notes..."
                maxLength={2000}
                rows={4}
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: '8px',
                  marginBottom: '12px',
                  boxSizing: 'border-box',
                }}
              />

              <div
                style={{
                  display: 'flex',
                  gap: '10px',
                  flexWrap: 'wrap',
                }}
              >

                <button
                  className="secondary-button"
                  disabled={
                    actionLoading ||
                    detailLoading ||
                    selectedAlert.alertStatus === 'RESOLVED'
                  }
                  onClick={() => handleAlertAction('assign')}
                >
                  {actionLoading ? 'Processing...' : 'Assign to Me'}
                </button>

                <button
                  className="secondary-button"
                  disabled={
                    actionLoading ||
                    detailLoading ||
                    historyLoading ||
                    selectedAlert.decision !== 'HOLD' ||
                    selectedAlert.alertStatus !== 'UNDER_REVIEW'
                  }
                  onClick={() => handleAlertAction('approve')}
                >
                  Approve Transaction
                </button>

                <button
                  className="secondary-button"
                  disabled={
                    actionLoading ||
                    detailLoading ||
                    historyLoading ||
                    selectedAlert.decision !== 'HOLD' ||
                    selectedAlert.alertStatus !== 'UNDER_REVIEW'
                  }
                  onClick={() => handleAlertAction('reject')}
                >
                  Reject Transaction
                </button>
                <button
  className="secondary-button"
  disabled={
    actionLoading ||
    detailLoading ||
    historyLoading ||
    selectedAlert.decision !== 'BLOCK' ||
    selectedAlert.alertStatus !== 'UNDER_REVIEW'
  }
  onClick={() => handleAlertAction('confirm-block')}
>
  Confirm Block
</button>

              </div>

            </div>
          )}

          {/* Welcome section */}
          <section className="welcome-section">

            <div>
              <h2>Fraud Intelligence Overview</h2>

              <p>
                Monitor suspicious transactions and manage fraud operations
                in real time.
              </p>
            </div>

            <button
              className="secondary-button"
              onClick={() => setRefreshKey((current) => current + 1)}
            >
              <RefreshCw size={16} />
              Refresh
            </button>

          </section>

          {/* Statistics */}
          <section className="stats-grid">

            <div className="stat-card">
              <div className="stat-top">
                <span>Total Fraud Alerts</span>

                <div className="stat-icon purple">
                  <ShieldCheck size={20} />
                </div>
              </div>

              <h3>
                {loading ? '...' : summary?.totalAlerts ?? 0}
              </h3>

              <div className="stat-footer positive">
                <TrendingUp size={15} />
                <span>All recorded alerts</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-top">
                <span>Open Alerts</span>

                <div className="stat-icon orange">
                  <AlertTriangle size={20} />
                </div>
              </div>

              <h3>
                {loading ? '...' : summary?.openAlerts ?? 0}
              </h3>

              <div className="stat-footer warning">
                <Clock size={15} />
                <span>Require attention</span>
              </div>
            </div>

                      <div className="stat-card">
              <div className="stat-top">
                <span>Unassigned Alerts</span>

                <div className="stat-icon orange">
                  <AlertTriangle size={20} />
                </div>
              </div>

              <h3>
                {loading ? '...' : summary?.unassignedAlerts ?? 0}
              </h3>

              <div className="stat-footer warning">
                <Clock size={15} />
                <span>Awaiting analyst assignment</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-top">
                <span>High Risk Alerts</span>

                <div className="stat-icon red">
                  <Activity size={20} />
                </div>
              </div>

              <h3>
                {loading ? '...' : summary?.highRiskAlerts ?? 0}
              </h3>

              <div className="stat-footer danger">
                <TrendingUp size={15} />
                <span>Risk score &ge; 70</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-top">
                <span>Pending Amount</span>

                <div className="stat-icon blue">
                  <CircleDollarSign size={20} />
                </div>
              </div>

              <h3>
                {loading
                  ? '...'
                  : `\u20B9${(
                      summary?.totalPendingAmount ?? 0
                    ).toLocaleString('en-IN', {
                      minimumFractionDigits: 2,
                    })}`}
              </h3>

              <div className="stat-footer neutral">
                <Clock size={15} />
                <span>Awaiting review</span>
              </div>
            </div>

          </section>

          {/* Analytics */}
          <section className="analytics-grid">

            <div className="panel chart-panel">

              <div className="panel-header">

                <div>
                  <h3>Transaction Activity</h3>
                  <p>Weekly transaction monitoring overview</p>
                </div>

                <button className="select-button">
                  Last 7 days <ChevronDown size={15} />
                </button>

              </div>

              <div className="chart-legend">
                <span>
                  <i className="legend-blue"></i> Transactions
                </span>

                <span>
                  <i className="legend-purple"></i> Flagged
                </span>
              </div>

              <div className="chart-container">

                <ResponsiveContainer width="100%" height="100%">

                  <AreaChart data={chartData}>

                    <defs>

                      <linearGradient
                        id="transactionGradient"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="0%"
                          stopColor="#3b82f6"
                          stopOpacity={0.25}
                        />

                        <stop
                          offset="100%"
                          stopColor="#3b82f6"
                          stopOpacity={0}
                        />
                      </linearGradient>

                      <linearGradient
                        id="flaggedGradient"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="0%"
                          stopColor="#a78bfa"
                          stopOpacity={0.25}
                        />

                        <stop
                          offset="100%"
                          stopColor="#a78bfa"
                          stopOpacity={0}
                        />
                      </linearGradient>

                    </defs>

                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="#263244"
                      vertical={false}
                    />

                    <XAxis
                      dataKey="day"
                      stroke="#8190a5"
                      tickLine={false}
                      axisLine={false}
                    />

                    <YAxis
                      stroke="#8190a5"
                      tickLine={false}
                      axisLine={false}
                    />

                    <Tooltip
                      contentStyle={{
                        background: '#151e2d',
                        border: '1px solid #334155',
                        borderRadius: '10px',
                        color: '#fff',
                      }}
                    />

                    <Area
                      type="monotone"
                      dataKey="transactions"
                      stroke="#3b82f6"
                      fill="url(#transactionGradient)"
                      strokeWidth={2}
                    />

                    <Area
                      type="monotone"
                      dataKey="flagged"
                      stroke="#a78bfa"
                      fill="url(#flaggedGradient)"
                      strokeWidth={2}
                    />

                  </AreaChart>

                </ResponsiveContainer>

              </div>
            </div>

            {/* Risk distribution */}
            <div className="panel risk-panel">

              <div className="panel-header">

                <div>
                  <h3>Risk Distribution</h3>
                  <p>Fraud alert decisions</p>
                </div>

                <Activity size={19} className="muted-icon" />

              </div>

              <div className="risk-summary">
                <div className="risk-number">5</div>
                <span>Total assessments</span>
              </div>

              <div className="risk-bar">
                <div className="risk-block"></div>
                <div className="risk-hold"></div>
                <div className="risk-allow"></div>
              </div>

              <div className="risk-item">
                <span>
                  <i className="risk-dot red-dot"></i> Blocked
                </span>

                <strong>1 <small>20%</small></strong>
              </div>

              <div className="risk-item">
                <span>
                  <i className="risk-dot orange-dot"></i> Hold
                </span>

                <strong>4 <small>80%</small></strong>
              </div>

              <div className="risk-item">
                <span>
                  <i className="risk-dot green-dot"></i> Allowed
                </span>

                <strong>0 <small>0%</small></strong>
              </div>

              <div className="risk-note">
                <Activity size={16} />
                <span>Risk decisions from recorded alerts</span>
              </div>

            </div>

          </section>

          {/* Fraud alerts table */}
          <section className="panel alerts-panel">

            <div className="panel-header alerts-header">

              <div>
                <h3>Recent Fraud Alerts</h3>
                <p>Review and monitor suspicious transactions</p>
              </div>

              <button
  className="secondary-button"
  onClick={handleExportAlerts}
>
  <Download size={16} />
  Export
</button>

            </div>

            <div className="table-toolbar">

              <div className="table-search">
                <Search size={16} />

                <input
                  placeholder="Search transaction reference..."
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </div>

              <select
  value={statusFilter}
  onChange={(event) => {
    setStatusFilter(event.target.value)
    setAlertPageNumber(0)
  }}
  className="filter-select"
>
  <option value="ALL">All statuses</option>
  <option value="OPEN">Open</option>
  <option value="UNDER_REVIEW">Under Review</option>
  <option value="RESOLVED">Resolved</option>
</select>

<input
  type="number"
  placeholder="Min risk"
  min="0"
  max="100"
  value={minimumRiskScore}
  className="filter-select"
  onChange={(event) => {
    setMinimumRiskScore(event.target.value)
    setAlertPageNumber(0)
  }}
/>

<input
  type="number"
  placeholder="Max risk"
  min="0"
  max="100"
  value={maximumRiskScore}
  className="filter-select"
  onChange={(event) => {
    setMaximumRiskScore(event.target.value)
    setAlertPageNumber(0)
  }}
/>

<select
  value={transactionType}
  onChange={(event) => {
    setTransactionType(event.target.value)
    setAlertPageNumber(0)
  }}
  className="filter-select"
>
  <option value="ALL">All transaction types</option>
  <option value="DEPOSIT">Deposit</option>
  <option value="WITHDRAWAL">Withdrawal</option>
  <option value="TRANSFER">Transfer</option>
  <option value="BENEFICIARY_TRANSFER">Beneficiary Transfer</option>
</select>

            </div>

            <div className="table-wrapper">

              <table>

                <thead>
                  <tr>
                    <th>TRANSACTION REFERENCE</th>
                    <th>TYPE</th>
                    <th>AMOUNT</th>
                    <th>RISK SCORE</th>
                    <th>DECISION</th>
                    <th>STATUS</th>
                    <th>ACTION</th>
                  </tr>
                </thead>

                <tbody>

                  {filteredAlerts.map((alert) => (
                    <tr key={alert.alertId}>

                      <td className="reference-cell">
                        {alert.transactionReference}
                      </td>

                      <td>{alert.transactionType}</td>

                      <td className="amount-cell">
                        {`${alert.currency} ${alert.amount.toLocaleString(
                          'en-IN',
                          {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          }
                        )}`}
                      </td>

                      <td>
                        <div className="risk-score">

                          <div className="risk-progress">
                            <div
                              style={{
                                width: `${alert.riskScore}%`,
                                background:
                                  alert.riskScore >= 70
                                    ? '#ef4444'
                                    : alert.riskScore >= 40
                                      ? '#f59e0b'
                                      : '#3b82f6',
                              }}
                            />
                          </div>

                          <strong>{alert.riskScore}</strong>

                        </div>
                      </td>

                      <td>
                        <span
                          className={`decision-badge ${alert.decision.toLowerCase()}`}
                        >
                          {alert.decision === 'BLOCK' ? (
                            <XCircle size={13} />
                          ) : (
                            <Clock size={13} />
                          )}

                          {alert.decision}
                        </span>
                      </td>

                      <td>
                        <span
                          className={`status-badge ${alert.alertStatus.toLowerCase()}`}
                        >
                          {alert.alertStatus === 'OPEN' ? (
                            <span className="status-dot"></span>
                          ) : (
                            <CheckCircle2 size={13} />
                          )}

                          {alert.alertStatus}
                        </span>
                      </td>

                      <td>
                        <button
                          className="view-button"
                          title="View alert"
                          onClick={() => handleViewAlert(alert)}
                        >
                          <Eye size={17} />
                        </button>
                      </td>

                    </tr>
                  ))}

                  {filteredAlerts.length === 0 && (
                    <tr>
                      <td colSpan={7} className="empty-state">
                        No matching alerts found.
                      </td>
                    </tr>
                  )}

                </tbody>

              </table>

            </div>

            <div className="table-footer">

              <span>
                Showing {filteredAlerts.length} of{' '}
                {alertPage?.totalElements ?? 0} alerts
              </span>

<div className="pagination">
  <button
    disabled={!alertPage || alertPage.first}
    onClick={() =>
      setAlertPageNumber((current) => Math.max(0, current - 1))
    }
  >
    Previous
  </button>

  <span className="page-active">
    Page {alertPage ? alertPage.page + 1 : 1} of{' '}
    {alertPage?.totalPages ?? 0}
  </span>

  <button
    disabled={!alertPage || alertPage.last}
    onClick={() =>
      setAlertPageNumber((current) => current + 1)
    }
  >
    Next
  </button>
</div>

            </div>

          </section>

            </>
          )}

          {/* Footer */}
          <footer className="dashboard-footer">

            <span>
              &copy; 2026 FinSentinel. Intelligent Fraud Detection Platform.
            </span>

            <span className="footer-status">
              <span className="online-dot"></span>
              System operational
            </span>

          </footer>

        </div>
      </main>
    </div>
  )
}

export default App
