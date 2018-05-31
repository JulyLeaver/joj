import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
        dos.writeUTF("------------------------------");
        while ((s = br.readLine()) != null) {
            dos.writeUTF(s);
        }
        dos.writeUTF("------------------------------");
        br.close();
    }

    @Override
    public void run() {
        super.run();
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF("JOJ");

            String cmd;
            String[] cmdSplit;

            while (true) {
                dos.writeUTF("unlock");

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
                        int bufS = dis.read(buf);
                        fos.write(buf, 0, bufS);
                    }
                    fos.close();

                    final String FILE_NAME = cmdSplit[1];
                    final String PROBLEM_NUMBER = cmdSplit[2];

                    if (!(new File("../Problems/" + PROBLEM_NUMBER).isDirectory())) {
                        Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "존재 하지 않는 번호 " + PROBLEM_NUMBER + " 채점 시도"));
                        dos.writeUTF(PROBLEM_NUMBER + "번 문제가 존재 하지 않습니다.");
                        continue;
                    }

                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "채점 시작"));
                    dos.writeUTF("채점 시작");

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
                    p.waitFor();

                    String s;
                    s = "UPDATE STATUS SET SUBMIT_COUNT = SUBMIT_COUNT + 1 " +
                            "WHERE PROBLEM_ID = " + PROBLEM_NUMBER;
                    DB.getInstance().executeUpdate(s);

                    final int exitValue = p.exitValue();
                    if (exitValue == 126) { // Run64 인자 에러
                        Msg.msgHelper(Msg.getLocalTime() + " : ./Run64 인자 에러 발생, 종료");
                        System.exit(0);
                    } else if (exitValue == 123) { // 컴파일 에러
                        fileReadHelper(LOCAL_ADDRESS + "/compileStderr.out", dos);
                        Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "컴파일 에러"));
                    } else if (exitValue == 128) { // AC, WA, TLE, MLE, RTE
                        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(LOCAL_ADDRESS + "/exitCode")));
                        final int exitCode = Integer.parseInt(br.readLine());
                        br.close();

                        final int what = (exitCode & 0x00FF0000) >> 16; // AC = 0, WA, TLE, MLE, RTE
                        if (what == 0 || what == 1) {
                            if (what == 0) {
                                s = "UPDATE GRADING SET P" + PROBLEM_NUMBER + " = 'Y' " +
                                        "WHERE USER_ID = '" + LOCAL_ADDRESS + '\'';
                                DB.getInstance().executeUpdate(s);
                            }

                            s = "UPDATE GRADING SET P" + PROBLEM_NUMBER + "S = " +
                                    ((exitCode & 0x0000FF00) >> 8) + '.' + (exitCode & 0x000000FF) +
                                    " WHERE USER_ID = '" + LOCAL_ADDRESS + '\'';
                            DB.getInstance().executeUpdate(s);
                        }
                    }
                    fileReadHelper(LOCAL_ADDRESS + "/runStdout.out", dos);

                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "채점 끝"));
                    dos.writeUTF("채점 끝");
                } else if (cmdSplit[0].equals("status")) {
                    Msg.msgHelper(Msg.log(LOCAL_ADDRESS, "현재 결과 요청"));

                    ResultSet r = DB.getInstance().executeQuery(
                            "SELECT * FROM GRADING WHERE USER_ID = '" + LOCAL_ADDRESS + "'");
                    ResultSetMetaData rt = r.getMetaData();
                    int c = r.getMetaData().getColumnCount();
                    dos.writeUTF("------------------------------");
                    for (int i = 2; i <= c; ++i) {
                        dos.writeUTF(rt.getColumnName(i) + ": " + r.getString(i));
                    }
                    dos.writeUTF("------------------------------");
                } else {
                    dos.writeUTF("존재하지 않는 명령어");
                }
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
