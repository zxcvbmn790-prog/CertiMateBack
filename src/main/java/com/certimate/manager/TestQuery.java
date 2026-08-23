import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestQuery {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://3.39.237.132:3306/CertiMate";
        String user = "certimate";
        String pass = "CertiMate!2026App";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            System.out.println("--- ai_learn ---");
            ResultSet rs1 = stmt.executeQuery("SELECT learn_id, cert_id FROM ai_learn LIMIT 5");
            while(rs1.next()) {
                System.out.println(rs1.getLong("learn_id") + ", " + rs1.getLong("cert_id"));
            }
            rs1.close();

            System.out.println("--- CERTIFICATION ---");
            ResultSet rs2 = stmt.executeQuery("SELECT cert_id, cert_name FROM CERTIFICATION LIMIT 5");
            while(rs2.next()) {
                System.out.println(rs2.getLong("cert_id") + ", " + rs2.getString("cert_name"));
            }
            rs2.close();

            System.out.println("--- user_quiz_history ---");
            ResultSet rs3 = stmt.executeQuery("SELECT history_id, learn_id, is_correct FROM user_quiz_history ORDER BY history_id DESC LIMIT 5");
            while(rs3.next()) {
                System.out.println(rs3.getLong("history_id") + ", " + rs3.getLong("learn_id") + ", " + rs3.getBoolean("is_correct"));
            }
            rs3.close();

            System.out.println("--- user_learn_log ---");
            ResultSet rs4 = stmt.executeQuery("SELECT log_id, user_id, cert_id, study_time_min, correct_rate FROM user_learn_log ORDER BY log_id DESC LIMIT 5");
            while(rs4.next()) {
                System.out.println(rs4.getLong("log_id") + ", u=" + rs4.getLong("user_id") + ", c=" + rs4.getLong("cert_id") + ", t=" + rs4.getInt("study_time_min") + ", r=" + rs4.getFloat("correct_rate"));
            }
            rs4.close();
        }
    }
}
