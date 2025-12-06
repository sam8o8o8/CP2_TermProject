package termProject;
import java.util.*;
public abstract class Account {
	protected Customer owner; //소유 고객
	protected String accountNumber; //계좌번호
	protected String accountType; //계좌 유형
	protected double totalBalance; //총 잔액
	protected double availableBalance; //인출 가능 잔액
	protected Date openDate; //계좌 개설일
	private String password; //비밀번호(계좌 비밀번호)
	
	//생성자
	public Account(Customer owner, String accountNumber, String accountType, String password) {
		this.owner = owner;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.totalBalance = 0;
		this.availableBalance = 0;
		this.openDate = new Date();
		this.password = password;
	}
	
	public boolean checkPassword(String inputPassword) { //자식 클래스에서 비밀번호가 일치하는지 확인
		return this.password.equals(inputPassword);
	}
	
	//일부 계좌 속성에 대한 값 설정 - 비밀번호(계좌 비밀번호)
	public boolean setPassword(String previousPassword, String newPassword) {
		if(!this.password.equals(previousPassword)) {
			System.out.println("이전 비밀번호가 일치하지 않습니다.");
			return false;
		}
		
		this.password = newPassword;
		System.out.println("비밀번호가 변경되었습니다.");
		return true;
	}
	
	//일부 계좌 속성에 대한 값 반환 - 소유 고객
	public Customer getOwner() {
		return owner;
	}
	
	//일부 계좌 속성에 대한 값 반환 - 계좌번호
	public String getAccountNumber() {
		return accountNumber;
	}
	
	//일부 계좌 속성에 대한 값 반환 - 총 잔액
	public double getTotalBalance() {
		return totalBalance;
	}
	
	//일부 계좌 속성에 대한 값 반환 - 인출 가능 잔액
	public  double getAvailableBalance() {
		return availableBalance;
	}
	
	//일부 계좌 속성에 대한 값 반환 - 계좌 유형
	public String getAccountType() {
		return accountType;
	}
	
	//입금(Credit)
	public void deposit(double amount) {
		totalBalance += amount;
		availableBalance += amount;
	}
	
	//출금(Debit)
	public boolean withdraw(double amount, String inputPassword) {
		if (!this.password.equals(inputPassword)) { //비밀번호가 일치하지 않을 경우
			System.out.println("비밀번호가 일치하지 않습니다.");
			return false;
		}
		
		if (amount <= availableBalance) {
			totalBalance -= amount;
			availableBalance -= amount;
			System.out.println("출금 완료. 잔액: " + totalBalance + "원");
			return true;
		} else { //출금 요청 금액보다 인출 가능 잔액이 적을 경우
			System.out.println("잔액 부족"); //에러메시지 출력
			return false;
		}
	}
	
	//예금 계좌 종류 따라 자식클래스에서 재정의
	public abstract String display();
}
