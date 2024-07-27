package net.fractalinfinity.bettermaps;

import net.coobird.thumbnailator.Thumbnails;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class videoprocessor {
    public static void extractFramez(String videoFilePath, int width, int height, double frameRate, String id, String path) throws
            IOException, JCodecException {
        try (SeekableByteChannel channel = NIOUtils.readableFileChannel(videoFilePath)) {
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            double videoFrameRate = grab.getVideoTrack().getMeta().getTotalFrames() / grab.getVideoTrack().getMeta().getTotalDuration();
            int frameInterval = (int) Math.round(videoFrameRate / frameRate);
            try {
                for (File subfile : new File(path + id).listFiles()) {
                    subfile.delete();
                }
            } catch (Exception ignored) {
            }
            int frameNumber = 0;
            int savedFrameNumber = 0;
            Picture f;
            int totalframenum = grab.getVideoTrack().getMeta().getTotalFrames();
            while (null != (f = grab.getNativeFrame())) {
                if (frameNumber % frameInterval == 0) {
                    int finalSavedFrameNumber = savedFrameNumber;
                    saveFrame(f, finalSavedFrameNumber, id, width, height, path);
                    savedFrameNumber++;
                }
                frameNumber++;
            }

        } finally {
            System.out.println("done");
            Files.deleteIfExists(Path.of(videoFilePath));
        }
    }

    private static void saveFrame(Picture picture, int frameNumber, String id, int width, int height, String outdir) {
        try {
            BufferedImage bufferedImage = Thumbnails.of(AWTUtil.toBufferedImage(picture))
                    .size(width, height)
                    .keepAspectRatio(false)
                    .outputQuality(1.0)
                    .outputFormat("png")
                    .asBufferedImage();

            File outputDir = new File(outdir + id);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File outputFile = new File(outputDir, frameNumber + ".png");
            ImageIO.write(bufferedImage, "png", outputFile);

            //System.out.println("Saved frame_" + frameNumber + ".png");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save frame_" + frameNumber + ".png", e);
        }
    }
}
