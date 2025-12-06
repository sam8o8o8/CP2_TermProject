package termProject;
import java.util.*;
public class Customer {
	private String name; //고객 이름
	private String customerId; //고객 아이디
	private String password; //비밀번호(은행 로그인용)
	private String address; //주소
	private String phone; //연락처
	private List<Account> accounts; //고객이 소유한 계좌 목록
	
	//생성자
	public Customer(String name, String customerId, String password, String address, String phone) {
		this.name = name;
		this.customerId = customerId;
		this.password = password;
		this.address = address;
		this.phone = phone;
		this.accounts = new ArrayList<>();
	}
	
	//일부 고객 속성에 대한 값 설정 - 비밀번호(은행 로그인용)
	public boolean setPassword(String previousPassword, String newPassword) {
		if(!this.password.equals(previousPassword)) {
			System.out.println("이전 비밀번호가 일치하지 않습니다.");
			return false;
		}
		
		this.password = newPassword;
		System.out.println("비밀번호가 변경되었습니다.");
		return true;
	}
		
	//특정 소유 계좌의 발견
	public Account findAccount(String accountNumber) {
		for (Account acc : accounts) {
			if (acc.getAccountNumber().equals(accountNumber)) {
				return acc;
			}
		}
		return null;
	}
	
	//새로운 계좌의 추가
	public void addAccount(Account account) {
		accounts.add(account);
	}
	
	//소유 계좌의 삭제
	public void removeAccount(Account account) {
		accounts.remove(account);
	}
	
	//총 소유 계좌의 수
	public int getNumberOfAccounts() {
		return accounts.size();
	}
	
	//소유 계좌 잔액의 합
	public double getTotalBalance() {
		double total = 0;
		for (Account acc : accounts) {
			total += acc.getTotalBalance();
		}
		return total;
	}
	
	public String getName() { return name; }
	public String getCustomerId() { return customerId; }
	public String getPassword() { return password; }
	public String getAddress() { return address; }
	public String getPhone() { return phone; }
}
