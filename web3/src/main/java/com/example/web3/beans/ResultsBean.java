package com.example.web3.beans;

import com.example.web3.model.ResultRow;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.Serializable;
import java.sql.*;
import java.util.*;

public class ResultsBean implements Serializable {


    private String pointsJson;

    private static final String DB_URL = buildJdbcUrl();

    private static String buildJdbcUrl() {
        String baseDir = System.getProperty("jboss.server.data.dir");
        if (baseDir == null || baseDir.isBlank()) {
            baseDir = System.getProperty("user.home");
        }
        String dbPath = baseDir + File.separator + "web3-derby-db";
        return "jdbc:derby:" + dbPath + ";create=true";
    }

    public ResultsBean() {
        initDb();
    }

    public List<ResultRow> getAll() {
        // Показываем результаты ТОЛЬКО текущей сессии
        return loadFromDbForSession(currentSessionId());
    }

    public void add(ResultRow row) {
        Objects.requireNonNull(row, "row");
        insertIntoDb(row, currentSessionId());
    }

    public String getPointsJson() {
        List<ResultRow> snapshot = getAll();
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < snapshot.size(); i++) {
            ResultRow r = snapshot.get(i);
            sb.append("{\"x\":").append(fmt(r.getX()))
                    .append(",\"y\":").append(fmt(r.getY()))
                    .append(",\"hit\":").append(r.isHit())
                    .append('}');
            if (i < snapshot.size() - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    public void setPointsJson(String pointsJson) {
        this.pointsJson = pointsJson; // игнорируем
    }

    private String fmt(double v) {
        return String.format(Locale.US, "%.4f", v);
    }

    private void initDb() {
        ensureDerbyDriverAvailable();

        try (Connection c = DriverManager.getConnection(DB_URL)) {
            DatabaseMetaData md = c.getMetaData();

            boolean tableExists;
            try (ResultSet rs = md.getTables(null, null, "RESULTS", new String[]{"TABLE"})) {
                tableExists = rs.next();
            }

            if (!tableExists) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate(
                            "CREATE TABLE RESULTS (" +
                                    "id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                                    "session_id VARCHAR(64) NOT NULL, " +
                                    "x DOUBLE NOT NULL, " +
                                    "y DOUBLE NOT NULL, " +
                                    "r DOUBLE NOT NULL, " +
                                    "hit SMALLINT NOT NULL, " +
                                    "check_time VARCHAR(32) NOT NULL, " +
                                    "exec_time VARCHAR(32) NOT NULL" +
                                    ")"
                    );
                }
                return;
            }


            boolean hasSessionId;
            try (ResultSet cols = md.getColumns(null, null, "RESULTS", "SESSION_ID")) {
                hasSessionId = cols.next();
            }

            if (!hasSessionId) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE RESULTS ADD COLUMN session_id VARCHAR(64) NOT NULL DEFAULT 'legacy'");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }


    private void ensureDerbyDriverAvailable() {
        try {
            Class.forName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        } catch (ClassNotFoundException ignored) {
            boolean ok = false;
            try {
                Enumeration<Driver> drivers = DriverManager.getDrivers();
                while (drivers.hasMoreElements()) {
                    Driver d = drivers.nextElement();
                    String name = d.getClass().getName();
                    if (name.startsWith("org.apache.derby.")) {
                        ok = true;
                        break;
                    }
                }
            } catch (Exception ignored2) {
            }

            if (!ok) {
                throw new RuntimeException(
                        "Derby JDBC драйвер не найден в приложении. "
                );
            }
        }
    }

    private List<ResultRow> loadFromDbForSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Collections.emptyList();
        }

        String sql = "SELECT x, y, r, hit, check_time, exec_time FROM RESULTS WHERE session_id = ? ORDER BY id";
        try (Connection c = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, sessionId);

            List<ResultRow> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ResultRow(
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("r"),
                            rs.getInt("hit") == 1,
                            rs.getString("check_time"),
                            rs.getString("exec_time")
                    ));
                }
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("DB load failed: " + e.getMessage(), e);
        }
    }

    private void insertIntoDb(ResultRow row, String sessionId) {
        String sql = "INSERT INTO RESULTS (session_id, x, y, r, hit, check_time, exec_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, sessionId);
            ps.setDouble(2, row.getX());
            ps.setDouble(3, row.getY());
            ps.setDouble(4, row.getR());
            ps.setInt(5, row.isHit() ? 1 : 0);
            ps.setString(6, row.getCurrentTime());
            ps.setString(7, row.getExecTime());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB insert failed: " + e.getMessage(), e);
        }
    }

    private String currentSessionId() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) return "NOSESSION";

        Object sessionObj = fc.getExternalContext().getSession(true);
        if (sessionObj instanceof HttpSession) {
            return ((HttpSession) sessionObj).getId();
        }
        return String.valueOf(sessionObj);
    }

}
