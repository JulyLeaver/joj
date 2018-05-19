import java.io.DataInputStream;
import java.net.Socket;

public class Receiver extends Thread {
    private Socket socket;

    public Receiver(Socket socket) {
        super();
        this.socket = socket;
    }

    @Override
    public void run() {
        super.run();
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String cmd;
            while(true) {
                cmd = dis.readUTF();
                System.out.println(cmd);
            }
        } catch (Exception e) {
            System.out.println("서버와 연결이 끊어졌습니다.");
        }
    }
}