import java.io.*;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("실행 인자 오류");
            return;
        }
        final String IP = args[0];
        final int PORT = 1851;
        try {
            Socket socket = new Socket(IP, PORT);

            new Receiver(socket).start();

            OutputStream os = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            String cmd;
            String[] cmdSplit;
            File sourceFile;
            while(true) {
                System.out.print('>');

                cmd = br.readLine();
                cmdSplit = cmd.split(" ");

                dos.writeUTF(cmd);

                if(cmdSplit[0].equals("exit")) {
                    break;
                }
                else if(cmdSplit[0].equals("run")) { // ex: "run test.c 1000"

                    sourceFile = new File(cmdSplit[1]);
                    if (cmdSplit.length != 3) {
                        System.out.println("run 명령어 인자 오류");
                        continue;
                    }
                    if (!sourceFile.exists()) {
                        System.out.println('\"' + System.getProperty("user.dir") + '\"' + " 경로에 " + '\"' + cmdSplit[1] + '\"' + "파일이 존재하지 않습니다.");
                        continue;
                    }
                    FileInputStream fis = new FileInputStream(sourceFile);
                    byte[] buf = new byte[2048];
                    int len = 0;
                    while(fis.read(buf) != -1) {
                        ++len;
                    }
                    fis.close();
                    dos.writeInt(len);
                    fis = new FileInputStream(sourceFile);
                    while((len = fis.read(buf)) != -1) {
                        os.write(buf, 0, len);
                    }
                    fis.close();
                }
            }
            dos.close();
            br.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}