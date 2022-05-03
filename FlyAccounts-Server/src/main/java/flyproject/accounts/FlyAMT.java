package flyproject.accounts;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

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
            try {
                str = br.readLine();
                str = FlyAccounts.decrypt(str);
                if (str.startsWith("REG::")){
                    String[] reg = str.split("::");
                    String cdkey = reg[1];
                    String user = reg[2];
                    String pass = reg[3];
                    if (FlyAccounts.config.getStringList("cdkey").contains(cdkey)){
                        if (FlyAccounts.config.getString("user." + user)!=null){
                            pw.println(FlyAccounts.encrypt("USER"));
                            pw.flush();
                            pw.close();
                            br.close();
                            socket.close();
                            continue;
                        }
                        System.out.println("[REG] 注册账号中...");
                        List<String> keys = FlyAccounts.config.getStringList("cdkey");
                        keys.remove(cdkey);
                        if (keys.size()==0){
                            keys.add(UUID.randomUUID().toString());
                            System.out.println("[CDKey] 已自动生成一个CDK");
                        }
                        FlyAccounts.config.set("cdkey",keys);
                        FlyAccounts.config.set("user." + user,pass);
                        FlyAccounts.config.save(FlyAccounts.CF);
                        FlyAccounts.config = YamlConfiguration.loadConfiguration(FlyAccounts.CF);
                        pw.println(FlyAccounts.encrypt("OK"));
                        pw.flush();
                        pw.close();
                        br.close();
                        FlyAccounts.logger.info("[REG] IP: " + socket.getInetAddress().getHostAddress() + " 注册账号: " + user + " 成功");
                        socket.close();
                        continue;
                    } else {
                        pw.println(FlyAccounts.encrypt("INVAILD"));
                        pw.flush();
                        pw.close();
                        br.close();
                        socket.close();
                        continue;
                    }
                }
                String[] send = str.split(":");
                String password = FlyAccounts.config.getString("user." + send[0]);
                if (password==null){
                    pw.println(FlyAccounts.encrypt("INVAILD"));
                    pw.flush();
                    pw.close();
                    br.close();
                    socket.close();
                } else if (!password.equals(send[1])){
                    pw.println(FlyAccounts.encrypt("INVAILD"));
                    pw.flush();
                    pw.close();
                    br.close();
                    socket.close();
                } else if (FlyAccounts.cooldown.contains(send[0])){
                    pw.println(FlyAccounts.encrypt("CD"));
                    pw.flush();
                    pw.close();
                    br.close();
                    socket.close();
                } else {
                    File txtf = new File(System.getProperty("user.dir") + "/accounts.txt");
                    BufferedReader txr = new BufferedReader(new FileReader(txtf));
                    List<String> list = new ArrayList<>();
                    String r = null;
                    boolean skip = true;
                    while ((r = txr.readLine()) !=null){
                        if (skip){
                            pw.println(FlyAccounts.encrypt(r));
                            pw.flush();
                            pw.close();
                            skip = false;
                            continue;
                        }
                        list.add(r);
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(txtf));
                    for (String line : list){
                        bw.write(line);
                        bw.newLine();
                    }
                    bw.flush();
                    bw.close();
                    txr.close();
                    FlyAccounts.cooldown.add(send[0]);
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000 * 60 * 3);
                        } catch (InterruptedException e) {
                        }
                        if (FlyAccounts.cooldown.contains(send[0])){
                            FlyAccounts.cooldown.remove(send[0]);
                        }
                    }).start();
                    FlyAccounts.logger.info("[REWARD] 已返回一个账号给IP " + socket.getInetAddress().getHostAddress() + " 用户名: " + send[0]);
                    socket.close();
                }
            } catch (Exception e){
                continue;
            }
        }
    }
}
