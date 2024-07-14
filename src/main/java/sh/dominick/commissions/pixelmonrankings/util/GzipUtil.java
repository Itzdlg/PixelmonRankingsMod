package sh.dominick.commissions.pixelmonrankings.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

public class GzipUtil {
    private GzipUtil() {}

    public static void gzip(File source, File target) {
        try (GZIPOutputStream gos = new GZIPOutputStream(
                Files.newOutputStream(target.toPath()))) {

            Files.copy(source.toPath(), gos);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
