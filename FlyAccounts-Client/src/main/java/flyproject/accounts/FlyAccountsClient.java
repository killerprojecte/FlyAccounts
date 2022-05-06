package flyproject.accounts;

import javax.crypto.Cipher;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
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
    static String pkey = "Put your RSA public key here";
    static String prkey = "Put your RSA private key here";
    static String serverip = "Put your server ip here";
    static int serverport = 1567;
    static String u;
    static String p;
    public static void main(String[] args) throws IOException {
        File rfile = new File(System.getProperty("user.dir") + "/reg.lock");
        int reg = 9;
        if (!rfile.exists()){
            reg = JOptionPane.showConfirmDialog(null, "是否需要注册账号", "账号注册", JOptionPane.YES_NO_OPTION);
        }
        String cdkey = null;
        if (reg==JOptionPane.YES_OPTION){
            cdkey = JOptionPane.showInputDialog("请输入注册卡密");
        }
        if (!rfile.exists() && reg!=JOptionPane.YES_OPTION) System.exit(0);
        String user = JOptionPane.showInputDialog("请输入用户名");
        String pass = JOptionPane.showInputDialog("请输入密码");
        u = user;
        p = pass;
        if (user==null || pass==null){
            JOptionPane.showMessageDialog(null,"出错啦","输入的用户名或密码为空！",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (reg==JOptionPane.YES_OPTION){
            if (reg(user,pass,cdkey)){
                rfile.createNewFile();
            } else {
                System.exit(0);
            }
        }
        JFrame frame = new JFrame("FlyAccounts | Powered by FlyProject");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel mainPanel = new JPanel();
        JLabel label = new JLabel("用户名: ");
        JLabel label2 = new JLabel("密码: ");
        username = new JTextArea(1,27);
        username.setLineWrap(true);
        username.setWrapStyleWord(true);
        username.setEditable(false);
        username.setToolTipText("用户名");
        username.setText("用户名");
        password = new JTextArea(1,27);
        password.setLineWrap(true);
        password.setWrapStyleWord(true);
        password.setEditable(false);
        password.setToolTipText("密码");
        password.setText("密码");
        JButton sendButton = new JButton("获取账号");
        Font font = new Font("雅黑",Font.PLAIN,15);
        Font font2 = new Font("雅黑",Font.PLAIN,13);
        JLabel lb = new JLabel("你好, 尊敬的 " + user);
        Font font3 = new Font("雅黑",Font.BOLD,20);
        lb.setFont(font3);
        lb.setForeground(Color.GRAY);
        sendButton.addActionListener(new SendButtonListener());
        sendButton.setBackground(Color.PINK);
        sendButton.setFont(font);
        label.setFont(font2);
        label2.setFont(font2);
        username.setFont(font2);
        password.setFont(font2);
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.add(lb,gbc);
        mainPanel.add(label);
        mainPanel.add(username,gbc);
        mainPanel.add(label2);
        mainPanel.add(password,gbc);
        mainPanel.add(sendButton,gbc);
        mainPanel.setBackground(Color.PINK);
        frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
        frame.setSize(400, 200);
        frame.setVisible(true);
        Image image = ImageIO.read(FlyAccountsClient.class.getClassLoader().getResource("icon.png"));
        frame.setIconImage(image);
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
                username.setText("获取用户名错误");
                password.setText("获取密码错误");
            } else if (str.equals("CD")){
                JOptionPane.showMessageDialog(null,"用户正在冷却 请等待...");
                username.setText("获取用户名错误");
                password.setText("获取密码错误");
            } else {
                System.out.println(str);
                String[] arg = str.split(":");
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

    private static boolean reg(String user,String pass,String cdkey){
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
            pw.println(encrypt("REG::" + cdkey + "::" + user + "::" + pass));
            pw.flush();
            String str = decrypt(br.readLine());
            if (str.equals("INVAILD")){
                JOptionPane.showMessageDialog(null,"CDKey错误");
            } else if (str.equals("USER")){
                JOptionPane.showMessageDialog(null,"该账号已存在");
            } else if (str.equals("OK")){
                JOptionPane.showMessageDialog(null,"注册成功！");
                return true;
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
        return false;
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
            get(u,p);
        }
    }
}
