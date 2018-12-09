package info.jdip.test.loading;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLoadingSave {

    @Test
    public void loadSave() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("info/jdip/legacy/saves/bug_147.jdip");
        boolean wordsAreEqual = WorldComparator.compareWorlds(LegacyWorldLoader.readGameFile(in), NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }
}
