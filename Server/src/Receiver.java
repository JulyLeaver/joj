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

    private void fileReadHelper(final String path, DataOutputStream dos) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        String s;
        while ((s = br.readLine()) != null) {
            dos.writeUTF(s);
            Msg.msgHelper(Msg.log(LOCAL_ADDRESS, s));
        }
        br.close();
    }

    @Override
    public void run() {
        super.run();
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
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
                    for (int i = 0; i < len; ++i) { // 음수 체크 안된다... 클라 소켓이 close 되지 않아서..? ㅇㅇ
                        int bufS = dis.read(buf);
                        fos.write(buf, 0, bufS);
                    }
                    fos.close();

                    final String FILE_NAME = cmdSplit[1];
                    final String PROBLEM_NUMBER = cmdSplit[2];

                    if (!(new File("../Problems/" + PROBLEM_NUMBER).isDirectory())) {
                        Msg.msgHelper(Msg.log(LOCAL_ADDRESS, PROBLEM_NUMBER + "번 문제가 존재 하지 않습니다."));
                        dos.writeUTF(PROBLEM_NUMBER + "번 문제가 존재 하지 않습니다.");
                        dos.writeUTF("cmd_able");
                        continue;
                    }

                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "Run64 출력(시작)"));

                    ProcessBuilder pb = new ProcessBuilder("./Run64",
                            LOCAL_ADDRESS,
                            FILE_NAME,
                            PROBLEM_NUMBER
                    );
//                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
//                    pb.redirectOutput(new File(LOCAL_ADDRESS + "/GradingResult.out"));
                    Process p = null;
                    try {
                        p = pb.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(0);
                    }
                    p.waitFor();

                    /*
                    BufferedReader processBR = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    char[] cBuf = new char[2048];
                    while ((len = processBR.read(cBuf)) != -1) {
//                        Msg.msgHelper(Msg.log(LOCAL_ADDRESS, out));
                        StringBuffer s = new StringBuffer();
                        for (int i = 0; i < len; ++i) {
                            s.append(cBuf[i]);
                        }
                        dos.writeUTF(s.toString());
                    }
                    processBR.close();
                    */

                    String s;
                    s = "UPDATE STATUS SET SUBMIT_COUNT = SUBMIT_COUNT + 1 " +
                            "WHERE PROBLEM_ID = " + PROBLEM_NUMBER;
                    DB.getInstance().executeUpdate(s);

                    final int exitValue = p.exitValue();
                    if (exitValue == 126) { // ./Run64 인자 에러
                        Msg.msgHelper(Msg.getLocalTime() + " : ./Run64 인자 에러 발생, 종료");
                        System.exit(0);
                    } else if (exitValue == 1) { // 컴파일 에러
                        fileReadHelper(LOCAL_ADDRESS + "/compileStderr.out", dos);
                    } else if (exitValue == 0) { // AC
                        s = "UPDATE GRADING SET P" + PROBLEM_NUMBER + " = 'Y' " +
                                "WHERE USER_ID = '" + LOCAL_ADDRESS + '\'';
                        DB.getInstance().executeUpdate(s);
                    } else {
                    }
                    fileReadHelper(LOCAL_ADDRESS + "/runStdout.out", dos);
                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "Run64 출력(끝)"));
                }
                dos.writeUTF("cmd_able");
            }
            dis.close();
            dos.close();
            socket.close();
        } catch (IOException e) {
            Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "연결이 끊겼습니다."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (this) {
            sockets.remove(LOCAL_ADDRESS);
        }
    }
}
