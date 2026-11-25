import { useState } from "react";
import {
  Shield,
  User,
  Bell,
  Monitor,
  LockKeyhole,
  Info,
} from "lucide-react";

export default function Settings() {
  const [activeTab, setActiveTab] = useState("General");
  const [notifications, setNotifications] = useState(true);
  const [emailNotifications, setEmailNotifications] = useState(true);
  const [alertNotifications, setAlertNotifications] = useState(true);
  const [darkMode, setDarkMode] = useState(false);

  const tabs = [
    { label: "General", icon: Monitor },
    { label: "Profile", icon: User },
    { label: "Notifications", icon: Bell },
    { label: "Security", icon: Shield },
    { label: "System", icon: LockKeyhole },
  ];

  const cardStyle: React.CSSProperties = {
    background: "#ffffff",
    border: "1px solid #e5e7eb",
    borderRadius: "12px",
    padding: "24px",
    marginBottom: "20px",
  };

  const inputStyle: React.CSSProperties = {
    width: "100%",
    padding: "11px 13px",
    border: "1px solid #d1d5db",
    borderRadius: "8px",
    fontSize: "14px",
    boxSizing: "border-box",
    outline: "none",
    background: "#ffffff",
    color: "#1e293b",
  };

  const labelStyle: React.CSSProperties = {
    display: "block",
    marginBottom: "8px",
    fontWeight: 600,
    fontSize: "14px",
    color: "#334155",
  };

  const toggleStyle = (enabled: boolean): React.CSSProperties => ({
    width: "44px",
    height: "24px",
    borderRadius: "20px",
    background: enabled ? "#2563eb" : "#cbd5e1",
    position: "relative",
    cursor: "pointer",
    border: "none",
    transition: "0.2s",
    flexShrink: 0,
  });

  const toggleDotStyle = (enabled: boolean): React.CSSProperties => ({
    width: "18px",
    height: "18px",
    background: "#ffffff",
    borderRadius: "50%",
    position: "absolute",
    top: "3px",
    left: enabled ? "23px" : "3px",
    transition: "0.2s",
  });

  const toggle = (
    label: string,
    description: string,
    value: boolean,
    setter: (value: boolean) => void
  ) => (
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        gap: "20px",
        padding: "16px 0",
        borderBottom: "1px solid #f1f5f9",
      }}
    >
      <div>
        <div style={{ fontWeight: 600, color: "#1e293b" }}>{label}</div>
        <div
          style={{
            color: "#64748b",
            fontSize: "13px",
            marginTop: "5px",
          }}
        >
          {description}
        </div>
      </div>

      <button
        type="button"
        role="switch"
        aria-checked={value}
        aria-label={label}
        style={toggleStyle(value)}
        onClick={() => setter(!value)}
      >
        <span style={toggleDotStyle(value)} />
      </button>
    </div>
  );

  const renderContent = () => {
    switch (activeTab) {
      case "General":
        return (
          <>
            <div style={cardStyle}>
              <h3 style={{ marginTop: 0 }}>General Preferences</h3>
              <p style={{ color: "#64748b", fontSize: "14px" }}>
                Manage your dashboard preferences.
              </p>

              <div style={{ marginTop: "24px" }}>
                <label style={labelStyle}>Application Name</label>
                <input
                  style={inputStyle}
                  value="FinSentinel"
                  readOnly
                />
              </div>

              <div style={{ marginTop: "20px" }}>
                <label style={labelStyle}>Language</label>
                <select style={inputStyle} defaultValue="English">
                  <option>English</option>
                </select>
              </div>

              <div style={{ marginTop: "20px" }}>
                <label style={labelStyle}>Timezone</label>
                <select style={inputStyle} defaultValue="Asia/Kolkata">
                  <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
                  <option value="UTC">UTC</option>
                </select>
              </div>
            </div>

            <div style={cardStyle}>
              <h3 style={{ marginTop: 0 }}>Appearance</h3>
              {toggle(
                "Dark Mode",
                "Enable dark appearance for the dashboard.",
                darkMode,
                setDarkMode
              )}

              <p
                style={{
                  color: "#64748b",
                  fontSize: "13px",
                  marginTop: "12px",
                }}
              >
                This preference is currently a UI toggle and does not yet
                apply a global theme.
              </p>
            </div>
          </>
        );

      case "Profile":
        return (
          <div style={cardStyle}>
            <h3 style={{ marginTop: 0 }}>Profile Information</h3>
            <p style={{ color: "#64748b", fontSize: "14px" }}>
              View your account information.
            </p>

            <div style={{ marginTop: "24px" }}>
              <label style={labelStyle}>Account</label>
              <input
                style={inputStyle}
                value={
                  localStorage.getItem("finsentinel_user_email") ||
                  "Authenticated User"
                }
                readOnly
              />
            </div>

            <div style={{ marginTop: "20px" }}>
              <label style={labelStyle}>Assigned Roles</label>
              <input
                style={inputStyle}
                value={
                  (() => {
                    try {
                      const roles = JSON.parse(
                        localStorage.getItem("finsentinel_user_roles") ||
                          "[]"
                      );
                      return Array.isArray(roles)
                        ? roles.join(", ")
                        : "Unknown";
                    } catch {
                      return "Unknown";
                    }
                  })()
                }
                readOnly
              />
            </div>

            <div
              style={{
                marginTop: "20px",
                padding: "14px",
                background: "#eff6ff",
                borderRadius: "8px",
                color: "#1d4ed8",
                fontSize: "13px",
              }}
            >
              <Info size={16} style={{ verticalAlign: "middle" }} />{" "}
              Profile information is retrieved from your current local
              authentication session.
            </div>
          </div>
        );

      case "Notifications":
        return (
          <div style={cardStyle}>
            <h3 style={{ marginTop: 0 }}>Notification Preferences</h3>
            <p style={{ color: "#64748b", fontSize: "14px" }}>
              Control how you receive notifications.
            </p>

            {toggle(
              "All Notifications",
              "Enable or disable all notification preferences.",
              notifications,
              (value) => {
                setNotifications(value);
                setEmailNotifications(value);
                setAlertNotifications(value);
              }
            )}

            {toggle(
              "Email Notifications",
              "Receive notifications through email.",
              emailNotifications,
              setEmailNotifications
            )}

            {toggle(
              "Fraud Alert Notifications",
              "Receive updates about suspicious activity and fraud alerts.",
              alertNotifications,
              setAlertNotifications
            )}
          </div>
        );

      case "Security":
        return (
          <>
            <div style={cardStyle}>
              <h3 style={{ marginTop: 0 }}>Security Overview</h3>
              <p style={{ color: "#64748b", fontSize: "14px" }}>
                Security information for your account.
              </p>

              <div
                style={{
                  background: "#f0fdf4",
                  padding: "16px",
                  borderRadius: "8px",
                  marginTop: "20px",
                  color: "#166534",
                }}
              >
                <Shield size={20} style={{ verticalAlign: "middle" }} />{" "}
                Authentication session active
              </div>

              <div style={{ marginTop: "20px" }}>
                <label style={labelStyle}>Authentication</label>
                <input
                  style={inputStyle}
                  value="JWT-based authentication"
                  readOnly
                />
              </div>

              <div style={{ marginTop: "20px" }}>
                <label style={labelStyle}>Session Status</label>
                <input
                  style={inputStyle}
                  value="Authenticated"
                  readOnly
                />
              </div>
            </div>

            <div style={cardStyle}>
              <h3 style={{ marginTop: 0 }}>Security Recommendations</h3>

              {[
                "Never share your password or authentication tokens.",
                "Log out when using a shared computer.",
                "Review fraud alerts regularly.",
                "Use strong and unique account credentials.",
              ].map((item, index) => (
                <div
                  key={index}
                  style={{
                    padding: "12px 0",
                    borderBottom: "1px solid #f1f5f9",
                    color: "#475569",
                    fontSize: "14px",
                  }}
                >
                  <Shield
                    size={16}
                    style={{
                      verticalAlign: "middle",
                      marginRight: "10px",
                      color: "#16a34a",
                    }}
                  />
                  {item}
                </div>
              ))}
            </div>
          </>
        );

      case "System":
        return (
          <div style={cardStyle}>
            <h3 style={{ marginTop: 0 }}>System Information</h3>
            <p style={{ color: "#64748b", fontSize: "14px" }}>
              Application environment information.
            </p>

            {[
              ["Application", "FinSentinel"],
              ["Platform", "Web Dashboard"],
              ["Frontend", "React + TypeScript"],
              ["Backend", "Spring Boot"],
              ["Fraud Detection", "Rule Engine + ML"],
              ["Explainability", "SHAP"],
              ["Authentication", "JWT"],
            ].map(([label, value]) => (
              <div
                key={label}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  gap: "15px",
                  padding: "16px 0",
                  borderBottom: "1px solid #f1f5f9",
                }}
              >
                <span style={{ color: "#64748b" }}>{label}</span>
                <span style={{ fontWeight: 600, color: "#334155" }}>
                  {value}
                </span>
              </div>
            ))}
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div style={{ padding: "24px", color: "#0f172a" }}>
      <div style={{ marginBottom: "24px" }}>
        <h1 style={{ fontSize: "26px", margin: "0 0 8px" }}>
          Settings
        </h1>
        <p style={{ color: "#64748b", margin: 0 }}>
          Manage your preferences, account and security.
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "220px minmax(0, 1fr)",
          gap: "24px",
          alignItems: "start",
        }}
      >
        {/* Settings Sidebar */}
        <div
          style={{
            background: "#ffffff",
            border: "1px solid #e5e7eb",
            borderRadius: "12px",
            padding: "10px",
          }}
        >
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const selected = activeTab === tab.label;

            return (
              <button
                key={tab.label}
                onClick={() => setActiveTab(tab.label)}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "12px",
                  width: "100%",
                  padding: "13px",
                  borderRadius: "8px",
                  border: "none",
                  background: selected ? "#eff6ff" : "transparent",
                  color: selected ? "#2563eb" : "#475569",
                  fontWeight: selected ? 600 : 500,
                  cursor: "pointer",
                  textAlign: "left",
                  marginBottom: "4px",
                }}
              >
                <Icon size={18} />
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Settings Content */}
        <div>{renderContent()}</div>
      </div>
    </div>
  );
}