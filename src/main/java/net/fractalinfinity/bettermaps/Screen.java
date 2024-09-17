package net.fractalinfinity.bettermaps;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.fractalinfinity.bettermaps.mapdraw.*;

public class Screen implements Serializable {
    public int[][] ids;
    public String name;
    public short width;
    //0 is not playing, 1 is image, 2 is video
    public short playingtype = 0;
    public short height;
    public boolean videopaused = false;
    public File path;
    public Float fps;

    public Screen(String screenname,int[][] screenids){
        name = screenname;
        ids = screenids;
        height = (short) screenids.length;
        width = (short) screenids[0].length;
    }

    private boolean removemaprenderers(int[][] ids) {
        for (int[] ii : ids) {
            for (int id : ii) {
                MapView mv = Bettermaps.mapviewdict.get(id);
                if (mv != null) {
                    List<MapRenderer> mvrendererlist = mv.getRenderers();
                    for (MapRenderer i : mvrendererlist) mv.removeRenderer(i);
                } else return true;
            }

        }
        return false;
    }

    public void loadandstart() throws IOException {
        switch (playingtype){
            case 0:break;
            case 1:
            case 2:PlayVideo(path,fps);
        }
    }
    public void PlayVideo(File videopath, float videofps) throws IOException {
        if (!videopath.isDirectory())throw new IOException("videopath is not a directory");
        path = videopath;
        playingtype = 2;
        fps = videofps;
        AtomicBoolean hasrenderers = new AtomicBoolean(true);
        Thread.ofVirtual().start(() -> {
            long frame = 0;
            List<Player> pl = GetPlayersInMapRange(flatten(ids), 32);
            while (playingtype == 2) {
                 try {
                 long start = System.nanoTime();
                 if (frame % 20 == 0) pl = GetPlayersInMapRange(flatten(ids), 32);
                    //if (!pl.isEmpty()) {
                 if (hasrenderers.get()) hasrenderers.set(removemaprenderers(ids));
                 float ms = 0;
                 if (!videopaused & !pl.isEmpty()){
                            File file = new File(path, "/" + frame + ".png");
                            if (!file.exists()) {
                                frame = 0;
                                file = new File(path, "/" + frame + ".png");
                            }
                            BufferedImage image = ImageIO.read(file);
                            PutMinecraftImageOnMaps(image, pl, ids);
                            long end = System.nanoTime();
                            ms = 0F;
                            ms = (end - start) / 1000000F < 0 ? ms : 0F;
                            //System.out.println("ms: "+ms);
                            frame++;
                        }
                        Thread.sleep((long) (((1 / fps) * 1000) - ms));
                    } catch (Exception e) {
                        Bukkit.getLogger().warning(e.toString());
                    }
                //}
            }
        });
    }
    public void LoadImage(String path){}

}