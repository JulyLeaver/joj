import java.io.BufferedWriter;
import java.util.Calendar;

public class Msg {
    private static BufferedWriter bw = null;

    public static void setLogFileStream(BufferedWriter bw) {
        Msg.bw = bw;
    }

    public static String getLocalTime() {
        Calendar cal = Calendar.getInstance();
        return new String(Integer.toString(cal.get(Calendar.YEAR)) + "Y-" +
                Integer.toString(cal.get(Calendar.MONTH)) + "M-" +
                Integer.toString(cal.get(Calendar.DAY_OF_MONTH)) + "D-" +
                Integer.toString(cal.get(Calendar.HOUR_OF_DAY)) + "H-" +
                Integer.toString(cal.get(Calendar.MINUTE)) + "M-" +
                Integer.toString(cal.get(Calendar.SECOND)) + "S"
        );
    }

    public static String log(String NAME, String s) {
        return new String(getLocalTime() + "<Log>" + NAME + ": " + s);
    }

    public static String cmd(String NAME, String s) {
        return new String(getLocalTime() + "<Command>" + NAME + ": \"" + s + '\"');
    }

    public static void msgHelper(String log) {
        System.out.println(log);
        try {
            bw.write(log);
            bw.newLine();
            bw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}