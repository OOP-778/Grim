package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

@CheckData(name = "BadPacketsV")
public class BadPacketsV extends Check implements PacketCheck {
    public BadPacketsV(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            final WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            // BlockFace.OTHER is USE_ITEM for pre 1.9
            if (packet.getFace() == BlockFace.OTHER) {

                // This packet is always sent at (-1, -1, -1) at (0, 0, 0) on the block
                // except y gets wrapped?
                final int expectedY = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? 4095 : 255;

                // never sent when not holding anything
                final boolean failedItemCheck = packet.getItemStack().isPresent() && packet.getItemStack().get().isEmpty()
                        // ViaVersion can sometimes cause this part of the check to false
                        && player.getClientVersion().isOlderThan(ClientVersion.V_1_9);

                final Vector3i pos = packet.getBlockPosition();
                final Vector3f cursor = packet.getCursorPosition();

                if (failedItemCheck
                        || pos.x != -1
                        || pos.y != expectedY
                        || pos.z != -1
                        || cursor.x != 0
                        || cursor.y != 0
                        || cursor.z != 0
                ) {
                    final String verbose = String.format(
                            "xyz=%s, %s, %s, cursor=%s, %s, %s, item=%s",
                            pos.x, pos.y, pos.z, cursor.x, cursor.y, cursor.z, !failedItemCheck
                    );
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        player.onPacketCancel();
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
