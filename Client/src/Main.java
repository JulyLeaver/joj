import java.io.*;
import java.net.Socket;

public class Main {
    public static boolean lock = false;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("실행 인자 오류");
            return;
        }

        final String IP = args[0];
        final int PORT = 1851;
        try {
            Socket socket = new Socket(IP, PORT);

            new Receiver(socket).start();

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            String cmd;
            String[] cmdSplit;
            File sourceFile;
            while (true) {
                cmd = br.readLine();
                cmdSplit = cmd.split(" ");

                if (cmd.length() < 1 || cmdSplit.length < 1) continue;

                if (lock) {
                    System.out.println("처리 중 입니다.");
                    continue;
                }

                if (cmdSplit[0].equals("exit")) {
                    dos.writeUTF(cmd);
                    break;
                } else if (cmdSplit[0].equals("run")) { // ex: "run test.c 1000"
                    if (cmdSplit.length != 3) {
                        System.out.println("run 명령어 인자 오류");
                        continue;
                    }

                    sourceFile = new File(cmdSplit[1]);
                    if (!sourceFile.exists()) {
                        System.out.println('\"' + System.getProperty("user.dir") + '\"' + " 경로에 " + '\"' + cmdSplit[1] + '\"' + "파일이 존재하지 않습니다.");
                        continue;
                    }

                    lock = true;
                    dos.writeUTF(cmd);

                    FileInputStream fis = new FileInputStream(sourceFile);
                    byte[] buf = new byte[2048];
                    int len = 0;
                    while (fis.read(buf) != -1) {
                        ++len;
                    }
                    fis.close();
                    fis = new FileInputStream(sourceFile);
                    dos.writeInt(len);
                    while ((len = fis.read(buf)) != -1) {
                        dos.write(buf, 0, len);
                    }
                    fis.close();
                } else {
                    lock = true;
                    dos.writeUTF(cmd);
                }
            }
            br.close();
            dos.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("서버와 연결이 끊어졌습니다.");
        }
    }
}