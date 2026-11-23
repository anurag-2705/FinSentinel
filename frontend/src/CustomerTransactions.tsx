import { useEffect, useState, type FormEvent } from 'react'
import {
  ArrowDownLeft,
  ArrowUpRight,
  ArrowLeftRight,
  History,
  Plus,
  RefreshCw,
  ShieldCheck,
  Users,
} from 'lucide-react'

import {
  accountService,
  type Account,
  type AccountBalance,
} from './services/accountService'

import {
  transactionService,
  type Transaction,
} from './services/transactionService'

import {
  beneficiaryService,
  type Beneficiary,
} from './services/beneficiaryService'

import './CustomerTransactions.css'

type Operation =
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'TRANSFER'
  | 'BENEFICIARY_TRANSFER'

function getErrorMessage(error: unknown): string {
  const err = error as {
    response?: {
      data?: {
        message?: string
        error?: string
      }
    }
    message?: string
  }

  return (
    err.response?.data?.message ||
    err.response?.data?.error ||
    err.message ||
    'Something went wrong. Please try again.'
  )
}

function CustomerTransactions() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [balances, setBalances] = useState<Record<string, AccountBalance>>({})
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])

  const [operation, setOperation] = useState<Operation>('DEPOSIT')

  const [accountId, setAccountId] = useState('')
  const [destinationAccountId, setDestinationAccountId] = useState('')
  const [beneficiaryId, setBeneficiaryId] = useState('')

  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')

  const [beneficiaryName, setBeneficiaryName] = useState('')
  const [nickname, setNickname] = useState('')
  const [bankName, setBankName] = useState('')
  const [beneficiaryAccountNumber, setBeneficiaryAccountNumber] = useState('')
  const [ifscCode, setIfscCode] = useState('')

  const [loading, setLoading] = useState(true)
  const [processing, setProcessing] = useState(false)
  const [addingBeneficiary, setAddingBeneficiary] = useState(false)
  const [showBeneficiaryForm, setShowBeneficiaryForm] = useState(false)

  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const activeAccounts = accounts.filter(
    (account) => account.status === 'ACTIVE'
  )

  const activeBeneficiaries = beneficiaries.filter(
    (beneficiary) => beneficiary.status === 'ACTIVE'
  )

  const formatCurrency = (value: number, currency = 'INR') =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
    }).format(value)

  const loadData = async () => {
    try {
      setLoading(true)
      setError('')

      const [accountData, beneficiaryData, transactionData] =
        await Promise.all([
          accountService.getMyAccounts(),
          beneficiaryService.getBeneficiaries(),
          transactionService.getTransactionHistory(),
        ])

      setAccounts(accountData)
      setBeneficiaries(beneficiaryData)
      setTransactions(transactionData)

      const balanceResults = await Promise.all(
        accountData.map(async (account) => {
          try {
            const balance = await accountService.getBalance(account.id)
            return [account.id, balance] as const
          } catch {
            return null
          }
        })
      )

      const validBalances = balanceResults.filter(
        (item): item is NonNullable<typeof item> => item !== null
      )

      setBalances(Object.fromEntries(validBalances))

      if (!accountId && accountData.length > 0) {
        const activeAccount = accountData.find(
          (account) => account.status === 'ACTIVE'
        )

        if (activeAccount) {
          setAccountId(activeAccount.id)
        }
      }
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
    // Load customer banking data when the component mounts.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleTransaction = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    setError('')
    setSuccess('')

    const numericAmount = Number(amount)

    if (!accountId) {
      setError('Please select an active account.')
      return
    }

    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setError('Enter a valid amount greater than zero.')
      return
    }

    try {
      setProcessing(true)

      let result: Transaction

      if (operation === 'DEPOSIT') {
        result = await transactionService.deposit({
          accountId,
          amount: numericAmount,
          description: description || 'Simulated customer deposit',
        })
      } else if (operation === 'WITHDRAWAL') {
        result = await transactionService.withdraw({
          accountId,
          amount: numericAmount,
          description,
        })
      } else if (operation === 'TRANSFER') {
        if (!destinationAccountId) {
          setError('Select a destination account.')
          return
        }

        if (accountId === destinationAccountId) {
          setError('Source and destination accounts must be different.')
          return
        }

        result = await transactionService.transfer({
          sourceAccountId: accountId,
          destinationAccountId,
          amount: numericAmount,
          description,
        })
      } else {
        if (!beneficiaryId) {
          setError('Please select a beneficiary.')
          return
        }

        result = await transactionService.beneficiaryTransfer({
          sourceAccountId: accountId,
          beneficiaryId,
          amount: numericAmount,
          description,
        })
      }

      if (result.status === 'COMPLETED') {
        setSuccess(
          `Transaction completed successfully. Reference: ${result.referenceNumber}`
        )
      } else if (result.status === 'PENDING') {
        setSuccess(
          `Transaction submitted for review. Reference: ${result.referenceNumber}`
        )
      } else {
        setError(
          `Transaction was declined. Reference: ${result.referenceNumber}`
        )
      }

      setAmount('')
      setDescription('')

      await loadData()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setProcessing(false)
    }
  }

  const handleAddBeneficiary = async (
    event: FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault()

    setError('')
    setSuccess('')

    try {
      setAddingBeneficiary(true)

      await beneficiaryService.addBeneficiary({
        beneficiaryName,
        nickname: nickname || undefined,
        bankName,
        accountNumber: beneficiaryAccountNumber,
        ifscCode: ifscCode.toUpperCase(),
      })

      setSuccess('Beneficiary added successfully.')

      setBeneficiaryName('')
      setNickname('')
      setBankName('')
      setBeneficiaryAccountNumber('')
      setIfscCode('')
      setShowBeneficiaryForm(false)

      await loadData()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setAddingBeneficiary(false)
    }
  }

  const operationOptions = [
    { value: 'DEPOSIT', label: 'Deposit', icon: ArrowDownLeft },
    { value: 'WITHDRAWAL', label: 'Withdraw', icon: ArrowUpRight },
    { value: 'TRANSFER', label: 'Own Transfer', icon: ArrowLeftRight },
    {
      value: 'BENEFICIARY_TRANSFER',
      label: 'Beneficiary',
      icon: Users,
    },
  ] as const

  return (
    <section className="customer-transactions">

      <div className="ct-heading">
        <div>
          <span className="ct-eyebrow">BANKING OPERATIONS</span>
          <h2>Transactions</h2>
          <p>Transfer money and manage your banking activity.</p>
        </div>

        <button
          className="ct-refresh"
          onClick={loadData}
          disabled={loading}
        >
          <RefreshCw size={16} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="ct-message ct-error" role="alert">
          {error}
        </div>
      )}

      {success && (
        <div className="ct-message ct-success" role="status">
          {success}
        </div>
      )}

      <div className="ct-operation-layout">

        <div className="ct-operation-card">

          <h3>Choose Operation</h3>

          <div className="ct-operation-tabs">
            {operationOptions.map((item) => {
              const Icon = item.icon

              return (
                <button
                  key={item.value}
                  type="button"
                  className={
                    operation === item.value ? 'ct-tab active' : 'ct-tab'
                  }
                  onClick={() => {
                    setOperation(item.value)
                    setError('')
                    setSuccess('')
                  }}
                >
                  <Icon size={18} />
                  {item.label}
                </button>
              )
            })}
          </div>

          <form onSubmit={handleTransaction} className="ct-form">

            <div className="ct-form-title">
              <ShieldCheck size={21} />
              <div>
                <h3>
                  {operation === 'DEPOSIT' && 'Simulated Deposit'}
                  {operation === 'WITHDRAWAL' && 'Withdraw Money'}
                  {operation === 'TRANSFER' && 'Transfer Between Accounts'}
                  {operation === 'BENEFICIARY_TRANSFER' &&
                    'Transfer to Beneficiary'}
                </h3>

                <p>Complete the transaction details below.</p>
              </div>
            </div>

            <label>
              Source Account
              <select
                value={accountId}
                onChange={(event) => setAccountId(event.target.value)}
                required
              >
                <option value="">Select account</option>

                {activeAccounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.accountType} - {account.accountNumber}
                    {' | '}
                    {formatCurrency(
                      balances[account.id]?.availableBalance ?? 0,
                      account.currency
                    )}
                  </option>
                ))}
              </select>
            </label>

            {operation === 'TRANSFER' && (
              <label>
                Destination Account
                <select
                  value={destinationAccountId}
                  onChange={(event) =>
                    setDestinationAccountId(event.target.value)
                  }
                  required
                >
                  <option value="">Select destination account</option>

                  {activeAccounts
                    .filter((account) => account.id !== accountId)
                    .map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.accountType} - {account.accountNumber}
                      </option>
                    ))}
                </select>
              </label>
            )}

            {operation === 'BENEFICIARY_TRANSFER' && (
              <label>
                Select Beneficiary
                <select
                  value={beneficiaryId}
                  onChange={(event) => setBeneficiaryId(event.target.value)}
                  required
                >
                  <option value="">Select beneficiary</option>

                  {activeBeneficiaries.map((beneficiary) => (
                    <option key={beneficiary.id} value={beneficiary.id}>
                      {beneficiary.beneficiaryName}
                      {' - '}
                      {beneficiary.bankName}
                      {' ('}
                      {beneficiary.accountNumber}
                      {')'}
                    </option>
                  ))}
                </select>

                {activeBeneficiaries.length === 0 && (
                  <small>No active beneficiaries. Add one below.</small>
                )}
              </label>
            )}

            <label>
              Amount (INR)
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                placeholder="Enter amount"
                required
              />
            </label>

            <label>
              Description (Optional)
              <input
                type="text"
                maxLength={255}
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="What is this transaction for?"
              />
            </label>

            {operation === 'DEPOSIT' && (
              <div className="ct-demo-notice">
                This is a simulated deposit for project demonstration.
                It does not add real money from an external bank.
              </div>
            )}

            <button
              className="ct-submit"
              type="submit"
              disabled={processing || loading || activeAccounts.length === 0}
            >
              {processing ? 'Processing...' : 'Submit Transaction'}
            </button>

          </form>
        </div>

        <div className="ct-side-card">

          <div className="ct-side-heading">
            <div>
              <h3>Beneficiaries</h3>
              <p>Manage saved recipients.</p>
            </div>

            <button
              className="ct-add-button"
              onClick={() => setShowBeneficiaryForm(!showBeneficiaryForm)}
            >
              <Plus size={17} />
              Add
            </button>
          </div>

          {showBeneficiaryForm && (
            <form
              className="ct-beneficiary-form"
              onSubmit={handleAddBeneficiary}
            >
              <label>
                Beneficiary Name
                <input
                  value={beneficiaryName}
                  onChange={(event) => setBeneficiaryName(event.target.value)}
                  maxLength={100}
                  required
                />
              </label>

              <label>
                Nickname (Optional)
                <input
                  value={nickname}
                  onChange={(event) => setNickname(event.target.value)}
                  maxLength={50}
                />
              </label>

              <label>
                Bank Name
                <input
                  value={bankName}
                  onChange={(event) => setBankName(event.target.value)}
                  maxLength={100}
                  required
                />
              </label>

              <label>
                Account Number
                <input
                  value={beneficiaryAccountNumber}
                  onChange={(event) =>
                    setBeneficiaryAccountNumber(event.target.value)
                  }
                  inputMode="numeric"
                  pattern="[0-9]+"
                  minLength={5}
                  maxLength={34}
                  required
                />
              </label>

              <label>
                IFSC Code
                <input
                  value={ifscCode}
                  onChange={(event) => setIfscCode(event.target.value.toUpperCase())}
                  pattern="[A-Z]{4}0[A-Z0-9]{6}"
                  maxLength={11}
                  placeholder="ABCD0123456"
                  required
                />
              </label>

              <button type="submit" disabled={addingBeneficiary}>
                {addingBeneficiary ? 'Saving...' : 'Save Beneficiary'}
              </button>
            </form>
          )}

          <div className="ct-beneficiary-list">
            {beneficiaries.length === 0 ? (
              <p className="ct-empty">No beneficiaries added yet.</p>
            ) : (
              beneficiaries.map((beneficiary) => (
                <div className="ct-beneficiary-item" key={beneficiary.id}>
                  <div className="ct-beneficiary-icon">
                    <Users size={19} />
                  </div>

                  <div className="ct-beneficiary-details">
                    <strong>
                      {beneficiary.nickname || beneficiary.beneficiaryName}
                    </strong>
                    <span>{beneficiary.bankName}</span>
                    <small>{beneficiary.accountNumber}</small>
                  </div>

                  <span
                    className={`ct-beneficiary-status ${beneficiary.status.toLowerCase()}`}
                  >
                    {beneficiary.status}
                  </span>
                </div>
              ))
            )}
          </div>

        </div>
      </div>

      <div className="ct-history-card">

        <div className="ct-history-heading">
          <div>
            <h3>Transaction History</h3>
            <p>Your recent banking activity.</p>
          </div>

          <History size={22} />
        </div>

        {loading ? (
          <div className="ct-empty">
            <RefreshCw size={20} />
            Loading transactions...
          </div>
        ) : transactions.length === 0 ? (
          <div className="ct-empty">
            No transactions yet. Your activity will appear here.
          </div>
        ) : (
          <div className="ct-table-wrapper">
            <table className="ct-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>

              <tbody>
                {transactions.map((transaction) => (
                  <tr key={transaction.id}>
                    <td className="ct-reference">
                      {transaction.referenceNumber}
                    </td>

                    <td>
                      {transaction.transactionType.replaceAll('_', ' ')}
                    </td>

                    <td className="ct-amount">
                      {formatCurrency(
                        transaction.amount,
                        transaction.currency
                      )}
                    </td>

                    <td>
                      <span
                        className={`ct-transaction-status ${transaction.status.toLowerCase()}`}
                      >
                        {transaction.status}
                      </span>
                    </td>

                    <td>
                      {new Date(transaction.createdAt).toLocaleString('en-IN')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

      </div>

    </section>
  )
}

export default CustomerTransactions