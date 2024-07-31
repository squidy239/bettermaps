package net.fractalinfinity.bettermaps;

import net.coobird.thumbnailator.Thumbnails;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
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
import java.util.Arrays;

import static com.github.luben.zstd.Zstd.compress;
import static com.github.luben.zstd.Zstd.decompress;

public class videoprocessor {

    public static void main(String[] args) throws JCodecException, IOException {
        extractFramez("C:/Users/sacha/Downloads/Rick Astley - Never Gonna Give You Up (Official Music Video) (1).mp4",720,720,40,"test3","C:/Users/sacha/Downloads/mc_server/nonmctestdir/",true);
    }

    public static void extractFramez(String videoFilePath, int width, int height, double frameRate, String name, String path, boolean tobytes) throws IOException, JCodecException {
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
            int savedFrameNumber = 0;
            Picture f;
            System.out.println(frameInterval);
            while (null != (f = grab.getNativeFrame())) {
                if (Math.round(frameNumber % frameInterval) == 0) {
                    int finalSavedFrameNumber = savedFrameNumber;
                    if (tobytes)saveFrametoBytes(f, finalSavedFrameNumber, name, width, height, path);
                    else saveFrame(f, finalSavedFrameNumber, name, width, height, path);
                    savedFrameNumber++;
                    if (finalSavedFrameNumber % 10 == 0){System.out.println(finalSavedFrameNumber);}
                }
                frameNumber++;
            }

        } finally {
            System.out.println("done");
            Files.deleteIfExists(Path.of(videoFilePath));
        }
    }

    private static void saveFrame(Picture picture, int frameNumber, String name, int width, int height, String outdir) {
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
            ImageIO.write(bufferedImage, "png", outputFile);

            //System.out.println("Saved frame_" + frameNumber + ".png");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save frame_" + frameNumber + ".png", e);
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
            ImageIO.write(ImageUtils.imageToimageBytes(bufferedImage),"png",outputFile);
            //System.out.println("Saved frame_" + frameNumber + ".png");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save frame_" + frameNumber + ".png", e);
        }
    }
    public static void compressAndWriteArray(byte[][][] outputbyte, String outputFile) throws IOException {
        int depth = outputbyte.length;
        int height = outputbyte[0].length;
        int width = outputbyte[0][0].length;

        // Flatten the 3D array into a 1D array
        byte[] flatArray = flattenArray(outputbyte);

        // Compress the data
        byte[] compressedArray = compress(flatArray);

        // Write the compressed array to file
        try (FileOutputStream fileOut = new FileOutputStream(outputFile);
             DataOutputStream out = new DataOutputStream(fileOut)) {
            // Write the dimensions
            out.writeInt(depth);
            out.writeInt(height);
            out.writeInt(width);
            // Write the original length and the compressed length
            out.writeInt(flatArray.length);
            out.writeInt(compressedArray.length);
            // Write the compressed data
            out.write(compressedArray);
        }
    }

    private static byte[] flattenArray(byte[][][] array3D) {
        int depth = array3D.length;
        int height = array3D[0].length;
        int width = array3D[0][0].length;

        byte[] flatArray = new byte[depth * height * width];
        int index = 0;

        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < width; k++) {
                    flatArray[index++] = array3D[i][j][k];
                }
            }
        }

        return flatArray;
    }
        public static byte[][][] readAndDecompressArray(String inputFile) throws IOException {
            byte[] decompressedArray;
            int depth, height, width;

            // Read the compressed array from file
            try (FileInputStream fileIn = new FileInputStream(inputFile);
                 DataInputStream in = new DataInputStream(fileIn)) {
                // Read the dimensions
                depth = in.readInt();
                height = in.readInt();
                width = in.readInt();

                int originalLength = in.readInt();
                int compressedLength = in.readInt();
                byte[] compressedArray = new byte[compressedLength];
                in.readFully(compressedArray);
                // Decompress the array
                decompressedArray = decompress(compressedArray, originalLength);
            }

            // Reconstruct the 3D array
            return unflattenArray(decompressedArray, depth, height, width);
        }

        private static byte[][][] unflattenArray(byte[] flatArray, int depth, int height, int width) {
            byte[][][] array3D = new byte[depth][height][width];
            int index = 0;

            for (int i = 0; i < depth; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < width; k++) {
                        array3D[i][j][k] = flatArray[index++];
                    }
                }
            }

            return array3D;
        }
}
