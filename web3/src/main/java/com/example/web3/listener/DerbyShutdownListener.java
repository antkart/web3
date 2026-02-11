package com.example.web3.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DerbyShutdownListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            // у derby успешный shutdown всегда кидает SQLException
            // xj015 считается нормой для system shutdown
            if (!"XJ015".equals(e.getSQLState()) && !"08006".equals(e.getSQLState())) {
                sce.getServletContext().log("Derby shutdown error: " + e.getMessage(), e);
            }
        }
    }
}
