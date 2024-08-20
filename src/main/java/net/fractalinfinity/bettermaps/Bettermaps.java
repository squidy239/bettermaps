package net.fractalinfinity.bettermaps;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.fractalinfinity.bettermaps.mapdraw.*;
import static net.fractalinfinity.bettermaps.web.runweb;

public final class Bettermaps extends JavaPlugin implements Listener {
    public static ConcurrentHashMap<Long, MapView> mapviewdict = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<long[][], List<Object>> playingmedia = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        ConcurrentHashMap<Long, Object[]> maplocin = new ConcurrentHashMap<>();
        if (new File("mapimg/maplocations.dat").exists()) {
            try {
                FileInputStream fileIn = new FileInputStream("mapimg/maplocations.dat");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                maplocin = (ConcurrentHashMap<Long, Object[]>) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e);
            }
        }
        ConcurrentHashMap<Long, Location> prossesedmapin = new ConcurrentHashMap<>();
        for (Long i : maplocin.keySet()) {
            Object[] loc = maplocin.get(i);
            System.out.println(Arrays.toString(loc));
            prossesedmapin.put(i, new Location(Bukkit.getWorld((String) loc[0]), (int) loc[1], (int) loc[2], (int) loc[3]));
        }
        mapidlocations.initialize(prossesedmapin);
        try {
            Files.createDirectories(Paths.get("mapimg/media"));
            Files.createDirectories(Paths.get("mapimg/temp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (new File("mapimg/playingmedia.dat").exists()) {
            System.out.println("media exists");
            ConcurrentHashMap<long[][], List<Object>> playingmediafile = null;
            try {
                FileInputStream fileIn = new FileInputStream("mapimg/playingmedia.dat");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                playingmediafile = (ConcurrentHashMap<long[][], List<Object>>) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e);
            }
            playingmediafile.forEach((k, v) -> {
                List<Object> data = v;
                if ((Boolean) data.getFirst()) {
                    try {
                        System.out.println(Arrays.deepToString(k));
                        if (data.get(2).equals("bytemedia")) PlayMedia((File) data.get(1), k, 20F);
                        else
                            Bettermaps.getPlugin(this.getClass()).getLogger().warning(Arrays.deepToString(k) + " is not a valid media type, media type: " + data.get(2));
                    } catch (IOException e) {
                        JavaPlugin.getPlugin(this.getClass()).getLogger().warning(data.get(1).toString() + " does not exist!");
                        playingmedia.remove(k);
                    }
                }
            });
        }
        //try {PlayMedia(new File("mapimg/vids/128"), new long[][]{{172, 173,174,175,176,177,178,179}, {180, 181,182,183,184,185,186,187},{188,189,190,191,192,193,194,195},{196,197,198,199,200,201,202,203},{204,205,206,207,208,209,210,211}});} catch (IOException e) {throw new RuntimeException(e);}
        System.out.println("started");
        try {
            runweb();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getServer().getPluginManager().registerEvents(this, this);

    }

    @EventHandler
    public void mapevent(MapInitializeEvent mapp) {
        mapviewdict.put((long) mapp.getMap().getId(), mapp.getMap());
    }

    private static boolean removemaprenderers(long[][] ids) {
        for (long[] ii : ids) {
            for (long id : ii) {
                MapView mv = mapviewdict.get(id);
                if (mv != null) {
                    List<MapRenderer> mvrendererlist = mv.getRenderers();
                    for (MapRenderer i : mvrendererlist) mv.removeRenderer(i);
                } else return true;
            }

        }
        return false;
    }

    public static void PlayMedia(File path, long[][] ids, Float fps) throws IOException {
        if (!path.exists()) throw new IOException("path " + path + " does not exist");
        ArrayList<Object> arr = new ArrayList<>(2);
        // find if any overlap in ids and playingmedia and stop playing other if there is
        Enumeration<long[][]> keys = playingmedia.keys();
        while (keys.hasMoreElements()) {
            long[][] key = keys.nextElement();
            if (isoverlap(key, ids)) {
                playingmedia.remove(key);
            }
        }
        arr.addFirst(true);
        arr.add(1, path);
        arr.add(2, "bytemedia");
        playingmedia.put(ids, arr);
        AtomicBoolean hasrenderers = new AtomicBoolean(true);
        if (path.isDirectory()) {
            Thread.ofVirtual().start(() -> {
                long frame = 0;
                List<Player> pl = GetPlayersInMapRange(flatten(ids), 32);
                while (playingmedia.containsKey(ids)) {
                    long start = System.nanoTime();
                    if (frame % 20 == 0) pl = GetPlayersInMapRange(flatten(ids), 32);
                    if (!pl.isEmpty()) {
                        if (hasrenderers.get()) {
                            hasrenderers.set(removemaprenderers(ids));
                        }
                        try {
                            float ms = 0;
                            if ((boolean) playingmedia.get(ids).getFirst()) {
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
                    }
                }
            });
        } else if (path.isFile()) {
            // is image
            Thread.ofVirtual().start(() -> {
                while (playingmedia.containsKey(ids) && (boolean) playingmedia.get(ids).getFirst()) {
                    if (hasrenderers.get()) {
                        hasrenderers.set(removemaprenderers(ids));
                    }
                    try {
                        List<Player> pl = GetPlayersInMapRange(flatten(ids), 32);
                        BufferedImage image = ImageIO.read(new FileImageInputStream(path));
                        PutMinecraftImageOnMaps(image, pl, ids);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        Bukkit.getLogger().warning(e.toString());
                    }
                }
            });

        }
    }




    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            FileOutputStream fileOut = new FileOutputStream("mapimg/playingmedia.dat");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(playingmedia);
            out.close();
            fileOut.close();
            ConcurrentHashMap<Long, Object[]> maplocout = new ConcurrentHashMap<>();
            for (long i : mapidlocations.maplocations.keySet()) {
                Object[] o = new Object[4];
                o[0] = mapidlocations.maplocations.get(i).getWorld().getName();
                o[1] = mapidlocations.maplocations.get(i).getBlockX();
                o[2] = mapidlocations.maplocations.get(i).getBlockY();
                o[3] = mapidlocations.maplocations.get(i).getBlockZ();
                maplocout.put(i, o);
            }
            FileOutputStream fileOut2 = new FileOutputStream("mapimg/maplocations.dat");
            ObjectOutputStream out2 = new ObjectOutputStream(fileOut2);
            out2.writeObject(maplocout);
            out.close();
            fileOut.close();
            for (File subfile : new File("mapimg/temp").listFiles()) {
                subfile.delete();
            }
            new File("mapimg/temp").delete();
        } catch (Exception e) {
            System.out.println(e);
        }
    }


}
