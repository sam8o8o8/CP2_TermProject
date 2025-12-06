package termProjectGui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ATMClientGUI extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // 로그인 UI
    private JTextField idField = new JTextField(12);
    private JPasswordField pwField = new JPasswordField(12);
    private JButton loginBtn = new JButton("로그인");
    private JLabel loginStatus = new JLabel(" ");

    // 메인 UI
    private JTextArea infoArea = new JTextArea(8, 36);
    private JTextField accountField = new JTextField(16);
    private JLabel currentUserLabel = new JLabel("로그인 필요");

    // 입/출/이체 관련
    private JTextField amountField = new JTextField(10);
    private JPasswordField accPwField = new JPasswordField(10); // 계좌 비밀번호 입력 (출금/이체용)
    private JTextField toAccountField = new JTextField(16);

    public ATMClientGUI(String host, int port) {
        setTitle("ATM Client (GUI)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        connectToServer(host, port);

        // 상단: 로그인 패널
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBorder(BorderFactory.createTitledBorder("로그인"));
        top.add(new JLabel("고객 ID:")); top.add(idField);
        top.add(new JLabel("비밀번호:")); top.add(pwField);
        top.add(loginBtn);
        top.add(loginStatus);
        add(top, BorderLayout.NORTH);

        // 중앙: 고객 정보 / 계좌 입력
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createTitledBorder("고객 정보 / 계좌 선택"));
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        center.add(new JScrollPane(infoArea), BorderLayout.CENTER);

        JPanel choose = new JPanel(new FlowLayout(FlowLayout.LEFT));
        choose.add(new JLabel("계좌번호:")); choose.add(accountField);
        JButton refreshCustBtn = new JButton("고객정보 새로고침");
        choose.add(refreshCustBtn);
        center.add(choose, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        // 하단: 작업 버튼들
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        JPanel ops1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton balanceBtn = new JButton("잔액조회");
        JButton depositBtn = new JButton("입금");
        JButton withdrawBtn = new JButton("출금");
        ops1.add(balanceBtn); ops1.add(new JLabel("금액:")); ops1.add(amountField);
        ops1.add(depositBtn); ops1.add(withdrawBtn);
        ops1.add(new JLabel("계좌비밀번호(출금/이체):")); ops1.add(accPwField);

        JPanel ops2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ops2.add(new JLabel("이체받는 계좌:")); ops2.add(toAccountField);
        JButton transferBtn = new JButton("계좌이체");
        JButton logoutBtn = new JButton("로그아웃");
        ops2.add(transferBtn);
        ops2.add(logoutBtn);

        bottom.add(ops1);
        bottom.add(ops2);

        add(bottom, BorderLayout.SOUTH);

        // 이벤트 리스너
        loginBtn.addActionListener(e -> doLogin());
        refreshCustBtn.addActionListener(e -> fetchCustomerInfo());
        balanceBtn.addActionListener(e -> doBalance());
        depositBtn.addActionListener(e -> doDeposit());
        withdrawBtn.addActionListener(e -> doWithdraw());
        transferBtn.addActionListener(e -> doTransfer());
        logoutBtn.addActionListener(e -> doLogout());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("서버 연결: " + host + ":" + port);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "서버에 연결할 수 없습니다:\n" + ex.getMessage());
            System.exit(1);
        }
    }

    // ------------------------- 동작 메서드 -------------------------
    private void doLogin() {
        String id = idField.getText().trim();
        String pw = new String(pwField.getPassword()).trim();
        if (id.isEmpty() || pw.isEmpty()) {
            loginStatus.setText("ID/PW를 입력하세요.");
            return;
        }

        send("AUTHENTICATE_USER|" + id + "|" + pw);
        String resp = receive();
        if (resp == null) { loginStatus.setText("서버 응답 없음"); return; }

        if (resp.equals("CUSTOMER")) {
            currentUserLabel.setText("사용자: " + id);
            loginStatus.setText("로그인 성공 (CUSTOMER)");
            // 자동으로 고객 정보 조회
            fetchCustomerInfo();
        } else if (resp.equals("MANAGER")) {
            loginStatus.setText("관리자 계정입니다 (ATM 클라이언트는 고객용).");
        } else {
            loginStatus.setText("로그인 실패");
        }
    }

    private void fetchCustomerInfo() {
        String id = idField.getText().trim();
        if (id.isEmpty()) { loginStatus.setText("먼저 로그인하세요."); return; }
        send("FIND_CUSTOMER|" + id);
        String resp = receive();
        if (resp == null) { infoArea.setText("서버 응답 없음"); return; }

        if (resp.startsWith("SUCCESS|")) {
            String payload = resp.substring("SUCCESS|".length());
            // 서버가 반환한 customer.toString()을 그대로 표시
            infoArea.setText(payload + "\n\n(위 텍스트에서 계좌번호를 확인하여 '계좌번호' 입력란에 붙여넣기 하세요.)");
        } else {
            infoArea.setText("고객 정보를 가져오지 못했습니다.");
        }
    }

    private void doBalance() {
        String acc = accountField.getText().trim();
        if (acc.isEmpty()) { JOptionPane.showMessageDialog(this, "계좌번호 입력"); return; }
        send("FIND_BALANCE|" + acc);
        String resp = receive();
        if (resp == null) { JOptionPane.showMessageDialog(this, "서버 응답 없음"); return; }
        if (resp.startsWith("SUCCESS|")) {
            String bal = resp.substring("SUCCESS|".length());
            JOptionPane.showMessageDialog(this, "잔액: " + bal);
        } else {
            JOptionPane.showMessageDialog(this, "잔액조회 실패");
        }
    }

    private void doDeposit() {
        String acc = accountField.getText().trim();
        String amt = amountField.getText().trim();
        if (acc.isEmpty() || amt.isEmpty()) { JOptionPane.showMessageDialog(this, "계좌번호와 금액을 입력하세요."); return; }
        send("CREDIT|" + acc + "|" + amt);
        String resp = receive();
        if (resp == null) { JOptionPane.showMessageDialog(this, "서버 응답 없음"); return; }
        if (resp.equals("SUCCESS")) {
            JOptionPane.showMessageDialog(this, "입금 성공");
        } else {
            JOptionPane.showMessageDialog(this, "입금 실패");
        }
    }

    private void doWithdraw() {
        String acc = accountField.getText().trim();
        String amt = amountField.getText().trim();
        String accPw = new String(accPwField.getPassword()).trim();
        if (acc.isEmpty() || amt.isEmpty() || accPw.isEmpty()) { JOptionPane.showMessageDialog(this, "계좌번호, 금액, 계좌비밀번호 입력"); return; }
        send("DEBIT|" + acc + "|" + accPw + "|" + amt);
        String resp = receive();
        if (resp == null) { JOptionPane.showMessageDialog(this, "서버 응답 없음"); return; }
        if (resp.equals("SUCCESS")) {
            JOptionPane.showMessageDialog(this, "출금 성공");
        } else {
            JOptionPane.showMessageDialog(this, "출금 실패 (잔액 부족/비밀번호 오류 등)");
        }
    }

    private void doTransfer() {
        String from = accountField.getText().trim();
        String to = toAccountField.getText().trim();
        String amt = amountField.getText().trim();
        String accPw = new String(accPwField.getPassword()).trim();
        if (from.isEmpty() || to.isEmpty() || amt.isEmpty() || accPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "출금계좌, 수취계좌, 금액, 계좌비밀번호를 입력하세요.");
            return;
        }
        send("TRANSFER|" + from + "|" + to + "|" + accPw + "|" + amt);
        String resp = receive();
        if (resp == null) { JOptionPane.showMessageDialog(this, "서버 응답 없음"); return; }
        if (resp.equals("SUCCESS")) {
            JOptionPane.showMessageDialog(this, "이체 성공");
        } else {
            JOptionPane.showMessageDialog(this, "이체 실패 (잔액 부족/비밀번호 오류/대상계좌 없음 등)");
        }
    }

    private void doLogout() {
        try {
            // 단순 UI 초기화 (서버쪽 세션은 없음)
            idField.setText("");
            pwField.setText("");
            accountField.setText("");
            amountField.setText("");
            accPwField.setText("");
            toAccountField.setText("");
            infoArea.setText("");
            loginStatus.setText("로그아웃됨");
        } catch (Exception e) {}
    }

    // ------------------------- 통신 헬퍼 -------------------------
    private synchronized void send(String msg) {
        out.println(msg);
        out.flush();
        System.out.println("SEND: " + msg);
    }

    private synchronized String receive() {
        try {
            String line = in.readLine();
            System.out.println("RECV: " + line);
            return line;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "서버와의 통신 오류: " + ex.getMessage());
            return null;
        }
    }

    // 프로그램 종료시 소켓 정리
    private void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {}
    }

    // ------------------------- main -------------------------
    public static void main(String[] args) {
        // 기본 서버 호스트/포트 (BankServer.java에서 6000 포트로 열려 있음)
        String host = "127.0.0.1";
        int port = 6000;
        SwingUtilities.invokeLater(() -> {
            ATMClientGUI gui = new ATMClientGUI(host, port);
            gui.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    gui.closeConnection();
                }
            });
        });
    }
}

