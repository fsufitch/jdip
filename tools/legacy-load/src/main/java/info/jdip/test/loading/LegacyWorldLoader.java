package info.jdip.test.loading;

import dip.world.World;
import dip.world.variant.VariantManager;
import dip.world.variant.data.Variant;

import java.io.InputStream;

public class LegacyWorldLoader {

    // reads in a game file
    static public World readGameFile(InputStream inputStream)
            throws Exception {


        World w = dip.world.World.open(inputStream);

        // check if variant is available; if not, inform user.
        World.VariantInfo vi = w.getVariantInfo();

        if (VariantManager.getVariant(vi.getVariantName(), vi.getVariantVersion()) == null) {
            Variant variant = VariantManager.getVariant(vi.getVariantName(), VariantManager.VERSION_NEWEST);
            if (variant == null) {
                // we don't have the variant AT ALL
                throw new RuntimeException("No variant");
            } else {
                // try most current version: HOWEVER, warn the user that it might not work
//                ErrorDialog.displayVariantVersionMismatch(clientFrame, vi, variant.getVersion());
                vi.setVariantVersion(variant.getVersion());
            }
        }

        return w;
    }// readGameFile()
}
