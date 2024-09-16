package net.fractalinfinity.bettermaps;


import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

;

public class videoprocessor {

    public static void main(String[] args) throws IOException {
        ExtractFrames("C:/Users/sacha/Downloads/rickroll.mp4", 720, 720, 40, "test3", "C:/Users/sacha/Downloads/mc_server/nonmctestdir/");
    }

    //static ScheduledExecutorService vidprossesspool =  newScheduledThreadPool(12);
    public static void ExtractFrames(String videoFilePath, int width, int height, double frameRate, String name, String path) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFilePath)) {
            FFmpegFrameFilter framefilter = new FFmpegFrameFilter("fps=fps=" + frameRate+",scale=" + width + ":" + height,grabber.getImageWidth(),grabber.getImageHeight());
            framefilter.setPixelFormat(grabber.getPixelFormat());
            int frameNumber = 0;
            org.bytedeco.javacv.Frame frame;
            grabber.start();
            framefilter.start();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            while ((frame = grabber.grabFrame()) != null) {
                framefilter.push(frame);
                Frame pull = framefilter.pull();
                if (pull == null || pull.samples == null || pull.image == null) continue;
                BufferedImage image = converter.getBufferedImage(pull);
                File outputDir = new File(path + name);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                File outputFile = new File(outputDir, frameNumber + ".png");
                ImageUtils.ConvertAndWriteMinecraftImage(image, outputFile);
                frameNumber++;
            }

            grabber.stop();
            grabber.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



        /*@Deprecated
        private static void saveFrametoPng8(BufferedImage image, long frameNumber, String name, int width, int height, String outdir) throws IOException {
            BufferedImage bufferedImage = Thumbnails.of(image)
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
    }
    @Deprecated
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
            AtomicLong savedFrameNumber = new AtomicLong();
            Picture f;
            System.out.println(frameInterval);
            while (null != (f = grab.getNativeFrame())) {
                if (Math.round(frameNumber % frameInterval) == 0) {
                    long finalSavedFrameNumber = savedFrameNumber.get();
                    saveFrametoPng8(AWTUtil.toBufferedImage(f), finalSavedFrameNumber, name, width, height, path);
                    savedFrameNumber.getAndIncrement();
                    if (finalSavedFrameNumber % 20 == 0) {
                        int finalFrameNumber = frameNumber;
                        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage("Video upload frame: " + finalFrameNumber));

                    }
                }
                frameNumber++;
            }

        } catch (JCodecException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("done");
            Files.deleteIfExists(Path.of(videoFilePath));
        }
    }*/
}




