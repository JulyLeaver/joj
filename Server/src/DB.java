import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;

/*
 * GRADING TABLE
 * USER_ID 		| P1000 | P1000S | P1001 | P1001S | P1002 | P1002S ...
 * <127.0.0.1	     Y	   56.4       N 	   0       Y      100>
 *
 * STATUS TABLE
 * PROBLEM_ID   | SUBMIT_COUNT
 * <1000   		            3>
 * <1001		            5>
 * <1002		            0>
 * ...
 * */
public class DB {
    private static String DB_NAME = "System.db";

    public static void setDB_NAME(final String DB_NAME) {
        DB.DB_NAME = DB_NAME;
    }

    private static DB instance = null;

    public static DB getInstance() {
        return instance == null ? instance = new DB() : instance;
    }

    Connection connection = null;
    Statement sm = null;

    private DB() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            sm = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() {
//        executeUpdate("DROP TABLE IF EXISTS STATUS");
//        executeUpdate("DROP TABLE IF EXISTS GRADING");

        StringBuilder s = new StringBuilder(128);

        s.append("CREATE TABLE STATUS(");
        s.append("PROBLEM_ID NUMBER NOT NULL UNIQUE, ");
        s.append("SUBMIT_COUNT NUMBER DEFAULT 0");
        s.append(")");
        executeUpdate(s.toString());

        ArrayList<Integer> problemFolders = new ArrayList<>();
        final String PROBLEM_FOLDER_PATH = "../Problems/";
        File file = new File(PROBLEM_FOLDER_PATH);
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; ++i) {
            if (files[i].isDirectory()) {
                problemFolders.add(Integer.parseInt(files[i].getName()));
            }
        }
        Collections.sort(problemFolders);

        for (int i = 0; i < problemFolders.size(); ++i) {
            executeUpdate("INSERT INTO STATUS(PROBLEM_ID) VALUES(" + problemFolders.get(i) + ")");
        }
        s = new StringBuilder(128);
        s.append("CREATE TABLE GRADING(");
        s.append("USER_ID VARCHAR2(15) NOT NULL UNIQUE, ");
        for (int i = 0; i < problemFolders.size(); ++i) {
            s.append("P" + problemFolders.get(i) + " CHAR(1) DEFAULT 'N', " +
                    "P" + problemFolders.get(i) + "S REAL DEFAULT 0,");
        }
        s.setCharAt(s.length() - 1, ')');
        executeUpdate(s.toString());
    }

    public int executeUpdate(String s) {
        int r = 0;
        synchronized (instance) {
            try {
                r = sm.executeUpdate(s);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return r;
    }

    public ResultSet executeQuery(String s) {
        ResultSet r = null;
        synchronized (instance) {
            try {
                r = sm.executeQuery(s);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return r;
    }
}
