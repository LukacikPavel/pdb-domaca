package sk.upjs.gursky.pdb;

import java.nio.ByteBuffer;

import sk.upjs.gursky.bplustree.BPKey;

public class SalaryKey implements BPKey<SalaryKey> {

	private static final long serialVersionUID = -6145106565419658974L;
	private int salary;

	public SalaryKey() {
	}

	public SalaryKey(int salary) {
		super();
		this.salary = salary;
	}

	@Override
	public int compareTo(SalaryKey o) {
		return salary - o.getSalary();
	}

	@Override
	public void load(ByteBuffer bb) {
		salary = bb.getInt();
	}

	@Override
	public void save(ByteBuffer bb) {
		bb.putInt(salary);
	}

	@Override
	public int getSize() {
		return 4;
	}
	
	public int getSalary() {
		return salary;
	}

	@Override
	public String toString() {
		return "SalaryKey [salary=" + salary + "]";
	}

}
