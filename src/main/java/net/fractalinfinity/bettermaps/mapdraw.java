package net.fractalinfinity.bettermaps;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

import static net.fractalinfinity.bettermaps.Bettermaps.imageutils;

    private static ProtocolManager protocolManager;

    public void ItemFramePacketInterceptor(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public static void registerPacketInterceptor() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                // Check if the spawned entity is an item frame
                if (packet.getEntityTypeModifier().read(0) == EntityType.ITEM_FRAME) {
                    int entityId = packet.getIntegers().read(0);
                    UUID uniqueId = packet.getUUIDs().read(0);
                    double x = packet.getDoubles().read(0);
                    double y = packet.getDoubles().read(1);
                    double z = packet.getDoubles().read(2);

                    // Log or process the item frame data
                    plugin.getLogger().info("Item frame spawned: ID " + entityId + " at (" + x + ", " + y + ", " + z + ")");
                    // You can modify the packet here if needed
                    // For example, changing the item frame's position:
                    // packet.getDoubles().write(0, newX);

                    // If you need to access or modify the item in the frame,
                    // you'll need to intercept a separate packet (ENTITY_METADATA)
                    // that is sent immediately after this one.
                }
            }
        });

        // Intercept the ENTITY_METADATA packet to get/modify the item in the frame
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                int entityId = packet.getIntegers().read(0);

                // You would need to keep track of item frame entity IDs to know if this packet is for an item frame
                // For this example, we'll process all ENTITY_METADATA packets
                WrappedDataWatcher watcher = new WrappedDataWatcher(packet.getWatchableCollectionModifier().read(0));

                Optional<WrappedWatchableObject> itemObject = watcher.getWatchableObjects().stream()
                        .filter(wrapper -> wrapper.getIndex() == 8) // Index 8 is typically for the item in item frames
                        .findFirst();

                if (itemObject.isPresent()) {
                    Object item = watcher.getObject(itemObject.get().getIndex());
                    // Process or modify the item here
                    plugin.getLogger().info("Item frame " + entityId + " contains item: " + item);

                    // If you want to modify the item:
                    // WrappedDataWatcher.WrappedDataWatcherObject watcherObject = itemObject.get();
                    // watcher.setObject(watcherObject.getIndex(), newItemStack);
                    // packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
                }
            }
        });
    }

    public void unregisterPacketInterceptor() {
        protocolManager.removePacketListeners(plugin);
    }
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
    public static List<Entity> getNearbyEntities(net.minecraft.world.entity.Entity e, double x, double y, double z) {
        Preconditions.checkState(!e.generation, "Cannot get nearby entities during world generation");
        List<net.minecraft.world.entity.Entity> notchEntityList = e.level().getEntities(e, e.getBoundingBox().inflate(x, y, z), Predicates.alwaysTrue());
        List<Entity> bukkitEntityList = new ArrayList(notchEntityList.size());
        Iterator var9 = notchEntityList.iterator();
        while(var9.hasNext()) {
            net.minecraft.world.entity.Entity en = (net.minecraft.world.entity.Entity)var9.next();
            bukkitEntityList.add(en.getBukkitEntity());
        }

        return bukkitEntityList;
    }

    public static List<Player> getplayersinmaprange(long[] ids, int x, int y, int z) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        return players.stream()
                .filter(player -> {
                    List<Entity> Entities = ;
                    return Entities.stream()
                            .filter(entity -> entity instanceof ItemFrame)
                            .map(entity -> (ItemFrame) entity)
                            .filter(frame -> frame.getItem().getType() == Material.FILLED_MAP)
                            .map(frame -> ((MapMeta) frame.getItem().getItemMeta()).getMapId())
                            .anyMatch(mapId -> ArrayUtils.contains(ids, mapId));
                })
                .collect(Collectors.toList());
    }

    public static void setmapbytes(BufferedImage imagebytes, long id,List<Player> pl,int x, int y) {
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
    private static long[] flatten(long[][] matrix) {
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
    public static void putimageonmaps(BufferedImage image, long[][] ids) {
            for (int l = 0; l < ids.length; l++) {
                for (int i = 0; i < l; i++){
                    setmapimg(image.getSubimage(i * 128, l * 128, 128, 128), ids[l][i], 0, 0);// System.out.println(ids[finalL][finalI] + " dosent exist");
                };
            }
    }

    public static void putbytesonmaps(BufferedImage imagebytes, long[][] ids) {
        List<Player> pl = getplayersinmaprange(flatten(ids),4,4,4);
            try{
            for (int l = 0; l < ids.length; l++) {
                for (int i = 0; i < ids[l].length; i++){
                    setmapbytes(imagebytes.getSubimage(i*128,l*128,128,128), ids[l][i], pl,0, 0);// System.out.println(ids[finalL][finalI] + " dosent exist");
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
