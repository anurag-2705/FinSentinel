import { useEffect, useMemo, useState } from "react";
import {
  getAdminUsers,
  assignUserRole,
  removeUserRole,
  suspendUser,
  reactivateUser,
  type AdminUser,
} from "./services/adminUserService";

const AVAILABLE_ROLES = ["CUSTOMER", "FRAUD_ANALYST", "ADMIN"];

function formatDate(value: string | null) {
  if (!value) return "Never";
  return new Date(value).toLocaleString();
}

function statusColor(status: string) {
  switch (status) {
    case "ACTIVE":
      return { color: "#4ade80", background: "#22c55e1c" };
    case "SUSPENDED":
      return { color: "#f87171", background: "#ef44441c" };
    case "PENDING_VERIFICATION":
      return { color: "#fbbf24", background: "#f59e0b1c" };
    default:
      return { color: "#94a3b8", background: "#94a3b81c" };
  }
}

export default function AdminUsers() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [notice, setNotice] = useState("");
  const [roleSelections, setRoleSelections] = useState<Record<string, string>>({});

  async function loadUsers() {
    try {
      setLoading(true);
      setError("");
      const data = await getAdminUsers();
      setUsers(data);
    } catch (err: any) {
      console.error(err);
      setError(err?.response?.data?.message || "Unable to load users.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const searchable = [
        user.firstName,
        user.lastName,
        user.email,
        user.phoneNumber ?? "",
        user.id,
      ]
        .join(" ")
        .toLowerCase();

      const matchesSearch = searchable.includes(search.toLowerCase());
      const matchesStatus =
        statusFilter === "ALL" || user.status === statusFilter;
      const matchesRole =
        roleFilter === "ALL" || user.roles.includes(roleFilter);

      return matchesSearch && matchesStatus && matchesRole;
    });
  }, [users, search, statusFilter, roleFilter]);

  const activeCount = users.filter((u) => u.status === "ACTIVE").length;
  const suspendedCount = users.filter((u) => u.status === "SUSPENDED").length;
  const pendingCount = users.filter(
    (u) => u.status === "PENDING_VERIFICATION"
  ).length;

  async function handleAssignRole(user: AdminUser) {
    const role = roleSelections[user.id];

    if (!role) {
      setNotice("Select a role first.");
      return;
    }

    if (user.roles.includes(role)) {
      setNotice("This user already has that role.");
      return;
    }

    try {
      setActionLoading(user.id);
      setNotice("");
      const updated = await assignUserRole(user.id, role);
      setUsers((current) =>
        current.map((item) => (item.id === user.id ? updated : item))
      );
      setNotice(`${role} assigned successfully.`);
    } catch (err: any) {
      setNotice(
        err?.response?.data?.message || "Failed to assign role."
      );
    } finally {
      setActionLoading(null);
    }
  }

  async function handleRemoveRole(user: AdminUser, role: string) {
    if (!window.confirm(`Remove ${role} from ${user.email}?`)) return;

    try {
      setActionLoading(user.id);
      setNotice("");
      const updated = await removeUserRole(user.id, role);
      setUsers((current) =>
        current.map((item) => (item.id === user.id ? updated : item))
      );
      setNotice(`${role} removed successfully.`);
    } catch (err: any) {
      setNotice(err?.response?.data?.message || "Failed to remove role.");
    } finally {
      setActionLoading(null);
    }
  }

  async function handleStatusChange(user: AdminUser) {
    const isSuspended = user.status === "SUSPENDED";
    const action = isSuspended ? "reactivate" : "suspend";

    if (!window.confirm(`${action.toUpperCase()} account ${user.email}?`))
      return;

    try {
      setActionLoading(user.id);
      setNotice("");

      const updated = isSuspended
        ? await reactivateUser(user.id)
        : await suspendUser(user.id);

      setUsers((current) =>
        current.map((item) => (item.id === user.id ? updated : item))
      );

      setNotice(`Account ${action}d successfully.`);
    } catch (err: any) {
      setNotice(
        err?.response?.data?.message || `Failed to ${action} account.`
      );
    } finally {
      setActionLoading(null);
    }
  }

  const cardStyle: React.CSSProperties = {
    background: "linear-gradient(145deg, #152236, #111c2d)",
    border: "1px solid #26364b",
    borderRadius: "11px",
    padding: "19px",
  };

  if (loading) {
    return (
      <div className="dashboard-content">
        <div className="panel" style={{ padding: 30 }}>
          Loading user management...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-content">
        <div className="panel" style={{ padding: 30 }}>
          <p style={{ color: "#f87171" }}>{error}</p>
          <button className="secondary-button" onClick={loadUsers}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-content">
      <div className="welcome-section">
        <div>
          <h2>User Management</h2>
          <p>Manage accounts, permissions and account access.</p>
        </div>
        <button className="secondary-button" onClick={loadUsers}>
          Refresh Users
        </button>
      </div>

      <div className="stats-grid">
        {[
          { label: "Total Users", value: users.length, color: "#60a5fa" },
          { label: "Active Users", value: activeCount, color: "#4ade80" },
          { label: "Suspended", value: suspendedCount, color: "#f87171" },
          { label: "Pending Verification", value: pendingCount, color: "#fbbf24" },
        ].map((stat) => (
          <div key={stat.label} style={cardStyle}>
            <div style={{ color: "#9aa9bd", fontSize: 12 }}>{stat.label}</div>
            <h3 style={{ color: stat.color, fontSize: 28, margin: "14px 0 0" }}>
              {stat.value}
            </h3>
          </div>
        ))}
      </div>

      <div className="panel" style={{ marginTop: 20, overflow: "hidden" }}>
        <div className="alerts-header">
          <div className="panel-header">
            <div>
              <h3>Platform Users</h3>
              <p>Accounts, roles and access status</p>
            </div>
          </div>
        </div>

        <div className="table-toolbar">
          <div className="table-search">
            <input
              type="text"
              placeholder="Search name, email, phone or ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          <select
            className="filter-select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="PENDING_VERIFICATION">Pending verification</option>
            <option value="LOCKED">Locked</option>
          </select>

          <select
            className="filter-select"
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
          >
            <option value="ALL">All roles</option>
            {AVAILABLE_ROLES.map((role) => (
              <option key={role} value={role}>
                {role}
              </option>
            ))}
          </select>
        </div>

        {notice && (
          <div
            style={{
              margin: "0 21px 15px",
              padding: 12,
              borderRadius: 7,
              color: "#93c5fd",
              background: "#2563eb18",
              border: "1px solid #263f64",
              fontSize: 12,
            }}
          >
            {notice}
          </div>
        )}

        <div className="table-wrapper">
          <table style={{ minWidth: 1150 }}>
            <thead>
              <tr>
                <th>User</th>
                <th>Status</th>
                <th>Verification</th>
                <th>Roles</th>
                <th>Login / Security</th>
                <th>Registered</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div style={{ color: "#e2e8f0", fontWeight: 650 }}>
                      {user.firstName} {user.lastName}
                    </div>
                    <div style={{ color: "#8bbcff", marginTop: 5 }}>
                      {user.email}
                    </div>
                    <div style={{ color: "#8190a5", marginTop: 4 }}>
                      {user.phoneNumber || "No phone"}
                    </div>
                    <div
                      style={{
                        color: "#64748b",
                        marginTop: 4,
                        fontSize: 9,
                        maxWidth: 180,
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                      }}
                    >
                      {user.id}
                    </div>
                  </td>

                  <td>
                    <span
                      className="status-badge"
                      style={statusColor(user.status)}
                    >
                      {user.status.replaceAll("_", " ")}
                    </span>
                  </td>

                  <td>
                    <div
                      style={{
                        color: user.emailVerified ? "#4ade80" : "#fbbf24",
                        marginBottom: 6,
                      }}
                    >
                      Email {user.emailVerified ? "?" : "?"}
                    </div>
                    <div
                      style={{
                        color: user.phoneVerified ? "#4ade80" : "#fbbf24",
                      }}
                    >
                      Phone {user.phoneVerified ? "?" : "?"}
                    </div>
                  </td>

                  <td>
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "flex-start",
                        gap: 7,
                      }}
                    >
                      {user.roles.map((role) => (
                        <div
                          key={role}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: 7,
                          }}
                        >
                          <span
                            style={{
                              color: "#cbd5e1",
                              background: "#33415555",
                              border: "1px solid #34445b",
                              padding: "5px 8px",
                              borderRadius: 5,
                              fontSize: 9,
                            }}
                          >
                            {role}
                          </span>
                          <button
                            title={`Remove ${role}`}
                            disabled={actionLoading === user.id}
                            onClick={() => handleRemoveRole(user, role)}
                            style={{
                              color: "#f87171",
                              border: "1px solid #7f1d1d55",
                              background: "#ef444418",
                              borderRadius: 5,
                              padding: "3px 7px",
                              fontSize: 10,
                            }}
                          >
                            ×
                          </button>
                        </div>
                      ))}
                    </div>

                    <div style={{ display: "flex", gap: 5, marginTop: 10 }}>
                      <select
                        className="filter-select"
                        style={{ maxWidth: 125 }}
                        value={roleSelections[user.id] || ""}
                        onChange={(e) =>
                          setRoleSelections((current) => ({
                            ...current,
                            [user.id]: e.target.value,
                          }))
                        }
                      >
                        <option value="">Add role...</option>
                        {AVAILABLE_ROLES.filter(
                          (role) => !user.roles.includes(role)
                        ).map((role) => (
                          <option key={role} value={role}>
                            {role}
                          </option>
                        ))}
                      </select>

                      <button
                        disabled={actionLoading === user.id}
                        className="secondary-button"
                        style={{ padding: "6px 9px" }}
                        onClick={() => handleAssignRole(user)}
                      >
                        +
                      </button>
                    </div>
                  </td>

                  <td>
                    <div>{formatDate(user.lastLoginAt)}</div>
                    <div
                      style={{
                        color: user.failedLoginAttempts > 0 ? "#fbbf24" : "#8190a5",
                        marginTop: 6,
                      }}
                    >
                      Failed attempts: {user.failedLoginAttempts}
                    </div>
                    {user.lockedUntil && (
                      <div style={{ color: "#f87171", marginTop: 5 }}>
                        Locked until: {formatDate(user.lockedUntil)}
                      </div>
                    )}
                  </td>

                  <td>{formatDate(user.createdAt)}</td>

                  <td>
                    <button
                      disabled={actionLoading === user.id}
                      onClick={() => handleStatusChange(user)}
                      style={{
                        border: "1px solid",
                        borderColor:
                          user.status === "SUSPENDED"
                            ? "#166534"
                            : "#7f1d1d",
                        background:
                          user.status === "SUSPENDED"
                            ? "#22c55e18"
                            : "#ef444418",
                        color:
                          user.status === "SUSPENDED"
                            ? "#4ade80"
                            : "#f87171",
                        borderRadius: 6,
                        padding: "8px 10px",
                        fontSize: 10,
                      }}
                    >
                      {actionLoading === user.id
                        ? "Processing..."
                        : user.status === "SUSPENDED"
                        ? "Reactivate"
                        : "Suspend"}
                    </button>
                  </td>
                </tr>
              ))}

              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan={7} className="empty-state">
                    No users match your filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="table-footer">
          <span>
            Showing {filteredUsers.length} of {users.length} users
          </span>
        </div>
      </div>
    </div>
  );
}
