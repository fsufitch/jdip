package info.jdip.test.loading;

import info.jdip.world.World;
import info.jdip.world.WorldImporter;
import info.jdip.world.variant.VariantManager;
import info.jdip.world.variant.data.Variant;

import java.io.InputStream;

public class NewWorldLoader {

    static public World importGameFile(InputStream inputStream) throws Exception {
        WorldImporter wi = new WorldImporter();
        World w = wi.importGame(inputStream);

        // check if variant is available; if not, inform user.
        World.VariantInfo vi = w.getVariantInfo();

        if (VariantManager.getVariant(vi.getVariantName(), vi.getVariantVersion()) == null) {
            Variant variant = VariantManager.getVariant(vi.getVariantName(), VariantManager.VERSION_NEWEST);
            if (variant == null) {
                // we don't have the variant AT ALL
                throw new RuntimeException("No variant");
            } else {
                // try most current version: HOWEVER, warn the user that it might not work
                vi.setVariantVersion(variant.getVersion());
            }
        }

        return w;
    }// readGameFile()

}
