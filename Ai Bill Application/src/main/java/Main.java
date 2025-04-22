import Controller.MenuUI;
import DAO.CsvTransactionDao;
import Service.Impl.TransactionServiceImpl;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Create and show the GUI
        TransactionServiceImpl.csvTransactionDao = new CsvTransactionDao();
        MenuUI menuUI=new MenuUI();

        JPanel mainPanel = menuUI.createMainPanel();
        showUI(mainPanel);
    }
    private static void showUI(JPanel panel) {
        // TODO 查看根目录
        System.out.println("当前工作目录：" + System.getProperty("user.dir"));

        JFrame frame = new JFrame("MenuUI Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);
        frame.add(panel);
        frame.setVisible(true);

        // 保持界面显示一段时间（5秒）
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        frame.dispose(); // 关闭界面
    }
}