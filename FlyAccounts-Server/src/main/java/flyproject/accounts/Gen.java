package flyproject.accounts;

import java.security.NoSuchAlgorithmException;

public class Gen {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        String[] key = FlyAccounts.genKey();
        System.out.println("Public Key:");
        System.out.println(key[0]);
        System.out.println();
        System.out.println("Private Key:");
        System.out.println(key[1]);
    }
}
