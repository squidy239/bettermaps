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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.luben.zstd.Zstd.compress;
import static com.github.luben.zstd.Zstd.decompress;
import static java.util.concurrent.Executors.newScheduledThreadPool;

public class videoprocessor {

    public static void main(String[] args) throws JCodecException, IOException {
        extractFramez("C:/Users/sacha/Downloads/Rick Astley - Never Gonna Give You Up (Official Music Video) (1).mp4",720,720,40,"test3","C:/Users/sacha/Downloads/mc_server/nonmctestdir/");
    }
    //static ScheduledExecutorService vidprossesspool =  newScheduledThreadPool(12);
    public static void extractFramez(String videoFilePath, int width, int height, double frameRate, String name, String path) throws IOException, JCodecException {
        try (SeekableByteChannel channel = NIOUtils.readableFileChannel(videoFilePath)) {
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            double videoFrameRate = grab.getVideoTrack().getMeta().getTotalFrames() / grab.getVideoTrack().getMeta().getTotalDuration();
            double frameInterval = videoFrameRate / frameRate;
            try {
                for (File subfile : new File(path + name).listFiles()) {
                    subfile.delete();
                }
            } catch (Exception ignored) {
            }
            int frameNumber = 0;
            AtomicInteger savedFrameNumber = new AtomicInteger();
            Picture f;
            System.out.println(frameInterval);
            while (null != (f = grab.getNativeFrame())) {
                if (Math.round(frameNumber % frameInterval) == 0) {
                    int finalSavedFrameNumber = savedFrameNumber.get();
                    Picture finalf = f;
                    saveFrametoBytes(finalf, finalSavedFrameNumber, name, width, height, path);
                    savedFrameNumber.getAndIncrement();
                    if (finalSavedFrameNumber % 10 == 0){System.out.println(finalSavedFrameNumber);};
                }
                frameNumber++;
            }

        } finally {
            System.out.println("done");
            Files.deleteIfExists(Path.of(videoFilePath));
        }
    }

    private static void saveFrametoBytes(Picture picture, int frameNumber, String name, int width, int height, String outdir) {
        try {
            BufferedImage bufferedImage = Thumbnails.of(AWTUtil.toBufferedImage(picture))
                    .size(width, height)
                    .keepAspectRatio(false)
                    .outputQuality(1.0)
                    .outputFormat("png")
                    .asBufferedImage();

            File outputDir = new File(outdir + name);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File outputFile = new File(outputDir, frameNumber + ".png");
            ImageUtils.ConvertAndWriteMinecraftImage(bufferedImage, outputFile);
            //ImageIO.write(ImageUtils.imageToimageBytes(bufferedImage),"png",outputFile);
            //System.out.println("Saved frame_" + frameNumber + ".png");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save frame_" + frameNumber + ".png", e);
        }
    }

}
