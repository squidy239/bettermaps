package net.fractalinfinity.bettermaps;

import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.fractalinfinity.bettermaps.Bettermaps.imageutils;

public class mapdraw {

    public static void setmapimg(BufferedImage image, long id, int x, int y) {
        List<Player> pl = (List<Player>) Bukkit.getOnlinePlayers();
        boolean islocked = true;
        for (int i = 0; i < pl.size(); i++) {
            if (((CraftPlayer) pl.get(i)).getHandle().connection != null) {
                Collection<MapDecoration> icons = new ArrayList();
                byte[] renderedimage = imageutils.imageToBytes(image);
                ClientboundMapItemDataPacket packet = new ClientboundMapItemDataPacket(new MapId((int) id), MapView.Scale.valueOf("CLOSEST").getValue(), islocked, icons, new MapItemSavedData.MapPatch(x, y, 128, 128, renderedimage));
                ((CraftPlayer) pl.get(i)).getHandle().connection.send(packet);
            }
        }
    }

    public static void setmapbytes(BufferedImage imagebytes, long id, int x, int y) {
        List<Player> pl = (List<Player>) Bukkit.getOnlinePlayers();
        int[] pixels = new int[imagebytes.getWidth() * imagebytes.getHeight()];
        imagebytes.getRGB(0, 0, imagebytes.getWidth(), imagebytes.getHeight(), pixels, 0, imagebytes.getWidth());
        byte[] bytes = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++)bytes[i] = (byte) pixels[i];
        boolean islocked = true;
        for (int i = 0; i < pl.size(); i++) {
            if (((CraftPlayer) pl.get(i)).getHandle().connection != null && !pl.get(i).getDisplayName().equals(".gabeisthebest13")) {
                Collection<MapDecoration> icons = new ArrayList<>();
                ClientboundMapItemDataPacket packet = new ClientboundMapItemDataPacket(new MapId((int) id), MapView.Scale.valueOf("CLOSEST").getValue(), islocked, icons, new MapItemSavedData.MapPatch(x, y, 128, 128,bytes));
                ((CraftPlayer) pl.get(i)).getHandle().connection.send(packet);
            }
        }
    }


    public static void putimageonmaps(BufferedImage image, long[][] ids) {
            for (int l = 0; l < ids.length; l++) {
                for (int i = 0; i < l; i++){
                    setmapimg(image.getSubimage(i * 128, l * 128, 128, 128), ids[l][i], 0, 0);// System.out.println(ids[finalL][finalI] + " dosent exist");
                };
            }
    }

    public static void putbytesonmaps(BufferedImage imagebytes, long[][] ids) {
            try{
            for (int l = 0; l < ids.length; l++) {
                for (int i = 0; i < ids[l].length; i++){
                    setmapbytes(imagebytes.getSubimage(l,i,128,128), ids[l][i], 0, 0);// System.out.println(ids[finalL][finalI] + " dosent exist");
                };
            }}catch (Exception e){System.out.println(e);}
    }
    public static boolean isoverlap(long[][] first, long[][] second) {
        if (first == null || second == null || first.length == 0 || second.length == 0) {
            return false;
        }

        int firstRows = first.length;
        int firstCols = first[0].length;
        int secondRows = second.length;
        int secondCols = second[0].length;

        for (int i = 0; i < firstRows; i++) {
            for (int j = 0; j < firstCols; j++) {
                for (int k = 0; k < secondRows; k++) {
                    for (int l = 0; l < secondCols; l++) {
                        if (first[i][j] == second[k][l]) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
