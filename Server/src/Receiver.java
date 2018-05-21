import java.io.*;
import java.net.Socket;
import java.util.Map;

public class Receiver extends Thread {
    Map<String, Socket> sockets;
    String LOCAL_ADDRESS;
    Socket socket;

    public Receiver(Map<String, Socket> sockets, Socket socket, String LOCAL_ADDRESS) {
        super();
        this.sockets = sockets;
        this.LOCAL_ADDRESS = LOCAL_ADDRESS;
        this.socket = socket;
    }

    @Override
    public void run() {
        super.run();
        try {
            InputStream is = socket.getInputStream();
            DataInputStream dis = new DataInputStream(is);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            String cmd;
            String[] cmdSplit;

            while (true) {
                cmd = dis.readUTF();
                cmdSplit = cmd.split(" ");

                Msg.msgHelper(Msg.cmd(LOCAL_ADDRESS, cmd));

                if (cmdSplit[0].equals("exit")) {
                    dos.writeUTF("연결을 끊습니다.");
                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "연결을 끊습니다."));
                    break;
                } else if (cmdSplit[0].equals("run")) { // ex: "run test.cpp 1000"
                    FileOutputStream fos = new FileOutputStream(LOCAL_ADDRESS + '/' + cmdSplit[1]);
                    byte[] buf = new byte[2048];
                    int len = dis.readInt();
                    for (int i = 0; i < len; ++i) {
                        int bufS = is.read(buf);
                        fos.write(buf, 0, bufS);
                    }
                    fos.close();

                    final String FILE_NAME = cmdSplit[1];
                    final String PROBLEM_NUMBER = cmdSplit[2];

                    if (!(new File("../Problems/" + PROBLEM_NUMBER).isDirectory())) {
                        Msg.msgHelper(Msg.log(LOCAL_ADDRESS, PROBLEM_NUMBER + "번 문제가 존재 하지 않습니다."));
                        dos.writeUTF(PROBLEM_NUMBER + "번 문제가 존재 하지 않습니다.");
                        continue;
                    }

                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "Run64 출력(시작)"));

                    ProcessBuilder pb = new ProcessBuilder("./Run64",
                            LOCAL_ADDRESS,
                            FILE_NAME,
                            PROBLEM_NUMBER
                    );
                    Process p = null;
                    try {
                        p = pb.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(0);
                    }

                    BufferedReader processBR = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String out;
                    while ((out = processBR.readLine()) != null) {
                        Msg.msgHelper(Msg.log(LOCAL_ADDRESS, out));
                        dos.writeUTF(out);
                    }
                    p.waitFor();
                    processBR.close();

                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "Run64 출력(끝)"));

                    String s = "UPDATE STATUS SET SUBMIT_COUNT = SUBMIT_COUNT + 1 " +
                            "WHERE PROBLEM_ID = " + PROBLEM_NUMBER;
                    DB.getInstance().executeUpdate(s);

                    if (p.exitValue() == 1) { // 맞았습니다.
                        s = "UPDATE GRADING SET P" + PROBLEM_NUMBER + " = 'Y' " +
                                "WHERE USER_ID = '" + LOCAL_ADDRESS + '\'';
                        DB.getInstance().executeUpdate(s);
                    }
                }
            }
            dis.close();
            socket.close();
        } catch (IOException e) { // EOFException -> IOException
            Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "연결이 끊겼습니다."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (this) {
            sockets.remove(LOCAL_ADDRESS);
        }
    }
}
