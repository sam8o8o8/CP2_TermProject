package termProject;

//당좌예금계좌
public class CheckingAccount extends Account {
	private SavingsAccount linkedSavings; //잔액이 모자랄 때, 자동이체가 가능하도록 연계된 저축예금계좌
	
	//생성자
	public CheckingAccount(Customer owner, String accountNumber, String password) {
		super(owner, accountNumber, "Checking", password);
	}
	
	//연결될 저축예금계좌 설정
	public void setLinkedSavings(SavingsAccount savings) {
		this.linkedSavings = savings;
	}
	
	//출금
	@Override
	public boolean withdraw(double amount, String inputPassword) {
		if (!checkPassword(inputPassword)) { //비밀번호가 일치하지 않을 경우
			System.out.println("비밀번호가 일치하지 않습니다.");
			return false;
		}
		
		if (amount <= availableBalance) {
			totalBalance -= amount;
			availableBalance -= amount;
			System.out.println("출금 완료. 잔액: " + totalBalance + "원");
			return true;
		} else if (linkedSavings != null) { //잔액부족
			double shortage = amount - availableBalance; //모자란 금액 계산
			if (shortage <= linkedSavings.getMaxTransferAmountToChecking() && linkedSavings.automaticWithdrawal(shortage)) { //자동이체 메서드 호출
				deposit(shortage);
				return super.withdraw(amount, inputPassword); //연결된 저축예금계좌로부터 자동이체한도 내에서 모자란 만큼 자동이체
			}
			return false;
		} else {
			System.out.println("잔액 부족 및 자동이체 불가");
			return false;
		}
	}
	
	@Override
	public String display() { //계좌 정보 출력
		return "[당좌예금계좌] 계좌번호: " + accountNumber + " 잔액: " + totalBalance;
	}
}
