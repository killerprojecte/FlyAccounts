package flyproject.accounts;

import javax.crypto.Cipher;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class FlyAccountsClient {
    static JTextArea username;
    static JTextArea password;
    static String pkey = "";
    static String prkey = "";
    static String serverip = "127.0.0.1";
    static int serverport = 1567;
    public static void main(String[] args) {
        String user = JOptionPane.showInputDialog("请输入用户名");
        String pass = JOptionPane.showInputDialog("请输入密码");
        JFrame frame = new JFrame("FlyAccounts | Powered by FlyProject");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel mainPanel = new JPanel();
        username = new JTextArea(1,30);
        username.setLineWrap(true);
        username.setWrapStyleWord(true);
        username.setEditable(false);
        username.setToolTipText("用户名");
        username.setText("用户名");
        password = new JTextArea(1,30);
        password.setLineWrap(true);
        password.setWrapStyleWord(true);
        password.setEditable(false);
        password.setToolTipText("密码");
        password.setText("密码");
        JButton sendButton = new JButton("获取账号");
        sendButton.addActionListener(new SendButtonListener());
        mainPanel.add(username);
        mainPanel.add(password);
        mainPanel.add(sendButton);
        frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
        frame.setSize(400, 200);
        frame.setVisible(true);
    }
    private static void get(String user,String pass){
        Socket socket = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            //客户端socket指定服务器的地址和端口号
            socket = new Socket(serverip, serverport);
            socket.setSoTimeout(10000);
            //同服务器原理一样
            br = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream())));
            pw.println(encrypt(user + ":" + pass));
            pw.flush();
            String str = decrypt(br.readLine());
            if (str.equals("INVAILD")){
                JOptionPane.showMessageDialog(null,"账户名或密码错误");
            } else if (str.equals("CD")){
                JOptionPane.showMessageDialog(null,"用户正在冷却 请等待...");
            } else {
                String[] arg = str.split("|");
                username.setText(arg[0]);
                password.setText(arg[1]);
            }
        } catch (Exception e) {
        } finally {
            try {
                br.close();
                pw.close();
                socket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static String encrypt(String str) throws Exception{
        //base64编码的公钥
        byte[] decoded = Base64.getDecoder().decode(pkey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        //RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        String outStr = Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));
        return outStr;
    }
    public static String decrypt(String str) throws Exception{
        //64位解码加密后的字符串
        byte[] inputByte = Base64.getDecoder().decode(str.getBytes("UTF-8"));
        //base64编码的私钥
        byte[] decoded = Base64.getDecoder().decode(prkey);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        //RSA解密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        String outStr = new String(cipher.doFinal(inputByte));
        return outStr;
    }

    public static class SendButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {

        }
    }
}
