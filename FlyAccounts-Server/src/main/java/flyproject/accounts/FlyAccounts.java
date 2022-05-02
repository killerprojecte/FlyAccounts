package flyproject.accounts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.crypto.Cipher;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class FlyAccounts {
    public static Logger logger = LogManager.getRootLogger();
    public static FileConfiguration config;
    public static String pkey;
    public static String prkey;
    public static List<String> cooldown = new ArrayList<>();
    public static void main(String[] args) throws Exception {
        logger.info("[FlyAccounts] 正在加载...");
        saveResource("config.yml");
        saveResource("accounts.txt");
        File CF = new File(System.getProperty("user.dir") + "/config.yml");
        config = YamlConfiguration.loadConfiguration(CF);
        if (config.get("privatekey")==null || config.get("publickey")==null){
            logger.warn("检测到未配置RSA密钥");
            String[] key = genKey();
            config.set("privatekey",key[1]);
            config.set("publickey",key[0]);
            logger.warn("已自动生成");
            config.save(CF);
            config = YamlConfiguration.loadConfiguration(CF);
        }
        pkey = config.getString("publickey");
        prkey = config.getString("privatekey");
        Thread t = new Thread(new FlyAMT());
        t.start();
        Thread daemon = new Thread(() -> {
            logger.info("[守护进程] 守护进程已启动!");
            while (true){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                if (!t.isAlive()){
                    logger.warn("[守护进程] 主线程崩溃 正在自动重启");
                    t.start();
                }
            }
        });
        daemon.setDaemon(true);
        daemon.start();
        new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                try {
                    String str = br.readLine();
                    if (str.startsWith("add")){
                        String[] arg = str.split(" ");
                        config.set("user." + arg[1],arg[2]);
                        config.save(CF);
                        config = YamlConfiguration.loadConfiguration(CF);
                        System.out.println("添加成功");
                    } else if (str.equalsIgnoreCase("reload")){
                        config = YamlConfiguration.loadConfiguration(CF);
                        System.out.println("重载成功");
                    } else {
                        System.out.println("add 用户名 密码 ———— 添加账号");
                        System.out.println("reload ———— 重载配置文件");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public static String encrypt(String str) throws Exception {
        //base64编码的公钥
        byte[] decoded = Base64.getDecoder().decode(pkey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        //RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        String outStr = Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));
        return outStr;
    }

    public static String decrypt(String str) throws Exception {
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
    public static String[] genKey() throws NoSuchAlgorithmException {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // 初始化密钥对生成器，密钥大小为96-1024位
        keyPairGen.initialize(1024, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();   // 得到私钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();  // 得到公钥
        String publicKeyString = new String(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        // 得到私钥字符串
        String privateKeyString = new String(Base64.getEncoder().encodeToString((privateKey.getEncoded())));
        // 将公钥和私钥保存到Map
        return new String[]{publicKeyString,privateKeyString};
    }
    private static void saveFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }
    public static void saveResource(String name){
        URL resource = FlyAccounts.class.getClassLoader().getResource(name);
        File file = new File(System.getProperty("user.dir") + "/" + name);
        if (file.exists()) return;
        if (resource==null) return;
        try {
            URLConnection connection = resource.openConnection();
            connection.setUseCaches(false);
            saveFile(connection.getInputStream(),file);
            logger.info("[FlyAccounts] 正在生成文件: " + name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
