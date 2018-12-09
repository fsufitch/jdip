package info.jdip.test.loading;

import dip.world.World;
import dip.world.variant.VariantManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLoadingSave {
    @BeforeAll
    public static void initVariants() throws Exception{
        File file = new File("../../jdip_168r1/build/tmp/variants");
        VariantManager.init(new File[]{file},false);
        info.jdip.world.variant.VariantManager.init(new File[]{file},false);
    }

    @Test
    public void loadSave() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("info/jdip/legacy/saves/bug_147.jdip");
        World legacyWorld = LegacyWorldLoader.readGameFile(in);
        in = this.getClass().getClassLoader().getResourceAsStream("info/jdip/legacy/saves/bug_147.jdip");
        boolean wordsAreEqual = WorldComparator.compareWorlds(legacyWorld, NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }
}
