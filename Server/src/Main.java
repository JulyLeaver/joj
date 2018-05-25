import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        DB db = DB.getInstance();
        db.initTable();

        final int PORT = 1851;
        System.out.println("JOJ Open");
        System.out.println("PORT: " + PORT);

        Map<String, Socket> sockets = new HashMap<>();
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log.txt")));
            Msg.setLogFileStream(bw);

            ServerSocket serverSocket = new ServerSocket(PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                final String LOCAL_ADDRESS = socket.getInetAddress().getHostAddress();
                if (sockets.containsKey(LOCAL_ADDRESS)) {
                    new DataOutputStream(socket.getOutputStream()).writeUTF("이미 접속 중...");
                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "이미 접속 중..."));
                    socket.close();
                    continue;
                }
                Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "접속 하셨습니다."));

                sockets.put(LOCAL_ADDRESS, socket);
                new File(LOCAL_ADDRESS).mkdir();

                ResultSet rs = DB.getInstance().executeQuery("SELECT NOT EXISTS (SELECT * FROM GRADING WHERE USER_ID = '" + LOCAL_ADDRESS + "')");
                rs.next();
                if (rs.getBoolean(1)) {
                    DB.getInstance().executeUpdate("INSERT INTO GRADING(USER_ID) VALUES('" + LOCAL_ADDRESS + "')");
                }
                new Receiver(sockets, socket, LOCAL_ADDRESS).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}