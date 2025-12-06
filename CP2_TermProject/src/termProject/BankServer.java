import java.io.*;
import java.net.*;
import java.util.*;

public class BankServer {
    List<Customer> customerList;
    List<Account> accountList;
    String managerID = "admin";
    String managerPassword = "admin123";
    
    public BankServer() {
        customerList = new ArrayList<>();
        accountList = new ArrayList<>();
        
        load();
        
        try {
        	ServerSocket server = new ServerSocket(6000);
            while (true) {
                Socket clientSocket = server.accept();
                
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }
    
    public void load() { loadCustomers(); loadAccounts();}
    public void save() { saveCustomers(); saveAccounts();}
    
    // 처음 실행될 때
    public void loadCustomers() {
        File file = new File("customer.txt");
        if (!file.exists()) {
            try {file.createNewFile();} 
            catch (IOException e) {e.printStackTrace();}
        } else {
        		try {
        			ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
					customerList = (ArrayList<Customer>) in.readObject();
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
        }
    }
   
    
    // list가 변경될 때
    public synchronized void saveCustomers() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("customer.txt"))) {
            out.writeObject(customerList);
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    // 처음 실행될 때
    public synchronized void loadAccounts() {
    	File file = new File("account.txt");
        if (!file.exists()) {
            try {file.createNewFile();} 
            catch (IOException e) {e.printStackTrace();}
        } else {
        	try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
        		accountList = (ArrayList<Account>) in.readObject();
        		for (Account acc : accountList) {
        			Customer owner = acc.getOwner();
        			if (owner != null) {
        				owner.addAccount(acc);
        			}
        		}	
        	} catch (IOException | ClassNotFoundException e) {
        		e.printStackTrace();
        	}
        }
    }
    
    // list가 변경될때
    public synchronized void saveAccounts() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("account.txt"))) {
            out.writeObject(accountList);
        } catch (IOException e) {}
    }
    
    // 권한 인증
    public synchronized String authenticateUser(String userID, String password) {
        if (userID.equals(managerID) && password.equals(managerPassword)) {
            return "MANAGER";
        }
        
        for (Customer customer : customerList) {
            if (customer.getCustomerId().equals(userID) && customer.getPassword().equals(password)) {
                return "CUSTOMER";
            }
        }
        
        return "INVALID";
    }
    
    // 신규고객추가
    public synchronized boolean addCustomer(String name, String customerID, String password, String address, String phone) {
        if (findCustomer(customerID) == null) {
        	Customer customer = new Customer(name, customerID, password, address, phone);
        	customerList.add(customer);
        	save();
        	return true;
        }
        return false;
    }
    
    // 고객삭제
    public synchronized boolean deleteCustomer(String customerID) {
        Customer customer = findCustomer(customerID);
        if (customer != null) {

        	// 고객의 모든 계좌 삭제
        	List<Account> customerAccounts = new ArrayList<>(customer.getAccountList());
        	for (Account account : customerAccounts) {
        		deleteAccount(account.getAccountNumber());
        	}
        
        	customerList.remove(customer);
        	save();
        	
        	return true;	
    	}
        
        return false;
    }
    
    // 고객조회
    public synchronized Customer findCustomer(String customerID) {
        Customer result = null;
        for (Customer c : customerList) {
            if (c.getCustomerId().equals(customerID)) {
                result = c;
                break;
            }
        }
        return result;  
    }
    
    // 계좌추가
    public synchronized boolean addAccount(String customerID, String accountNumber, String accountType, String password,  double interestRate, double maxTransferAmountToChecking, Date maturityDate) {
        Customer customer = findCustomer(customerID);
        if (customer == null || findAccount(accountNumber) != null) {
            return false;
        }
        
        Account account = null;
        
        if (accountType.equals("Checking")) {
            account = new CheckingAccount(customer, accountNumber, password);
        } else if (accountType.equals("Savings")) {
            account = new SavingsAccount(customer, accountNumber, password, interestRate, maxTransferAmountToChecking, maturityDate);
        }
        
        accountList.add(account);
        customer.addAccount(account);
        save();
        return true;
    }
    
    // 계좌삭제
    public synchronized boolean deleteAccount(String accountNumber) {
        Account account = findAccount(accountNumber);
        if (account == null) {
            return false;
        }
        
        Customer customer = account.getOwner();
        if (customer != null) {
        	customer.removeAccount(account);
        }
        
        accountList.remove(account);
        save();
        return true;
    }   
    
    // 계좌조회
    public synchronized Account findAccount(String accountNumber) {   
        Account result = null;
        for (Account a : accountList) {
            if (a.getAccountNumber().equals(accountNumber)) {
                result = a;
                break;
            }
        }
        return result;
    }
    
    // 특정 계좌 잔액 조회
    public synchronized double findbalance(String accountNumber) {
    	Account account = findAccount(accountNumber);
    	if(account != null) {
    		return account.getTotalBalance();
    	}
    	return -1;
    }
    
    // 입금
    public synchronized void credit(String accountNumber, double amount) {
    	Account account = findAccount(accountNumber);
    	if(account != null) {
    		account.deposit(amount);
    		save();
    	}
    }
    
    
    // 출금
    public synchronized boolean debit(String accountNumber,String inputPassword, double amount) {
    	Account account = findAccount(accountNumber);
    	boolean success = account.withdraw(amount, inputPassword); 
        	if (success) {
            save();
            return true;
       } return false;
    }
    
    // 계좌이체
    public synchronized boolean transfer(String fromAccountNumber, String toAccountNumber, String inputPassword, double amount) {
        Account fromAccount = findAccount(fromAccountNumber);
        Account toAccount = findAccount(toAccountNumber);
        
        if (fromAccount == null || toAccount == null || amount <= 0) {
            return false;
        }
        
        if (fromAccount.withdraw(amount, inputPassword)) {
            toAccount.deposit(amount);
            save();
            return true;
        }
        
        return false;
    }
    
    // 모든 고객 정보 출력
    public synchronized String printCustomerList() {
        StringBuilder sb = new StringBuilder();
        for (Customer customer : customerList) {
            sb.append("고객이름: ").append(customer.getName())
              .append(", 고객아이디: ").append(customer.getCustomerId())
              .append(", 고객주소: ").append(customer.getAddress())
              .append(", 연락처: ").append(customer.getPhone())
              .append(", 계좌개수: ").append(customer.getNumberOfAccounts())
              .append("\n");
        }
        return sb.toString();
    }
    
    // 모든 계좌 정보 출력
    public synchronized String printAccountList() {
        StringBuilder sb = new StringBuilder();
        for (Account account : accountList) {
            sb.append("계좌번호: ").append(account.getAccountNumber())
              .append(", 계좌유형: ").append(account.getAccountType())
              .append(", 소유고객: ").append(account.getOwner())
              .append("\n");
        }
        return sb.toString();
    }
    
    // 모든 고객의 수 
    public synchronized int getNumberOfCustomers() {
        return customerList.size();
    }
    
    // 총보유잔고
    public synchronized double getTotalBankBalance() {
    	double sum = 0;
    	for (Account a : accountList) {
    	    sum += a.getTotalBalance();
    	}
    	return sum;
    }
    
    
    public static void main(String[] args) {
        BankServer server = new BankServer();

    }
}


class ClientHandler implements Runnable {
	Socket clientSocket;
	BankServer server;
	BufferedReader reader;
	PrintWriter writer;

    public ClientHandler(Socket socket, BankServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // 입출력 스트림 설정
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String request;
            while ((request = reader.readLine()) != null) {
                // 요청 처리 및 응답 전송
                String response = processRequest(request);
                writer.println(response);
            }
        } catch (IOException e) {
        	e.printStackTrace();
        } 
        finally {
            // 소켓 및 스트림 닫기
            try {
            	reader.close();
            	writer.close();
            	clientSocket.close();
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
    }

    // 클라이언트 요청을 분석하고 BankServer의 메서드를 호출하여 결과를 반환
    private String processRequest(String request) {
        String[] parts = request.split("\\|"); // 명령어와 인자를 구분
        String command = parts[0];
         
            switch (command) {
                // 권한 인증 : AUTHENTICATE_USER|userID|password
                case "AUTHENTICATE_USER" :
                    return server.authenticateUser(parts[1], parts[2]); // MANAGER, CUSTOMER, INVALID 반환
                    
                // 신규 고객 추가 : ADD_CUSTOMER|name|id|password|address|phone
                case "ADD_CUSTOMER":
                    boolean success1 = server.addCustomer(parts[1], parts[2], parts[3], parts[4], parts[5]);
                    return success1 ? "SUCCESS" : "FAIL";
                    
                // 고객 삭제 : DELETE_CUSTOMER|customerID
                case "DELETE_CUSTOMER":
                    boolean success2 = server.deleteCustomer(parts[1]);
                    return success2 ? "SUCCESS" : "FAIL";
                    
                // 고객 조회 : FIND_CUSTOMER|customerID
                case "FIND_CUSTOMER":
                    Customer customer = server.findCustomer(parts[1]);
                    return customer != null ? "SUCCESS|" + customer.toString() : "FAIL";
                    
                // 계좌 추가 : ADD_ACCOUNT|customerID|accountNumber|accountType|password|interestRate|maxTransferAmountToChecking|maturityDate
                case "ADD_ACCOUNT":
                	String customerID = parts[1];
                   	String accountNumber = parts[2];
                   	String accountType = parts[3];
                   	String password = parts[4];
                   	double interestRate = Double.parseDouble(parts[5]);
                   	double maxTransferAmountToChecking = Double.parseDouble(parts[6]);
                   	Date maturityDate = parts[7];
                        
                   	boolean success3 = server.addAccount(customerID, accountNumber, accountType, password, interestRate, maxTransferAmountToChecking, maturityDate);
                   	return success3 ? "SUCCESS" : "FAIL";
                    
                // 계좌 삭제 : DELETE_ACCOUNT|accountNumber
                case "DELETE_ACCOUNT": 
                    boolean success4 = server.deleteAccount(parts[1]);
                    return success4 ? "SUCCESS" : "FAIL";
                    
                // 계좌 조회 : FIND_ACCOUNT|accountNumber
                case "FIND_ACCOUNT":
                    Account account = server.findAccount(parts[1]);
                    return account != null ? "SUCCESS|" + account.display() : "FAIL";
                    
                // 잔액 조회 : FIND_BALANCE|accountNumber
                case "FIND_BALANCE":
                    double balance = server.findbalance(parts[1]);
                    return balance != -1 ? "SUCCESS|" + balance : "FAIL";
                    
                // 입금 : CREDIT|accountNumber|amount
                case "CREDIT":
                    double amount1 = Double.parseDouble(parts[2]);
                    server.credit(parts[1], amount1);
                    return "SUCCESS";
                    
                // 출금 :  DEBIT|accountNumber|inputPassword|amount
                case "DEBIT": 
                    double amount2 = Double.parseDouble(parts[3]);
                    boolean success5 = server.debit(parts[1], parts[2], amount2);
                    return success5 ? "SUCCESS" : "FAIL";
                    
                // 계좌 이체 : TRANSFER|fromAccountNumbe|toAccountNumbe|inputPassword|amount
                case "TRANSFER":
                    double amount = Double.parseDouble(parts[4]);
                    boolean success6 = server.transfer(parts[1], parts[2], parts[3],amount);
                    return success6 ? "SUCCESS" : "FAIL";
                    
                // 모든 고객 정보 출력
                case "PRINT_CUSTOMLIST":
                    return "SUCCESS|" + server.printCustomerList();
                    
                // 모든 계좌 정보 출력
                case "PRINTACCOUNTLIST":
                    return "SUCCESS|" + server.printAccountList();
                    
                // 고객 수 조회
                case "GET_NUM_CUSTOMERS":
                    return "SUCCESS|" + server.getNumberOfCustomers();
                    
                // 총 보유 잔고 조회
                case "GET_TOTAL_BALANCE":
                    return "SUCCESS|" + server.getTotalBankBalance();
                    
                default:
                    return "ERROR|";
            }
    }
}









