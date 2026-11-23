import { useEffect, useState } from 'react'
import {
  ShieldCheck,
  Wallet,
  Plus,
  RefreshCw,
  Lock,
  Unlock,
  Clock,
  X,
  LogOut,
  CreditCard,
  ArrowUpRight,
} from 'lucide-react'

import {
  accountService,
  type Account,
  type AccountBalance,
  type AccountStatusHistory,
  type AccountType,
} from './services/accountService'

import './CustomerDashboard.css'

interface CustomerDashboardProps {
  onLogout: () => void
}

function CustomerDashboard({ onLogout }: CustomerDashboardProps) {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [balances, setBalances] = useState<Record<string, AccountBalance | null>>({})
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [accountType, setAccountType] = useState<AccountType>('SAVINGS')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [history, setHistory] = useState<AccountStatusHistory[]>([])
  const [historyAccount, setHistoryAccount] = useState<Account | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [processingAccount, setProcessingAccount] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const loadAccounts = async () => {
      try {
        setLoading(true)
        setError('')

        const accountData = await accountService.getMyAccounts()
        

        if (cancelled) return

        setAccounts(accountData)

        const balanceResults = await Promise.all(
          accountData.map(async (account) => {
            try {
              const balance = await accountService.getBalance(account.id)
              return [account.id, balance] as const
            } catch {
              return [account.id, null] as const
            }
          })
        )

        if (cancelled) return

        setBalances(Object.fromEntries(balanceResults))
      } catch (err) {
        console.error('Account loading failed:', err)

        if (!cancelled) {
          setError('Unable to load your accounts. Please try again.')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadAccounts()

    return () => {
      cancelled = true
    }
  }, [refreshKey])

  const totalBalance = Object.values(balances).reduce(
    (total, balance) => total + (balance?.availableBalance ?? 0),
    0
  )

  const formatCurrency = (amount: number, currency = 'INR') =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
    }).format(amount)

  const handleCreateAccount = async () => {
    try {
      setCreating(true)
      setError('')
      setSuccess('')

      await accountService.createAccount({ accountType })

      setShowCreateForm(false)
      setSuccess(`${accountType} account created successfully.`)
      setRefreshKey((current) => current + 1)
    } catch (err) {
      console.error('Account creation failed:', err)
      setError('Unable to create account. Please try again.')
    } finally {
      setCreating(false)
    }
  }

  const handleAccountStatus = async (account: Account) => {
    try {
      setProcessingAccount(account.id)
      setError('')
      setSuccess('')

      if (account.status === 'ACTIVE') {
        await accountService.freezeAccount(account.id)
        setSuccess('Account frozen successfully.')
      } else if (account.status === 'FROZEN') {
        await accountService.unfreezeAccount(account.id)
        setSuccess('Account unfrozen successfully.')
      }

      setRefreshKey((current) => current + 1)
    } catch (err) {
      console.error('Account status update failed:', err)
      setError('Unable to update account status.')
    } finally {
      setProcessingAccount(null)
    }
  }

  const handleViewHistory = async (account: Account) => {
    try {
      setError('')
      setHistoryAccount(account)

      const historyData = await accountService.getStatusHistory(account.id)
      setHistory(historyData)
    } catch (err) {
      console.error('History loading failed:', err)
      setError('Unable to load account history.')
      setHistoryAccount(null)
    }
  }

  return (
    <div className="customer-dashboard">
      <header className="customer-header">
        <div className="customer-brand">
          <div className="customer-brand-icon">
            <ShieldCheck size={25} />
          </div>

          <div>
            <h2>FinSentinel</h2>
            <span>SECURE DIGITAL BANKING</span>
          </div>
        </div>

        <div className="customer-header-actions">
          <span className="customer-role">CUSTOMER</span>

          <button className="customer-logout" onClick={onLogout}>
            <LogOut size={17} />
            Logout
          </button>
        </div>
      </header>

      <main className="customer-main">
        <section className="customer-welcome">
          <div>
            <span className="customer-eyebrow">YOUR BANKING OVERVIEW</span>
            <h1>Welcome to FinSentinel</h1>
            <p>Manage your accounts and monitor your available balance.</p>
          </div>

          <button
            className="customer-refresh"
            onClick={() => setRefreshKey((current) => current + 1)}
            disabled={loading}
          >
            <RefreshCw size={17} />
            Refresh
          </button>
        </section>

        {error && (
          <div className="customer-message error-message" role="alert">
            {error}
            <button onClick={() => setError('')} aria-label="Dismiss error">
              <X size={16} />
            </button>
          </div>
        )}

        {success && (
          <div className="customer-message success-message" role="status">
            {success}
            <button onClick={() => setSuccess('')} aria-label="Dismiss message">
              <X size={16} />
            </button>
          </div>
        )}

        <section className="customer-overview-grid">
          <div className="customer-overview-card balance-overview">
            <div className="overview-card-top">
              <div className="overview-icon">
                <Wallet size={22} />
              </div>
              <span>AVAILABLE BALANCE</span>
            </div>

            <h2>
              {loading ? 'Loading...' : formatCurrency(totalBalance)}
            </h2>

            <p>Combined balance across your accounts</p>
          </div>

          <div className="customer-overview-card">
            <div className="overview-card-top">
              <div className="overview-icon purple-icon">
                <CreditCard size={22} />
              </div>
              <span>TOTAL ACCOUNTS</span>
            </div>

            <h2>{loading ? '...' : accounts.length}</h2>
            <p>Your registered bank accounts</p>
          </div>

          <div className="customer-overview-card">
            <div className="overview-card-top">
              <div className="overview-icon green-icon">
                <ShieldCheck size={22} />
              </div>
              <span>ACTIVE ACCOUNTS</span>
            </div>

            <h2>
              {loading
                ? '...'
                : accounts.filter((account) => account.status === 'ACTIVE').length}
            </h2>

            <p>Accounts currently available for transactions</p>
          </div>
        </section>

        <section className="customer-accounts-section">
          <div className="customer-section-heading">
            <div>
              <h2>My Accounts</h2>
              <p>View and manage your banking accounts.</p>
            </div>

            <button
              className="create-account-button"
              onClick={() => setShowCreateForm(!showCreateForm)}
            >
              <Plus size={18} />
              Open New Account
            </button>
          </div>

          {showCreateForm && (
            <div className="create-account-form">
              <div>
                <h3>Open a Bank Account</h3>
                <p>Select the type of account you want to create.</p>
              </div>

              <select
                value={accountType}
                onChange={(event) =>
                  setAccountType(event.target.value as AccountType)
                }
              >
                <option value="SAVINGS">Savings Account</option>
                <option value="CURRENT">Current Account</option>
              </select>

              <button
                className="confirm-create-button"
                onClick={handleCreateAccount}
                disabled={creating}
              >
                {creating ? 'Creating...' : 'Create Account'}
              </button>

              <button
                className="cancel-create-button"
                onClick={() => setShowCreateForm(false)}
              >
                Cancel
              </button>
            </div>
          )}

          {loading ? (
            <div className="customer-empty-state">
              <RefreshCw className="loading-icon" size={25} />
              <p>Loading your accounts...</p>
            </div>
          ) : accounts.length === 0 ? (
            <div className="customer-empty-state">
              <Wallet size={38} />
              <h3>No bank accounts yet</h3>
              <p>Open your first account to start using FinSentinel.</p>

              <button
                className="create-account-button"
                onClick={() => setShowCreateForm(true)}
              >
                <Plus size={17} />
                Open Your First Account
              </button>
            </div>
          ) : (
            <div className="customer-account-grid">
              {accounts.map((account) => (
                <article className="customer-account-card" key={account.id}>
                  <div className="account-card-top">
                    <div className="account-type-icon">
                      <CreditCard size={22} />
                    </div>

                    <span className={`account-status ${account.status.toLowerCase()}`}>
                      {account.status}
                    </span>
                  </div>

                  <span className="account-type-label">
                    {account.accountType} ACCOUNT
                  </span>

                  <h3>{formatCurrency(
                    balances[account.id]?.availableBalance ?? 0,
                    account.currency
                  )}</h3>

                  <p className="account-number">
                    Account Number: {account.accountNumber}
                  </p>

                  {balances[account.id] === null && (
                    <p className="balance-unavailable">
                      Balance currently unavailable
                    </p>
                  )}

                  <div className="account-card-actions">
                    {account.status !== 'CLOSED' && (
                      <button
                        className="account-action-button"
                        onClick={() => handleAccountStatus(account)}
                        disabled={processingAccount === account.id}
                      >
                        {account.status === 'ACTIVE' ? (
                          <Lock size={15} />
                        ) : (
                          <Unlock size={15} />
                        )}

                        {processingAccount === account.id
                          ? 'Please wait...'
                          : account.status === 'ACTIVE'
                            ? 'Freeze'
                            : 'Unfreeze'}
                      </button>
                    )}

                    <button
                      className="account-history-button"
                      onClick={() => handleViewHistory(account)}
                    >
                      <Clock size={15} />
                      History
                    </button>
                  </div>

                  <div className="account-card-footer">
                    <span>Account details</span>
                    <ArrowUpRight size={15} />
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <footer className="customer-footer">
          <span>© 2026 FinSentinel. Secure Digital Banking.</span>
          <span>
            <span className="customer-online-dot"></span>
            Banking services connected
          </span>
        </footer>
      </main>

      {historyAccount && (
        <div className="history-modal-overlay">
          <div className="history-modal">
            <div className="history-modal-header">
              <div>
                <h2>Account Status History</h2>
                <p>{historyAccount.accountNumber}</p>
              </div>

              <button
                onClick={() => {
                  setHistoryAccount(null)
                  setHistory([])
                }}
                aria-label="Close history"
              >
                <X size={20} />
              </button>
            </div>

            {history.length === 0 ? (
              <div className="history-empty">
                <Clock size={28} />
                <p>No status changes recorded for this account.</p>
              </div>
            ) : (
              <div className="history-list">
                {history.map((item) => (
                  <div className="history-item" key={item.id}>
                    <div className="history-item-icon">
                      {item.action === 'FREEZE' ? (
                        <Lock size={17} />
                      ) : (
                        <Unlock size={17} />
                      )}
                    </div>

                    <div>
                      <strong>{item.action}</strong>
                      <p>
                        {item.previousStatus} → {item.newStatus}
                      </p>
                      <span>
                        {new Date(item.createdAt).toLocaleString('en-IN')}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default CustomerDashboard