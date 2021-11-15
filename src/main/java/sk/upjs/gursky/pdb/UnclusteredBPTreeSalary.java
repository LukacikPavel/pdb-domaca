package sk.upjs.gursky.pdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import sk.upjs.gursky.bplustree.BPTree;

public class UnclusteredBPTreeSalary extends BPTree<SalaryKey, SalaryOffsetEntry> {

	public static final int PAGE_SIZE = 4096;

	private File personsFile;

	private UnclusteredBPTreeSalary(File personFile, File indexFile) {
		super(SalaryOffsetEntry.class, indexFile);
		this.personsFile = personFile;
	}

	public static UnclusteredBPTreeSalary newTreeBulkLoading(File personFile, File indexFile) throws IOException {
		UnclusteredBPTreeSalary tree = new UnclusteredBPTreeSalary(personFile, indexFile);
		tree.setNodeSize(PAGE_SIZE);
		RandomAccessFile raf = new RandomAccessFile(personFile, "rw");

		FileChannel channel = raf.getChannel();
		ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);

		long fileSize = personFile.length();

		List<SalaryOffsetEntry> pairs = new ArrayList<>();
		for (int offset = 0; offset < fileSize; offset += PAGE_SIZE) {
//			System.out.println("Čítam " + (offset / PAGE_SIZE) + ". stránku");

			buffer.clear();
			channel.read(buffer, offset);
			buffer.rewind();

			int personsCount = buffer.getInt();

			for (int i = 0; i < personsCount; i++) {
				PersonEntry personEntry = new PersonEntry();
				personEntry.load(buffer);
				pairs.add(new SalaryOffsetEntry(personEntry.salary, offset + 4 + i * personEntry.getSize()));
			}
		}
		channel.close();
		raf.close();
		Collections.sort(pairs);
		tree.openAndBatchUpdate(pairs.iterator(), pairs.size());

		return tree;
	}

	public List<PersonEntry> intervalQueryEntries(SalaryKey low, SalaryKey high) throws IOException {
		List<SalaryOffsetEntry> pairs = super.intervalQuery(low, high);
		RandomAccessFile raf = new RandomAccessFile(personsFile, "rw");

		FileChannel channel = raf.getChannel();
		ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);

		List<PersonEntry> entries = new LinkedList<>();
		for (SalaryOffsetEntry pair : pairs) {
			buffer.clear();
			long pageOffset = (pair.offset / PAGE_SIZE) * PAGE_SIZE;
			int bufferOffset = (int) (pair.getOffset() - pageOffset);
			channel.read(buffer, pageOffset);
			buffer.position(bufferOffset);

			PersonEntry personEntry = new PersonEntry();
			personEntry.load(buffer);
			entries.add(personEntry);
		}

		channel.close();
		raf.close();
		return entries;
	}

}
