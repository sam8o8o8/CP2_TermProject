package termProject;
import java.util.Date;
//저축예금계좌
public class SavingsAccount extends Account {
	private double interestRate; //이자율
	private double maxTransferAmountToChecking; //당좌예금계좌에 자동이체 될 수 있는 최대 금액
	private Date maturityDate; //만기일
	
	//생성자
	public SavingsAccount(Customer owner, String accountNumber, String password, double interestRate, double maxTransferAmountToChecking, Date maturityDate) {
		super(owner, accountNumber, "Savings", password);
		this.interestRate = interestRate;
		this.maxTransferAmountToChecking = maxTransferAmountToChecking;
		this.maturityDate = maturityDate;
	}
	
	//일부 계좌 속성에 대한 값 반환 - 당좌예금계좌에 자동이체 될 수 있는 최대 금액
	public double getMaxTransferAmountToChecking() {
		return maxTransferAmountToChecking;
	}
	
	public void applyInterest() {
		double interest = totalBalance * interestRate;
		deposit(interest);
	}
	
	@Override
	public boolean withdraw(double amount, String inputPassword) {
		if (!checkPassword(inputPassword)) { //비밀번호가 일치하지 않을 경우
			System.out.println("비밀번호가 일치하지 않습니다.");
			return false;
		}
		
		Date today = new Date();
		if (today.before(maturityDate)) {
			System.out.println("만기일 이전에는 직접 출금할 수 없습니다.");
			return false;
		}
		
		if (amount <= availableBalance) {
			totalBalance -= amount;
			availableBalance -= amount;
			System.out.println("출금 완료. 잔액: " + totalBalance + "원");
			return true;
		} else { //잔액부족
			System.out.println("잔액부족");
			return false;
		}
	}
	
	public boolean automaticWithdrawal(double amount) { //자동이체
		if (amount <= maxTransferAmountToChecking && amount <= availableBalance) {
			totalBalance -= amount;
			availableBalance -= amount;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void display() { //계좌 정보 출력
		System.out.println("[저축예금계좌] 계좌번호: " + accountNumber + " 잔액: " + totalBalance + " 이자율: " + interestRate);
	}
}
