package flyproject.accounts;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class FlyAMT implements Runnable{
    @Override
    public void run() {
        FlyAccounts.logger.info("[THREAD] 主线程正在启动...");
        FlyAccounts.logger.info("正在监听 /0.0.0.0:" + FlyAccounts.config.getInt("port"));
        ServerSocket s = null;
        try {
            s = new ServerSocket(FlyAccounts.config.getInt("port"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true){
            Socket socket = null;
            try {
                socket = s.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (FlyAccounts.config.getStringList("blacklist").contains(socket.getInetAddress().getHostAddress())){
                FlyAccounts.logger.warn("[BLACKLIST] 已拦截黑名单IP " + socket.getInetAddress().getHostAddress() + " 的请求");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                continue;
            }
            FlyAccounts.logger.info("[REQUEST] IP: " + socket.getInetAddress().getHostAddress() + " 发起请求");
            //用于接收客户端发来的请求
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            //用于发送返回信息,可以不需要装饰这么多io流使用缓冲流时发送数据要注意调用.flush()方法
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String str = null;
            boolean status = true;
            try {
                str = br.readLine();
                str = FlyAccounts.decrypt(str);
                String[] send = str.split(":");
                String password = FlyAccounts.config.getString("user." + send[0]);
                if (password==null){
                    pw.println(FlyAccounts.encrypt("INVAILD"));
                    pw.flush();
                    pw.close();
                    br.close();
                    socket.close();
                    status = false;
                } else if (!password.equals(send[1])){
                    pw.println(FlyAccounts.encrypt("INVAILD"));
                    pw.flush();
                    pw.close();
                    br.close();
                    socket.close();
                    status = false;
                } else if (FlyAccounts.cooldown.contains(send[0])){
                    pw.println(FlyAccounts.encrypt("CD"));
                    pw.flush();
                    pw.close();
                    br.close();
                    socket.close();
                    status = false;
                } else {
                    Scanner sc = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/accounts.txt"));
                    String rw = sc.nextLine();
                    pw.println(FlyAccounts.encrypt(rw));
                    pw.flush();
                    pw.close();
                    FileWriter fileStream = new FileWriter(System.getProperty("user.dir") + "/accounts.txt");
                    BufferedWriter out = new BufferedWriter(fileStream);
                    while(sc.hasNextLine()) {
                        String next = sc.nextLine();
                        if(next.equals("\n"))
                            out.newLine();
                        else
                            out.write(next);
                        out.newLine();
                    }
                    FlyAccounts.cooldown.add(send[0]);
                    new Thread(() -> {
                        if (FlyAccounts.cooldown.contains(send[0])){
                            FlyAccounts.cooldown.remove(send[0]);
                        }
                    }).start();
                    out.close();
                    socket.close();
                }
            } catch (Exception e){
                continue;
            }
            if (!status) continue;
            FlyAccounts.logger.info("[REWARD] 已返回一个账号给IP " + socket.getInetAddress().getHostAddress());
        }
    }
}
