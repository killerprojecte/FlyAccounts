package flyproject.accounts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FlyAccountsClient {
    static JTextArea username;
    static JTextArea password;
    public static void main(String[] args) {
        JFrame frame = new JFrame("FlyAccounts | Powered by FlyProject");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel mainPanel = new JPanel();
        username = new JTextArea(1,30);
        username.setLineWrap(true);
        username.setWrapStyleWord(true);
        username.setEditable(false);
        username.setToolTipText("Username");
        username.setText("Username");
        password = new JTextArea(1,30);
        password.setLineWrap(true);
        password.setWrapStyleWord(true);
        password.setEditable(false);
        password.setToolTipText("Password");
        password.setText("Password");
        JButton sendButton = new JButton("Get");
        sendButton.addActionListener(new SendButtonListener());
        mainPanel.add(sendButton);
        mainPanel.add(username);
        mainPanel.add(password);
        frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
        frame.setSize(500, 200);
        frame.setVisible(true);
    }
    public static class SendButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {

        }
    }
}
