package com.certimate.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestQuery {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://54.180.98.39:3306/CertiMate";
        String user = "certimate";
        String pass = "CertiMate!2026App";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            System.out.println("=== DESCRIBE community_post ===");
            ResultSet descRs = stmt.executeQuery("DESCRIBE community_post");
            while (descRs.next()) {
                System.out.println(descRs.getString(1) + " | " + descRs.getString(2));
            }
            descRs.close();

            System.out.println("=== community_post rows ===");
            ResultSet postRs = stmt.executeQuery("SELECT * FROM community_post");
            java.sql.ResultSetMetaData md = postRs.getMetaData();
            int colCount = md.getColumnCount();
            while (postRs.next()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    sb.append(md.getColumnName(i)).append("=").append(postRs.getObject(i)).append(", ");
                }
                System.out.println(sb.toString());
            }
            postRs.close();

        }
    }
}
