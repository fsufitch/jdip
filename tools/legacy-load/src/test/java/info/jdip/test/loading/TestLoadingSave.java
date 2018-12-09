package info.jdip.test.loading;

import dip.world.World;
import dip.world.variant.VariantManager;
import jdk.nashorn.internal.ir.annotations.Ignore;
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

    @Ignore
    @Test
    public void load_bug_147() throws Exception {
        String name = "info/jdip/legacy/saves/bug_147.jdip";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(name);
        World legacyWorld = LegacyWorldLoader.readGameFile(in);
        in = this.getClass().getClassLoader().getResourceAsStream(name);
        boolean wordsAreEqual = WorldComparator.compareWorlds(legacyWorld, NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }

    @Ignore
    @Test
    public void load_1902_winter() throws Exception {
        String name = "info/jdip/legacy/saves/1902_winter.jdip";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(name);
        World legacyWorld = LegacyWorldLoader.readGameFile(in);
        in = this.getClass().getClassLoader().getResourceAsStream(name);
        boolean wordsAreEqual = WorldComparator.compareWorlds(legacyWorld, NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }

    @Ignore
    @Test
    public void load_1905_summer_before_retreat() throws Exception {
        String name = "info/jdip/legacy/saves/1905-summer-before-retreat.jdip";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(name);
        World legacyWorld = LegacyWorldLoader.readGameFile(in);
        in = this.getClass().getClassLoader().getResourceAsStream(name);
        boolean wordsAreEqual = WorldComparator.compareWorlds(legacyWorld, NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }

    @Ignore
    @Test
    public void load_1912_fall() throws Exception {
        String name = "info/jdip/legacy/saves/1912-fall.jdip";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(name);
        World legacyWorld = LegacyWorldLoader.readGameFile(in);
        in = this.getClass().getClassLoader().getResourceAsStream(name);
        boolean wordsAreEqual = WorldComparator.compareWorlds(legacyWorld, NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }

    @Ignore
    @Test
    public void load_1913_spring() throws Exception {
        String name = "info/jdip/legacy/saves/1913-spring.jdip";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(name);
        World legacyWorld = LegacyWorldLoader.readGameFile(in);
        in = this.getClass().getClassLoader().getResourceAsStream(name);
        boolean wordsAreEqual = WorldComparator.compareWorlds(legacyWorld, NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }

    @Ignore
    @Test
    public void load_Loeb9_F2F() throws Exception {
        String name = "info/jdip/legacy/saves/Loeb9-F2F.jdip";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(name);
        World legacyWorld = LegacyWorldLoader.readGameFile(in);
        in = this.getClass().getClassLoader().getResourceAsStream(name);
        boolean wordsAreEqual = WorldComparator.compareWorlds(legacyWorld, NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }

    @Ignore
    @Test
    public void load_spring() throws Exception {
        String name = "info/jdip/legacy/saves/spring.jdip";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(name);
        World legacyWorld = LegacyWorldLoader.readGameFile(in);
        in = this.getClass().getClassLoader().getResourceAsStream(name);
        boolean wordsAreEqual = WorldComparator.compareWorlds(legacyWorld, NewWorldLoader.importGameFile(in));
        assertTrue(wordsAreEqual);
    }

}
