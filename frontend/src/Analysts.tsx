import { useEffect, useState } from "react";
import {
  dashboardService,
  type AnalystWorkload,
  type FraudAnalystPerformance,
} from "./services/dashboardService";

export default function Analysts() {
  const [workload, setWorkload] = useState<AnalystWorkload[]>([]);
  const [performance, setPerformance] = useState<FraudAnalystPerformance[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadAnalysts() {
      try {
        setLoading(true);

        const [workloadData, performanceData] = await Promise.all([
          dashboardService.getAnalystWorkload(),
          dashboardService.getAnalystPerformance("7D"),
        ]);

        setWorkload(workloadData);
        setPerformance(performanceData);
      } catch (err) {
        console.error(err);
        setError("Failed to load analyst data.");
      } finally {
        setLoading(false);
      }
    }

    loadAnalysts();
  }, []);

  const totalAssigned = workload.reduce(
    (sum, analyst) => sum + analyst.totalAssignedAlerts,
    0
  );

  const totalOpen = workload.reduce(
    (sum, analyst) => sum + analyst.openAssignedAlerts,
    0
  );

  const totalUnderReview = workload.reduce(
    (sum, analyst) => sum + analyst.underReviewAlerts,
    0
  );

  const totalResolved = workload.reduce(
    (sum, analyst) => sum + analyst.resolvedAlerts,
    0
  );

  const cardStyle: React.CSSProperties = {
    background: "#ffffff",
    border: "1px solid #e5e7eb",
    borderRadius: "12px",
    padding: "20px",
  };

  const tableHeaderStyle: React.CSSProperties = {
    padding: "14px 16px",
    textAlign: "left",
    color: "#64748b",
    fontSize: "12px",
    textTransform: "uppercase",
    letterSpacing: "0.6px",
    background: "#f8fafc",
    borderBottom: "1px solid #e5e7eb",
  };

  const tableCellStyle: React.CSSProperties = {
    padding: "16px",
    borderBottom: "1px solid #f1f5f9",
    fontSize: "14px",
    color: "#334155",
  };

  if (loading) {
    return (
      <div style={{ padding: "30px", color: "#64748b" }}>
        Loading analyst data...
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: "30px", color: "#dc2626" }}>
        {error}
      </div>
    );
  }

  return (
    <div style={{ padding: "24px", color: "#0f172a" }}>
      <div style={{ marginBottom: "24px" }}>
        <h1 style={{ fontSize: "26px", margin: "0 0 8px" }}>
          Fraud Analysts
        </h1>
        <p style={{ color: "#64748b", margin: 0 }}>
          Monitor analyst workload and investigation performance.
        </p>
      </div>

      {/* Summary Cards */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
          gap: "16px",
          marginBottom: "28px",
        }}
      >
        {[
          {
            label: "Total Assigned",
            value: totalAssigned,
            color: "#2563eb",
          },
          {
            label: "Open Alerts",
            value: totalOpen,
            color: "#f59e0b",
          },
          {
            label: "Under Review",
            value: totalUnderReview,
            color: "#8b5cf6",
          },
          {
            label: "Resolved",
            value: totalResolved,
            color: "#16a34a",
          },
        ].map((item) => (
          <div key={item.label} style={cardStyle}>
            <p style={{ color: "#64748b", margin: "0 0 10px", fontSize: "14px" }}>
              {item.label}
            </p>

            <h2
              style={{
                fontSize: "28px",
                margin: 0,
                color: item.color,
              }}
            >
              {item.value}
            </h2>
          </div>
        ))}
      </div>

      {/* Workload Table */}
      <div
        style={{
          ...cardStyle,
          padding: 0,
          overflowX: "auto",
          marginBottom: "28px",
        }}
      >
        <div style={{ padding: "20px" }}>
          <h2 style={{ margin: 0, fontSize: "18px" }}>
            Analyst Workload
          </h2>
          <p style={{ color: "#64748b", margin: "6px 0 0", fontSize: "13px" }}>
            Current assigned investigation workload
          </p>
        </div>

        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            minWidth: "700px",
          }}
        >
          <thead>
            <tr>
              <th style={tableHeaderStyle}>Analyst</th>
              <th style={tableHeaderStyle}>Total Assigned</th>
              <th style={tableHeaderStyle}>Open</th>
              <th style={tableHeaderStyle}>Under Review</th>
              <th style={tableHeaderStyle}>Resolved</th>
            </tr>
          </thead>

          <tbody>
            {workload.map((analyst) => (
              <tr key={analyst.analystId}>
                <td style={tableCellStyle}>
                  <div style={{ fontWeight: 600 }}>
                    {analyst.analystEmail}
                  </div>
                  <div style={{ color: "#94a3b8", fontSize: "12px" }}>
                    {analyst.analystId}
                  </div>
                </td>

                <td style={tableCellStyle}>
                  {analyst.totalAssignedAlerts}
                </td>

                <td style={tableCellStyle}>
                  <span
                    style={{
                      background: "#fef3c7",
                      color: "#92400e",
                      padding: "5px 10px",
                      borderRadius: "20px",
                    }}
                  >
                    {analyst.openAssignedAlerts}
                  </span>
                </td>

                <td style={tableCellStyle}>
                  <span
                    style={{
                      background: "#ede9fe",
                      color: "#6d28d9",
                      padding: "5px 10px",
                      borderRadius: "20px",
                    }}
                  >
                    {analyst.underReviewAlerts}
                  </span>
                </td>

                <td style={tableCellStyle}>
                  <span
                    style={{
                      background: "#dcfce7",
                      color: "#166534",
                      padding: "5px 10px",
                      borderRadius: "20px",
                    }}
                  >
                    {analyst.resolvedAlerts}
                  </span>
                </td>
              </tr>
            ))}

            {workload.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  style={{
                    ...tableCellStyle,
                    textAlign: "center",
                    padding: "30px",
                    color: "#94a3b8",
                  }}
                >
                  No analyst workload data available.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Performance Table */}
      <div
        style={{
          ...cardStyle,
          padding: 0,
          overflowX: "auto",
        }}
      >
        <div style={{ padding: "20px" }}>
          <h2 style={{ margin: 0, fontSize: "18px" }}>
            Analyst Performance
          </h2>
          <p style={{ color: "#64748b", margin: "6px 0 0", fontSize: "13px" }}>
            Investigation performance during the last 7 days
          </p>
        </div>

        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            minWidth: "700px",
          }}
        >
          <thead>
            <tr>
              <th style={tableHeaderStyle}>Analyst</th>
              <th style={tableHeaderStyle}>Assigned</th>
              <th style={tableHeaderStyle}>Approved</th>
              <th style={tableHeaderStyle}>Rejected</th>
              <th style={tableHeaderStyle}>Completed</th>
              <th style={tableHeaderStyle}>Resolution Rate</th>
            </tr>
          </thead>

          <tbody>
            {performance.map((analyst) => (
              <tr key={analyst.analystId}>
                <td style={tableCellStyle}>
                  <div style={{ fontWeight: 600 }}>
                    {analyst.analystEmail}
                  </div>
                </td>

                <td style={tableCellStyle}>
                  {analyst.totalAssigned}
                </td>

                <td style={tableCellStyle}>
                  {analyst.approvedAlerts}
                </td>

                <td style={tableCellStyle}>
                  {analyst.rejectedAlerts}
                </td>

                <td style={tableCellStyle}>
                  {analyst.completedInvestigations}
                </td>

                <td style={tableCellStyle}>
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "10px",
                    }}
                  >
                    <div
                      style={{
                        width: "80px",
                        height: "6px",
                        background: "#e2e8f0",
                        borderRadius: "10px",
                        overflow: "hidden",
                      }}
                    >
                      <div
                        style={{
                          width: `${Math.min(
                            Math.max(analyst.resolutionRate, 0),
                            100
                          )}%`,
                          height: "100%",
                          background: "#16a34a",
                        }}
                      />
                    </div>

                    <span style={{ fontWeight: 600 }}>
                      {analyst.resolutionRate.toFixed(1)}%
                    </span>
                  </div>
                </td>
              </tr>
            ))}

            {performance.length === 0 && (
              <tr>
                <td
                  colSpan={6}
                  style={{
                    ...tableCellStyle,
                    textAlign: "center",
                    padding: "30px",
                    color: "#94a3b8",
                  }}
                >
                  No analyst performance data available.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}