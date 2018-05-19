import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        final int PORT = 1851;
        System.out.println("JOJ Open");
        System.out.println("PORT: " + PORT);

        Map<String, Socket> sockets = new Hashtable<>();
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
                new File(LOCAL_ADDRESS).mkdir();
                sockets.put(LOCAL_ADDRESS, socket);
                new Receiver(sockets, socket, LOCAL_ADDRESS).start();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}