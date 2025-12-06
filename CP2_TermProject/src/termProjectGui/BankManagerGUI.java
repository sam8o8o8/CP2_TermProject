package termProjectGui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class BankManagerGUI extends JFrame {
    private JTextArea logArea;
    private JTextField idField, pwField;
    private JButton loginButton;
    private JButton addCustomerButton, viewCustomerButton, deleteCustomerButton;
    private JButton addAccountButton, viewAccountButton, deleteAccountButton;
    private JButton printCustomerListButton, printAccountListButton;
    private JButton stopButton, clearButton;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean loggedIn = false;
    
    public BankManagerGUI() {
        setTitle("은행 관리자 GUI");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 로그인 패널
        JPanel loginPanel = new JPanel();
        loginPanel.add(new JLabel("ID:"));
        idField = new JTextField(8);
        loginPanel.add(idField);
        loginPanel.add(new JLabel("PW:"));
        pwField = new JPasswordField(8);
        loginPanel.add(pwField);
        loginButton = new JButton("로그인");
        loginPanel.add(loginButton);

        // 버튼 객체 생성
        addCustomerButton = new JButton("고객 추가");
        viewCustomerButton = new JButton("고객 조회");
        deleteCustomerButton = new JButton("고객 삭제");
        printCustomerListButton = new JButton("고객 리스트");

        addAccountButton = new JButton("계좌 추가");
        viewAccountButton = new JButton("계좌 조회");
        deleteAccountButton = new JButton("계좌 삭제");
        printAccountListButton = new JButton("계좌 리스트");

        stopButton = new JButton("정지");
        clearButton = new JButton("초기화");

        // 고객 관련 버튼 패널
        JPanel customerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        customerPanel.add(addCustomerButton);
        customerPanel.add(viewCustomerButton);
        customerPanel.add(deleteCustomerButton);
        customerPanel.add(printCustomerListButton);

        // 계좌 관련 버튼 패널
        JPanel accountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        accountPanel.add(addAccountButton);
        accountPanel.add(viewAccountButton);
        accountPanel.add(deleteAccountButton);
        accountPanel.add(printAccountListButton);

        // 서버 제어 버튼 패널
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(stopButton);
        controlPanel.add(clearButton);

        // 하단 전체 패널 (BoxLayout)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(customerPanel);
        bottomPanel.add(accountPanel);
        bottomPanel.add(controlPanel);

        // 로그 출력 영역
        logArea = new JTextArea();
        logArea.setEditable(false);

        add(loginPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 버튼 상태
        setButtonsEnabled(false);

        // 이벤트 설정
        loginButton.addActionListener(e -> authenticateManager());
        addCustomerButton.addActionListener(e -> addCustomer());
        viewCustomerButton.addActionListener(e -> viewCustomer());
        deleteCustomerButton.addActionListener(e -> deleteCustomer());
        printCustomerListButton.addActionListener(e -> printCustomerList());

        addAccountButton.addActionListener(e -> addAccount());
        viewAccountButton.addActionListener(e -> viewAccount());
        deleteAccountButton.addActionListener(e -> deleteAccount());
        printAccountListButton.addActionListener(e -> printAccountList());

        clearButton.addActionListener(e -> logArea.setText(""));
        stopButton.addActionListener(e -> stopServer());

        // 서버와 연결 시도
        connectToServer();
    }

    // 서버와 소켓 연결
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 6000); // 서버 주소/포트
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            appendLog("서버에 연결됨.");
        } catch (IOException e) {
            appendLog("서버 연결 실패: " + e.getMessage());
        }
    }

    // 로그 추가
    private void appendLog(String msg) {
        logArea.append(msg + "\n");
    }

    // 로그인 처리
    private void authenticateManager() {
        String id = idField.getText().trim();
        String pw = pwField.getText().trim();
        writer.println("AUTHENTICATE_USER|" + id + "|" + pw);
        try {
            String resp = reader.readLine();
            if ("MANAGER".equals(resp)) {
                loggedIn = true;
                appendLog("관리자 인증 성공.");
                setButtonsEnabled(true);
            } else {
                appendLog("관리자 인증 실패.");
            }
        } catch (IOException e) {
            appendLog("인증 오류: " + e.getMessage());
        }
    }

    // 버튼 활성화/비활성화
    private void setButtonsEnabled(boolean enabled) {
        addCustomerButton.setEnabled(enabled);
        viewCustomerButton.setEnabled(enabled);
        deleteCustomerButton.setEnabled(enabled);
        printCustomerListButton.setEnabled(enabled);

        addAccountButton.setEnabled(enabled);
        viewAccountButton.setEnabled(enabled);
        deleteAccountButton.setEnabled(enabled);
        printAccountListButton.setEnabled(enabled);

        stopButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }

    // 고객 추가
    private void addCustomer() {
        JTextField nameField = new JTextField();
        JTextField cidField = new JTextField();
        JTextField pwField = new JTextField();
        JTextField addrField = new JTextField();
        JTextField phField = new JTextField();
        Object[] params = {
            "이름", nameField,
            "아이디", cidField,
            "비밀번호", pwField,
            "주소", addrField,
            "전화", phField
        };
        int res = JOptionPane.showConfirmDialog(this, params, "고객 추가", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String cmd = "ADD_CUSTOMER|" + nameField.getText() + "|" + cidField.getText() + "|" + pwField.getText() +
                    "|" + addrField.getText() + "|" + phField.getText();
            writer.println(cmd);
            try {
                appendLog("고객 추가 요청: " + cmd);
                String resp = reader.readLine();
                appendLog("결과: " + resp);
            } catch (IOException e) { appendLog("오류: "+e.getMessage()); }
        }
    }

    // 고객 조회
    private void viewCustomer() {
        String cid = JOptionPane.showInputDialog(this, "검색할 고객 아이디?");
        if (cid != null && !cid.isEmpty()) {
            writer.println("FIND_CUSTOMER|" + cid);
            try {
                String resp = reader.readLine();
                appendLog("조회 결과: " + resp);
            } catch (IOException e) { appendLog("오류: "+e.getMessage()); }
        }
    }

    // 고객 삭제
    private void deleteCustomer() {
        String cid = JOptionPane.showInputDialog(this, "삭제할 고객 아이디?");
        if (cid != null && !cid.isEmpty()) {
            writer.println("DELETE_CUSTOMER|" + cid);
            try {
                String resp = reader.readLine();
                appendLog("삭제 결과: " + resp);
            } catch (IOException e) { appendLog("오류: "+e.getMessage()); }
        }
    }

    // 고객 리스트
    private void printCustomerList() {
        writer.println("PRINT_CUSTOMLIST");
        try {
        	String resp = reader.readLine();
        	String[] customers = resp.split(";");
        	StringBuilder sb = new StringBuilder("전체 고객 리스트:\n");
        	for (String c : customers) {
        	    if (!c.isEmpty()) sb.append(c).append("\n");
        	}
        	logArea.setText(sb.toString());
        } catch (IOException e) {
            appendLog("오류: " + e.getMessage());
        }
    }

    // 계좌 추가
    private void addAccount() {
        JTextField cidField = new JTextField();
        JTextField accNumField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Checking", "Savings"});
        JTextField pwField = new JTextField();
        JTextField rateField = new JTextField("0");
        JTextField maxTransField = new JTextField("0");
        Object[] params = {
            "고객 아이디", cidField,
            "계좌번호", accNumField,
            "계좌유형", typeBox,
            "비밀번호", pwField,
            "이자율", rateField,
            "자동이체한도", maxTransField
        };
        int res = JOptionPane.showConfirmDialog(this, params, "계좌 추가", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String cmd = "ADD_ACCOUNT|" + cidField.getText() + "|" + accNumField.getText()
                    + "|" + typeBox.getSelectedItem() + "|" + pwField.getText()
                    + "|" + rateField.getText() + "|" + maxTransField.getText();
            writer.println(cmd);
            try {
                String resp = reader.readLine();
                appendLog("계좌 추가 요청: " + cmd + "\n결과: " + resp);
            } catch (IOException e) { appendLog("오류: "+e.getMessage()); }
        }
    }

    // 계좌 조회
    private void viewAccount() {
        String accNum = JOptionPane.showInputDialog(this, "조회할 계좌번호?");
        if (accNum != null && !accNum.isEmpty()) {
            writer.println("FIND_ACCOUNT|" + accNum);
            try {
                String resp = reader.readLine();
                appendLog("계좌 조회 결과: " + resp);
            } catch (IOException e) { appendLog("오류: "+e.getMessage()); }
        }
    }

    // 계좌 삭제
    private void deleteAccount() {
        String accNum = JOptionPane.showInputDialog(this, "삭제할 계좌번호?");
        if (accNum != null && !accNum.isEmpty()) {
            writer.println("DELETE_ACCOUNT|" + accNum);
            try {
                String resp = reader.readLine();
                appendLog("계좌 삭제 결과: " + resp);
            } catch (IOException e) { appendLog("오류: "+e.getMessage()); }
        }
    }


    // 계좌 리스트
    private void printAccountList() {
        writer.println("PRINT_ACCOUNTLIST");
        try {
            String resp = reader.readLine();
            String[] accounts = resp.split(";");
            StringBuilder sb = new StringBuilder("전체 계좌 리스트:\n");
            for (String acc : accounts) {
                if (!acc.isEmpty()) sb.append(acc).append("\n");
            }
            logArea.setText(sb.toString());
        } catch (IOException e) {
            appendLog("오류: " + e.getMessage());
        }
    }

    
    private void stopServer() {
        appendLog("서버 중지(예시 구현, 실제 서버 중지 명령 추가 필요)");
        try {
            socket.close();
        } catch (IOException e) {}
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankManagerGUI().setVisible(true));
    }
}