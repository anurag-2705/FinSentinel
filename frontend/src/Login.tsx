import { useState, type FormEvent } from 'react'
import { ShieldCheck } from 'lucide-react'
import api from './services/api'

interface LoginProps {
  onLogin: () => void
}

function Login({ onLogin }: LoginProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    try {
      setLoading(true)
      setError('')

      const response = await api.post('/api/v1/auth/login', {
        email,
        password,
      })

      localStorage.setItem(
  'finsentinel_access_token',
  response.data.accessToken
)

localStorage.setItem(
  'finsentinel_user_roles',
  JSON.stringify(response.data.roles)
)

localStorage.setItem(
  'finsentinel_user_name',
  `${response.data.firstName} ${response.data.lastName}`
)

      onLogin()
    } catch (err) {
      console.error('Login error:', err)
      setError('Login failed. Check your email and password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      background: '#0f172a',
      padding: '20px',
    }}>
      <form
        onSubmit={handleLogin}
        style={{
          width: '100%',
          maxWidth: '400px',
          padding: '35px',
          background: '#1e293b',
          borderRadius: '14px',
          color: 'white',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '25px' }}>
          <ShieldCheck size={45} />
          <h2>FinSentinel</h2>
          <p>Secure Banking & Fraud Monitoring</p>
        </div>

        <label>Email Address</label>
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="Enter your email"
          required
          style={inputStyle}
        />

        <label>Password</label>
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="Enter your password"
          required
          style={inputStyle}
        />

        {error && (
          <p style={{ color: '#f87171' }} role="alert">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={loading}
          style={{
            width: '100%',
            padding: '12px',
            marginTop: '15px',
            background: '#2563eb',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
          }}
        >
          {loading ? 'Signing in...' : 'Sign In'}
        </button>
      </form>
    </div>
  )
}

const inputStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  padding: '12px',
  margin: '8px 0 18px',
  boxSizing: 'border-box',
  borderRadius: '8px',
  border: '1px solid #475569',
  background: '#334155',
  color: 'white',
}

export default Login