package net.fractalinfinity.bettermaps;

import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class mapdraw{
   public static void setmapimg(BufferedImage image,List<Player> pl, long id, int x, int y) {
       byte[] renderedimage = ImageUtils.imageToBytes(image);
       setmapbytes(renderedimage,pl,id,x,y);
    }

    public static List<Player> GetPlayersInMapRange(long[] ids, int distance) {
        List<Player> allplayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Player> players = new ArrayList<>();
        ConcurrentHashMap<Long, Location> maploc = mapidlocations.maplocations;
        for (Long i : ids){
                if (maploc.containsKey(i)){
                for (Player p : allplayers){
                    if (maploc.get(i).distance(p.getLocation()) < distance && !players.contains(p)){
                        players.add(p);
                }
            }
                }
        }
        return players;
    }

    public static void setmapbytes(byte[] bytes,List<Player> pl, long id,int x, int y)  {
        boolean islocked = true;
        Collection<MapDecoration> icons = new ArrayList<>();
        ClientboundMapItemDataPacket packet = new ClientboundMapItemDataPacket(new MapId((int) id), MapView.Scale.valueOf("CLOSEST").getValue(), islocked, icons, new MapItemSavedData.MapPatch(x, y, 128, 128, bytes));
        for (Player player : pl) {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        }
    }
    static long[] flatten(long[][] matrix) {
        int totalElements = 0;
        for (long[] row : matrix) {
            totalElements += row.length;
        }

        long[] result = new long[totalElements];
        int index = 0;
        for (long[] row : matrix) {
            for (long element : row) {
                result[index++] = element;
            }
        }

        return result;
    }
    public static void putimageonmaps(BufferedImage image,List<Player> pl, long[][] ids) {
        for (int l = 0; l < ids.length; l++) {
                for (int i = 0; i < ids[l].length; i++){
                    setmapimg(image.getSubimage(i * 128, l * 128, 128, 128),pl, ids[l][i], 0, 0);// System.out.println(ids[finalL][finalI] + " dosent exist");
                }
            }
    }
    public static void putbytesonmaps(BufferedImage imagebytes,List<Player> pl, long[][] ids) {
        try{
            for (int l = 0; l < ids.length; l++) {
                for (int i = 0; i < ids[l].length; i++){
                    byte[] bytes = ((DataBufferByte) imagebytes.getData(new Rectangle(i*128,l*128,128,128)).getDataBuffer()).getData();
                    setmapbytes(bytes, pl, ids[l][i],0, 0);// System.out.println(ids[finalL][finalI] + " dosent exist");
                }
            }}catch (Exception e){System.out.println("putbytesonmaps: "+e);}
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
