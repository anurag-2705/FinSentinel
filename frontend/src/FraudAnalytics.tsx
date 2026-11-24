import { useEffect, useState } from 'react'

import {
  ResponsiveContainer,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
} from 'recharts'

import {
  ShieldCheck,
  AlertTriangle,
  Ban,
  IndianRupee,
  Activity,
} from 'lucide-react'

import {
  dashboardService,
  type FraudAnalyticsSummary,
  type FraudDashboardTrend,
  type FraudRuleFrequency,
  type FraudRiskDistribution,
  type FraudAnalystPerformance,
} from './services/dashboardService'

import './FraudAnalytics.css'

type AnalyticsPeriod = '7D' | '30D' | '90D' | 'ALL'

function FraudAnalytics() {
  const [summary, setSummary] =
    useState<FraudAnalyticsSummary | null>(null)

  const [trends, setTrends] =
    useState<FraudDashboardTrend[]>([])

  const [rules, setRules] =
    useState<FraudRuleFrequency[]>([])

  const [riskDistribution, setRiskDistribution] =
    useState<FraudRiskDistribution[]>([])

  const [analystPerformance, setAnalystPerformance] =
    useState<FraudAnalystPerformance[]>([])

  const [period, setPeriod] = useState<AnalyticsPeriod>('7D')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    const loadAnalytics = async () => {
      try {
        setLoading(true)
        setError('')

        const [
          summaryData,
          trendData,
          ruleData,
          riskData,
          analystData,
        ] = await Promise.all([
          dashboardService.getAnalyticsSummary(period),
          dashboardService.getDailyFraudTrends(period),
          dashboardService.getTopFraudRules(period),
          dashboardService.getRiskDistribution(period),
          dashboardService.getAnalystPerformance(period),
        ])

        if (cancelled) return

        setSummary(summaryData)
        setTrends(trendData)
        setRules(ruleData)
        setRiskDistribution(riskData)
        setAnalystPerformance(analystData)
      } catch (err) {
        if (cancelled) return

        console.error('Fraud analytics loading failed:', err)
        setError('Unable to load fraud analytics. Please refresh and try again.')
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadAnalytics()

    return () => {
      cancelled = true
    }
  }, [period])

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(amount)

  const cards = summary
    ? [
        {
          title: 'Assessed Transactions',
          value: summary.assessedTransactions.toLocaleString('en-IN'),
          icon: Activity,
          className: 'analytics-blue',
        },
        {
          title: 'Flagged Transactions',
          value: summary.flaggedTransactions.toLocaleString('en-IN'),
          icon: AlertTriangle,
          className: 'analytics-orange',
        },
        {
          title: 'Blocked Transactions',
          value: summary.blockedTransactions.toLocaleString('en-IN'),
          icon: Ban,
          className: 'analytics-red',
        },
        {
          title: 'Blocked Amount',
          value: formatCurrency(summary.blockedTransactionAmount),
          icon: IndianRupee,
          className: 'analytics-purple',
        },
      ]
    : []

  const riskColors: Record<string, string> = {
    LOW: '#22c55e',
    MEDIUM: '#f59e0b',
    HIGH: '#ef4444',
  }

  return (
    <section className="fraud-analytics">

      {/* Heading and date filter */}
      <div className="analytics-heading">
        <div>
          <h2>Fraud Analytics</h2>
          <p>
            Real-time insights from FinSentinel transaction and fraud records.
          </p>
        </div>

        <div className="analytics-heading-actions">
          <div className="analytics-period-filter">
            <label htmlFor="analytics-period">
              Date Range
            </label>

            <select
              id="analytics-period"
              value={period}
              onChange={(event) =>
                setPeriod(event.target.value as AnalyticsPeriod)
              }
            >
              <option value="7D">Last 7 Days</option>
              <option value="30D">Last 30 Days</option>
              <option value="90D">Last 90 Days</option>
              <option value="ALL">All Time</option>
            </select>
          </div>

          {summary && (
            <div className="detection-rate">
              <ShieldCheck size={22} />
              <div>
                <span>Fraud Detection Rate</span>
                <strong>{summary.fraudDetectionRate.toFixed(2)}%</strong>
              </div>
            </div>
          )}
        </div>
      </div>

      {loading && (
        <div className="analytics-message">
          Loading {period === 'ALL' ? 'all-time' : period} fraud analytics...
        </div>
      )}

      {!loading && error && (
        <div className="analytics-message analytics-error">
          {error}
        </div>
      )}

      {!loading && !error && summary && (
        <>

          {/* Summary cards */}
          <div className="analytics-cards">
            {cards.map((card) => {
              const Icon = card.icon

              return (
                <div className="analytics-card" key={card.title}>
                  <div className={`analytics-icon ${card.className}`}>
                    <Icon size={22} />
                  </div>

                  <div>
                    <p>{card.title}</p>
                    <h3>{card.value}</h3>
                  </div>
                </div>
              )
            })}
          </div>

          {/* Additional transaction metrics */}
          <div className="analytics-secondary-stats">
            <div>
              <span>Held Transactions</span>
              <strong>{summary.heldTransactions}</strong>
            </div>

            <div>
              <span>Blocked Transactions</span>
              <strong>{summary.blockedTransactions}</strong>
            </div>

            <div>
              <span>Flagged Transactions</span>
              <strong>{summary.flaggedTransactions}</strong>
            </div>
          </div>

          {/* Daily trends */}
          <div className="analytics-panel">
            <h3>Daily Fraud Trends</h3>
            <p>
              {period === 'ALL'
                ? 'Daily fraud assessment trends over the last 90 days.'
                : `Assessment, flagged and blocked transactions over the last ${period === '7D' ? '7' : period === '30D' ? '30' : '90'} days.`}
            </p>

            <div className="analytics-chart">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trends}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="day" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Legend />

                  <Line
                    type="monotone"
                    dataKey="assessed"
                    name="Assessed"
                    stroke="#3b82f6"
                    strokeWidth={3}
                  />

                  <Line
                    type="monotone"
                    dataKey="flagged"
                    name="Flagged"
                    stroke="#f59e0b"
                    strokeWidth={3}
                  />

                  <Line
                    type="monotone"
                    dataKey="blocked"
                    name="Blocked"
                    stroke="#ef4444"
                    strokeWidth={3}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Risk distribution and fraud rules */}
          <div className="analytics-grid">

            <div className="analytics-panel">
              <h3>Transaction Risk Distribution</h3>
              <p>Transactions grouped by fraud risk score.</p>

              <div className="analytics-chart">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={riskDistribution}
                      dataKey="transactionCount"
                      nameKey="riskLevel"
                      cx="50%"
                      cy="50%"
                      outerRadius={105}
                      label
                    >
                      {riskDistribution.map((item) => (
                        <Cell
                          key={item.riskLevel}
                          fill={riskColors[item.riskLevel] ?? '#64748b'}
                        />
                      ))}
                    </Pie>

                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="analytics-panel">
              <h3>Most Triggered Fraud Rules</h3>
              <p>Rules ranked by their actual trigger frequency.</p>

              <div className="analytics-chart">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={rules.slice(0, 7)} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" allowDecimals={false} />
                    <YAxis
                      type="category"
                      dataKey="ruleCode"
                      width={130}
                      tick={{ fontSize: 11 }}
                    />
                    <Tooltip />
                    <Bar
                      dataKey="triggerCount"
                      name="Triggers"
                      fill="#6366f1"
                      radius={[0, 5, 5, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

          </div>

          {/* Analyst performance */}
          <div className="analytics-panel">
            <h3>Analyst Performance</h3>
            <p>Investigation and resolution metrics for fraud analysts.</p>

            <div className="analytics-table-wrapper">
              <table className="analytics-table">
                <thead>
                  <tr>
                    <th>Analyst</th>
                    <th>Assigned</th>
                    <th>Approved</th>
                    <th>Rejected</th>
                    <th>Completed</th>
                    <th>Resolution Rate</th>
                  </tr>
                </thead>

                <tbody>
                  {analystPerformance.map((analyst) => (
                    <tr key={analyst.analystId}>
                      <td>{analyst.analystEmail}</td>
                      <td>{analyst.totalAssigned}</td>
                      <td>{analyst.approvedAlerts}</td>
                      <td>{analyst.rejectedAlerts}</td>
                      <td>{analyst.completedInvestigations}</td>
                      <td>{analyst.resolutionRate.toFixed(2)}%</td>
                    </tr>
                  ))}

                  {analystPerformance.length === 0 && (
                    <tr>
                      <td colSpan={6} className="analytics-empty">
                        No analyst performance records available.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

        </>
      )}

    </section>
  )
}

export default FraudAnalytics