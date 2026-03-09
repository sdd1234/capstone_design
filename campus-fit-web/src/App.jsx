import { useState } from 'react';
import apiClient from './api/client';

function App() {
  const [email, setEmail] = useState('test@uni.ac.kr');
  const [password, setPassword] = useState('P@ssw0rd12');
  const [result, setResult] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const response = await apiClient.post('/api/v1/auth/login', { email, password });
      const token = response.data?.data?.accessToken;
      if (token) {
        localStorage.setItem('accessToken', token);
      }
      setResult(JSON.stringify(response.data, null, 2));
    } catch (error) {
      setResult(error.response?.data ? JSON.stringify(error.response.data, null, 2) : error.message);
    }
  };

  return (
    <main className="page">
      <section className="card">
        <h1>Campus Fit Web Setup</h1>
        <p>Spring Boot API and React app wiring is ready.</p>

        <form onSubmit={handleLogin}>
          <label htmlFor="email">Email</label>
          <input id="email" value={email} onChange={(e) => setEmail(e.target.value)} />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button type="submit">Test Login API</button>
        </form>

        {result && <pre>{result}</pre>}
      </section>
    </main>
  );
}

export default App;
