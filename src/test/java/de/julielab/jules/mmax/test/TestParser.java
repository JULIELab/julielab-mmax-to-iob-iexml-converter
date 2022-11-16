package de.julielab.jules.mmax.test;

import de.julielab.jules.mmax.MMAXParser;
import de.julielab.jules.mmax.MarkableContainer;
import de.julielab.jules.mmax.WordInformation;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestParser  {

	@Test
	public void testHandleOriginalTextInformation() throws Exception {

		WordInformation i0 = new WordInformation();
		i0.setText("Hallo");
		WordInformation i1 = new WordInformation();
		i1.setText("Welt");
		WordInformation i2 = new WordInformation();
		i2.setText(".");
		WordInformation i3 = new WordInformation();
		i3.setText("Test");
		WordInformation i4 = new WordInformation();
		i4.setText(",");
		WordInformation i5 = new WordInformation();
		i5.setText("Baum");
		WordInformation i6 = new WordInformation();
		i6.setText(":");
		WordInformation i7 = new WordInformation();
		i7.setText("Ende");
		WordInformation[] words = new WordInformation[] { i0, i1, i2, i3, i4, i5, i6, i7 };
		// String originalText = "Hallo Welt. Test, Baum : Ende";

		Method privateStringMethod = MMAXParser.class.getDeclaredMethod("handleOriginalTextInformation", String.class, WordInformation[].class);

		privateStringMethod.setAccessible(true);

		MMAXParser p = new MMAXParser();

		privateStringMethod.invoke(p, "src/test/resources/testText", words);

		assertTrue(i0.getText(), i0.isFollowedBySpace());
		assertFalse(i1.getText(), i1.isFollowedBySpace());
		assertTrue(i2.getText(), i2.isFollowedBySpace());
		assertFalse(i3.getText(), i3.isFollowedBySpace());
		assertTrue(i4.getText(), i4.isFollowedBySpace());
		assertTrue(i5.getText(), i5.isFollowedBySpace());
		assertTrue(i6.getText(), i6.isFollowedBySpace());
		assertFalse(i7.getText(), i7.isFollowedBySpace());
	}

	@Test
	public void testMarkableChoice() throws Exception {
		List<MarkableContainer> markables = new ArrayList<MarkableContainer>();
		MarkableContainer m1 = new MarkableContainer();

		m1.setBegin(0);
		m1.setEnd(10);
		m1.setPriority(3);
		m1.setId("4");
		markables.add(m1);

		m1 = new MarkableContainer();
		m1.setBegin(0);
		m1.setEnd(9);
		m1.setPriority(1);
		m1.setId("2");
		markables.add(m1);

		m1 = new MarkableContainer();
		m1.setBegin(1);
		m1.setEnd(11);
		m1.setPriority(1);
		m1.setId("3");
		markables.add(m1);

		m1 = new MarkableContainer();
		m1.setBegin(2);
		m1.setEnd(9);
		m1.setPriority(1);
		m1.setId("6");
		markables.add(m1);

		m1 = new MarkableContainer();
		m1.setBegin(0);
		m1.setEnd(10);
		m1.setPriority(100);
		m1.setId("1");
		markables.add(m1);

		m1 = new MarkableContainer();
		m1.setBegin(0);
		m1.setEnd(10);
		m1.setPriority(100);
		m1.setId("5");
		markables.add(m1);

		MMAXParser p = new MMAXParser();

		Method privateStringMethod = MMAXParser.class.getDeclaredMethod("handleMultipleMarkables", List.class);

		privateStringMethod.setAccessible(true);
		privateStringMethod.invoke(p, markables);

		for (MarkableContainer m : markables) {
			if (m.getId().equals("1")) {
				assertFalse(m.isIgnore());
			} else {
				assertTrue(m.isIgnore());
			}
		}

	}

	public void testManualRun() throws Exception {
		// MMAXParser.main(new
		// String[]{"src/test/resources/in","src/test/resources/out","src/test/resources/priolist.conf","IEXML","","proteins"});
	}

	private boolean compareFiles(String a, String b) throws Exception{
		File fa = new File(a);
		File fb = new File(b);
		FileInputStream fisa = new FileInputStream(fa);
		InputStreamReader isra = new InputStreamReader(fisa);

		FileInputStream fisb = new FileInputStream(fb);
		InputStreamReader isrb = new InputStreamReader(fisb);
		int ia =0;
		while ((ia = isra.read()) >= 0) {
			int ib = isrb.read();
			if(ia!=ib){
				return false;
			}
		}
		return true;
	}
}
