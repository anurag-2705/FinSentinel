import { useEffect, useState } from 'react'
import Login from './Login'
import CustomerDashboard from './CustomerDashboard'

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
  type FraudAlert,
  type FraudAlertPage,
  type FraudAlertReview,
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

const chartData = [
  { day: 'Mon', transactions: 120, flagged: 8 },
  { day: 'Tue', transactions: 180, flagged: 12 },
  { day: 'Wed', transactions: 150, flagged: 10 },
  { day: 'Thu', transactions: 240, flagged: 18 },
  { day: 'Fri', transactions: 200, flagged: 14 },
  { day: 'Sat', transactions: 300, flagged: 25 },
  { day: 'Sun', transactions: 260, flagged: 20 },
]

function App() {
  const [activeNav, setActiveNav] = useState('Dashboard')

  const [isAuthenticated, setIsAuthenticated] = useState(
    Boolean(localStorage.getItem('finsentinel_access_token'))
  )

  const [userRoles, setUserRoles] = useState<string[]>(getStoredRoles)

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const [summary, setSummary] =
    useState<FraudDashboardSummary | null>(null)

  const [dashboardAlerts, setDashboardAlerts] = useState<FraudAlert[]>([])

  const [selectedAlert, setSelectedAlert] =
    useState<FraudAlert | null>(null)

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

        const [summaryData, workloadData, alertsData] =
          await Promise.all([
            dashboardService.getSummary(),

            dashboardService.getAnalystWorkload(),

            dashboardService.getAlerts({
              page: 0,
              size: 10,
              sortBy: 'createdAt',
              direction: 'DESC',
            }),
          ])

        setSummary(summaryData)
        setAnalystWorkload(workloadData)
        setAlertPage(alertsData)
        setDashboardAlerts(alertsData.alerts)

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

  // Fetch complete alert details and review history.
  const handleViewAlert = async (alert: FraudAlert) => {
    setSelectedAlert(alert)
    setReviewNotes('')
    setReviewHistory([])
    setActionMessage('')
    setDetailError('')

    setDetailLoading(true)
    setHistoryLoading(true)

    try {
      const [alertDetails, history] = await Promise.all([
        dashboardService.getAlert(alert.alertId),
        dashboardService.getReviewHistory(alert.alertId),
      ])

      setSelectedAlert(alertDetails)
      setReviewHistory(history)

    } catch (error) {
      console.error('Unable to load alert details:', error)

      setDetailError(
        'Unable to load complete alert details or review history.'
      )
    } finally {
      setDetailLoading(false)
      setHistoryLoading(false)
    }
  }

  // Assign, approve, or reject an alert.
  const handleAlertAction = async (
    action: 'assign' | 'approve' | 'reject'
  ) => {
    if (!selectedAlert) return

    // Assignment does not require notes.
    // Approval and rejection require investigation notes.
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

      } else {
        await dashboardService.rejectAlert(
          selectedAlert.alertId,
          reviewNotes.trim()
        )
      }

      const successMessages = {
        assign: 'Alert assigned successfully.',
        approve: 'Transaction approved successfully.',
        reject: 'Transaction rejected successfully.',
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

  // Filter alerts.
  const filteredAlerts = dashboardAlerts.filter((alert) => {
    const matchesSearch = alert.transactionReference
      .toLowerCase()
      .includes(search.toLowerCase())

    const matchesStatus =
      statusFilter === 'ALL' || alert.alertStatus === statusFilter

    return matchesSearch && matchesStatus
  })

  const navigation = [
    { label: 'Dashboard', icon: LayoutDashboard },
    { label: 'Fraud Alerts', icon: AlertTriangle },
    { label: 'Transactions', icon: ArrowLeftRight },
    { label: 'Analysts', icon: Users },
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

              <span>⌘ K</span>
            </div>

            <button className="icon-button notification-button">
              <Bell size={19} />
              <span></span>
            </button>

            <div className="topbar-user">
              <div className="avatar">FA</div>
              <ChevronDown size={16} />
            </div>

          </div>
        </header>

        {/* Dashboard content */}
        <div className="dashboard-content">

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
                  {analyst.analystEmail} —{' '}
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
                <span>Risk score ≥ 70</span>
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
                  : `₹${(
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

              <button className="secondary-button">
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
                onChange={(event) => setStatusFilter(event.target.value)}
                className="filter-select"
              >
                <option value="ALL">All statuses</option>
                <option value="OPEN">Open</option>
                <option value="UNDER_REVIEW">Under Review</option>
                <option value="RESOLVED">Resolved</option>
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
                <button disabled>Previous</button>
                <button className="page-active">1</button>
                <button disabled>Next</button>
              </div>

            </div>

          </section>

          {/* Footer */}
          <footer className="dashboard-footer">

            <span>
              © 2026 FinSentinel. Intelligent Fraud Detection Platform.
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