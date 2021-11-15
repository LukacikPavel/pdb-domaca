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

public class UnclusteredBPTree extends BPTree<PersonStringKey, SurnameAndOffsetEntry> {

	public static final int PAGE_SIZE = 4096;

	private File personsFile;

	private UnclusteredBPTree(File personFile, File indexFile) {
		super(SurnameAndOffsetEntry.class, indexFile);
		this.personsFile = personFile;
	}

	public static UnclusteredBPTree newTreeBulkLoading(File personFile, File indexFile) throws IOException {
		UnclusteredBPTree tree = new UnclusteredBPTree(personFile, indexFile);
		tree.setNodeSize(PAGE_SIZE);
		RandomAccessFile raf = new RandomAccessFile(personFile, "rw");

		FileChannel channel = raf.getChannel();
		ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);

		long fileSize = personFile.length();

		List<SurnameAndOffsetEntry> pairs = new ArrayList<>();
		for (int offset = 0; offset < fileSize; offset += PAGE_SIZE) {
//			System.out.println("Čítam " + (offset / PAGE_SIZE) + ". stránku");

			buffer.clear();
			channel.read(buffer, offset);
			buffer.rewind();

			int personsCount = buffer.getInt();

			for (int i = 0; i < personsCount; i++) {
				PersonEntry personEntry = new PersonEntry();
				personEntry.load(buffer);
				pairs.add(new SurnameAndOffsetEntry(personEntry.surname, offset + 4 + i * personEntry.getSize()));
			}
		}
		channel.close();
		raf.close();
		Collections.sort(pairs);
		tree.openAndBatchUpdate(pairs.iterator(), pairs.size());

		return tree;
	}

	public List<PersonEntry> intervalQueryEntries(PersonStringKey low, PersonStringKey high) throws IOException {
		List<SurnameAndOffsetEntry> pairs = super.intervalQuery(low, high);
		RandomAccessFile raf = new RandomAccessFile(personsFile, "rw");

		FileChannel channel = raf.getChannel();
		ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);

		List<PersonEntry> entries = new LinkedList<>();
		for (SurnameAndOffsetEntry pair : pairs) {
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
