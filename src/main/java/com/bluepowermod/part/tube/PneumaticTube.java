/*
 * This file is part of Blue Power. Blue Power is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Blue Power is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along
 * with Blue Power. If not, see <http://www.gnu.org/licenses/>
 */
package com.bluepowermod.part.tube;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.common.util.ForgeDirection;

import com.bluepowermod.api.tube.IPneumaticTube.TubeColor;
import com.bluepowermod.api.tube.ITubeConnection;
import com.bluepowermod.client.renderers.IconSupplier;
import com.bluepowermod.helper.IOHelper;
import com.bluepowermod.helper.PartCache;
import com.bluepowermod.helper.TileEntityCache;
import com.bluepowermod.init.BPItems;
import com.bluepowermod.init.Config;
import com.bluepowermod.items.ItemDamageableColorableOverlay;
import com.bluepowermod.part.BPPart;
import com.bluepowermod.util.Color;
import com.qmunity.lib.client.render.RenderHelper;
import com.qmunity.lib.part.IPartTicking;
import com.qmunity.lib.part.compat.MultipartCompatibility;
import com.qmunity.lib.raytrace.QMovingObjectPosition;
import com.qmunity.lib.vec.Vec3d;
import com.qmunity.lib.vec.Vec3dCube;
import com.qmunity.lib.vec.Vec3i;

/**
 *
 * @author MineMaarten
 */

public class PneumaticTube extends BPPart implements IPartTicking {

    public final boolean[] connections = new boolean[6];
    /**
     * true when != 2 connections, when this is true the logic doesn't have to 'think' which way an item should go.
     */
    public boolean isCrossOver;
    protected final Vec3dCube sideBB = new Vec3dCube(AxisAlignedBB.getBoundingBox(0.25, 0, 0.25, 0.75, 0.25, 0.75));
    private TileEntityCache tileCache;
    private PartCache<PneumaticTube> partCache;
    private final TubeColor[] color = { TubeColor.NONE, TubeColor.NONE, TubeColor.NONE, TubeColor.NONE, TubeColor.NONE, TubeColor.NONE };
    private final TubeLogic logic = new TubeLogic(this);
    public boolean initialized; // workaround to the connections not properly initialized, but being tried to be used.
    private int tick;

    // private final ResourceLocation tubeSideTexture = new ResourceLocation(Refs.MODID + ":textures/blocks/Tubes/pneumatic_tube_side.png");
    // private final ResourceLocation tubeNodeTexture = new ResourceLocation(Refs.MODID + ":textures/blocks/Tubes/tube_end.png");

    @Override
    public String getType() {

        return "pneumaticTube";
    }

    @Override
    public String getUnlocalizedName() {

        return "pneumaticTube";
    }

    @Override
    public void addCollisionBoxesToList(List<Vec3dCube> boxes, Entity entity) {

        boxes.addAll(getSelectionBoxes());
    }

    /**
     * Gets all the selection boxes for this block
     *
     * @return A list with the selection boxes
     */
    @Override
    public List<Vec3dCube> getSelectionBoxes() {

        return getTubeBoxes();
    }

    private List<Vec3dCube> getTubeBoxes() {

        List<Vec3dCube> aabbs = getOcclusionBoxes();
        for (int i = 0; i < 6; i++) {
            if (connections[i]) {
                ForgeDirection d = ForgeDirection.getOrientation(i);
                Vec3dCube c = sideBB.clone().rotate(d, Vec3d.center);
                aabbs.add(c);
            }
        }
        return aabbs;
    }

    /**
     * Gets all the occlusion boxes for this block
     *
     * @return A list with the occlusion boxes
     */
    @Override
    public List<Vec3dCube> getOcclusionBoxes() {

        List<Vec3dCube> aabbs = new ArrayList<Vec3dCube>();
        aabbs.add(new Vec3dCube(0.25, 0.25, 0.25, 0.75, 0.75, 0.75));
        return aabbs;
    }

    @Override
    public void update() {

        if (initialized)
            logic.update();
        if (tick++ == 3) {
            clearCache();
            updateConnections();
        } else if (tick == 40) {
            sendUpdatePacket();
        }
        if (getWorld().isRemote && tick % 40 == 0)
            clearCache();// reset on the client, as it doesn't get update on neighbor block updates (as the
        // method isn't called on the client)
    }

    /**
     * Event called whenever a nearby block updates
     */
    @Override
    public void onUpdate() {

        if (getWorld() != null) {
            clearCache();
            updateConnections();
        }
    }

    public TileEntity getTileCache(ForgeDirection d) {

        if (tileCache == null) {
            tileCache = new TileEntityCache(getWorld(), getX(), getY(), getZ());
        }
        return tileCache.getValue(d);
    }

    public PneumaticTube getPartCache(ForgeDirection d) {

        if (partCache == null) {
            partCache = new PartCache(getWorld(), getX(), getY(), getZ(), PneumaticTube.class);
        }
        return partCache.getValue(d);
    }

    public void clearCache() {

        tileCache = null;
        partCache = null;
    }

    public TubeLogic getLogic() {

        return logic;
    }

    protected boolean canConnectToInventories() {

        return true;
    }

    private void updateConnections() {

        if (getWorld() != null && !getWorld().isRemote) {
            int connectionCount = 0;
            boolean clearedCache = false;
            clearCache();
            for (int i = 0; i < 6; i++) {
                boolean oldState = connections[i];
                ForgeDirection d = ForgeDirection.getOrientation(i);
                TileEntity neighbor = getTileCache(d);
                connections[i] = IOHelper.canInterfaceWith(neighbor, d.getOpposite(), this, canConnectToInventories());

                if (!connections[i])
                    connections[i] = neighbor instanceof ITubeConnection && ((ITubeConnection) neighbor).isConnectedTo(d.getOpposite());
                if (connections[i]) {
                    connections[i] = isConnected(d, null);
                }
                if (connections[i])
                    connectionCount++;
                if (!clearedCache && oldState != connections[i]) {
                    if (Config.enableTubeCaching)
                        getLogic().clearNodeCaches();
                    clearedCache = true;
                }
            }
            isCrossOver = connectionCount != 2;
            sendUpdatePacket();
        }
        initialized = true;
    }

    public boolean isConnected(ForgeDirection dir, PneumaticTube otherTube) {

        if (otherTube != null) {
            if (!(this instanceof Accelerator) && this instanceof MagTube != otherTube instanceof MagTube
                    && !(otherTube instanceof Accelerator))
                return false;
            TubeColor otherTubeColor = otherTube.getColor(dir.getOpposite());
            if (otherTubeColor != TubeColor.NONE && getColor(dir) != TubeColor.NONE && getColor(dir) != otherTubeColor)
                return false;
        }
        return getWorld() == null
                || !MultipartCompatibility.checkOcclusion(getWorld(), getX(), getY(), getZ(), sideBB.clone().rotate(dir, Vec3d.center));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {

        writeUpdateToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        readUpdateFromNBT(tag);
    }

    @Override
    public void writeUpdateToNBT(NBTTagCompound tag) {

        for (int i = 0; i < 6; i++) {
            tag.setBoolean("connections" + i, connections[i]);
        }
        for (int i = 0; i < color.length; i++) {
            tag.setByte("tubeColor" + i, (byte) color[i].ordinal());
        }

        NBTTagCompound logicTag = new NBTTagCompound();
        logic.writeToNBT(logicTag);
        tag.setTag("logic", logicTag);
    }

    @Override
    public void readUpdateFromNBT(NBTTagCompound tag) {

        int connectionCount = 0;
        for (int i = 0; i < 6; i++) {
            connections[i] = tag.getBoolean("connections" + i);
            if (connections[i])
                connectionCount++;
        }
        isCrossOver = connectionCount != 2;
        for (int i = 0; i < color.length; i++) {
            color[i] = TubeColor.values()[tag.getByte("tubeColor" + i)];
        }
        if (getParent() != null && getWorld() != null)
            getWorld().markBlockRangeForRenderUpdate(getX(), getY(), getZ(), getX(), getY(), getZ());

        NBTTagCompound logicTag = tag.getCompoundTag("logic");
        logic.readFromNBT(logicTag);
    }

    /**
     * Event called when the part is activated (right clicked)
     *
     * @param player
     *            Player that right clicked the part
     * @param item
     *            Item that was used to click it
     * @return Whether or not an action occurred
     */
    @Override
    public boolean onActivated(EntityPlayer player, QMovingObjectPosition mop, ItemStack item) {

        if (getWorld() == null)
            return false;

        if (!getWorld().isRemote) {

            if (item != null && item.getItem() == BPItems.paint_brush
                    && ((ItemDamageableColorableOverlay) BPItems.paint_brush).tryUseItem(item)) {

                List<Vec3dCube> boxes = getTubeBoxes();
                Vec3dCube box = mop.getCube();
                int face = -1;
                if (box.equals(boxes.get(0))) {
                    face = mop.sideHit;
                } else {
                    face = getSideFromAABBIndex(boxes.indexOf(box));
                }
                color[face] = TubeColor.values()[item.getItemDamage()];
                updateConnections();
                getLogic().clearNodeCaches();
                notifyUpdate();
                return true;

            }
        }
        return false;
    }

    @Override
    public List<ItemStack> getDrops() {

        List<ItemStack> drops = super.getDrops();
        for (TubeStack stack : logic.tubeStacks) {
            drops.add(stack.stack);
        }
        return drops;
    }

    /**
     * How 'dense' the tube is to the pathfinding algorithm. Is altered in the RestrictionTube
     *
     * @return
     */
    public int getWeigth() {

        return 1;
    }

    public TubeColor getColor(ForgeDirection dir) {

        return color[dir.ordinal()];
    }

    @Override
    public void renderDynamic(Vec3d translation, double delta, int pass) {

        if (pass == 1) {
            logic.renderDynamic(translation, (float) delta);
        }

        if (pass == 1 && !shouldRenderNode()) {
            renderSide();
        }
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {

        Tessellator t = Tessellator.instance;
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        t.startDrawingQuads();

        connections[2] = true;
        connections[3] = true;

        List<Vec3dCube> aabbs = getTubeBoxes();

        // renderMiddle(aabbs.get(0), getSideIcon());
        for (int i = 1; i < aabbs.size(); i++) {
            Vec3dCube aabb = aabbs.get(i);
            IIcon icon = !(this instanceof RestrictionTube) ? getNodeIcon()
                    : this instanceof RestrictionTubeOpaque ? IconSupplier.pneumaticTubeOpaqueNode : IconSupplier.pneumaticTubeNode;
            if (icon != null) {
                if (aabb.getMinZ() == 0) {
                    double minX = icon.getInterpolatedU(aabb.getMinX() * 16);
                    double maxX = icon.getInterpolatedU(aabb.getMaxX() * 16);
                    double minY = icon.getInterpolatedV(aabb.getMinY() * 16);
                    double maxY = icon.getInterpolatedV(aabb.getMaxY() * 16);

                    t.setNormal(0, 0, -1);

                    t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMinZ(), minX, maxY);// minZ
                    t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), maxX, maxY);
                    t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMinZ(), maxX, minY);
                    t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMinZ(), minX, minY);

                    t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMinZ(), minX, maxY);// minZ
                    t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMinZ(), minX, minY);
                    t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMinZ(), maxX, minY);
                    t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), maxX, maxY);
                }

                if (aabb.getMaxZ() == 1) {
                    double minX = icon.getInterpolatedU(aabb.getMinX() * 16);
                    double maxX = icon.getInterpolatedU(aabb.getMaxX() * 16);
                    double minY = icon.getInterpolatedV(aabb.getMinY() * 16);
                    double maxY = icon.getInterpolatedV(aabb.getMaxY() * 16);
                    t.setNormal(0, 0, 1);

                    t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMaxZ(), minX, minY);
                    t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMaxZ(), minX, maxY);// maxZ
                    t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMaxZ(), maxX, maxY);
                    t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ(), maxX, minY);

                    t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMaxZ(), minX, minY);
                    t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ(), maxX, minY);
                    t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMaxZ(), maxX, maxY);
                    t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMaxZ(), minX, maxY);// maxZ
                }
            }

            icon = getSideIcon();
            if (!connections[0]) {
                double minX = icon.getInterpolatedU(aabb.getMinX() * 16);
                double maxX = icon.getInterpolatedU(aabb.getMaxX() * 16);
                double minZ = icon.getInterpolatedV(aabb.getMinZ() * 16);
                double maxZ = icon.getInterpolatedV(aabb.getMaxZ() * 16);
                t.setNormal(0, -1, 0);

                t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), maxX, maxZ);// bottom
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMinZ(), minX, maxZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMaxZ(), minX, minZ);
                t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMaxZ(), maxX, minZ);

                t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), maxX, maxZ);// bottom
                t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMaxZ(), maxX, minZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMaxZ(), minX, minZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMinZ(), minX, maxZ);
            }

            if (!connections[1]) {
                double minX = icon.getInterpolatedU(aabb.getMinX() * 16);
                double maxX = icon.getInterpolatedU(aabb.getMaxX() * 16);
                double minZ = icon.getInterpolatedV(aabb.getMinZ() * 16);
                double maxZ = icon.getInterpolatedV(aabb.getMaxZ() * 16);
                t.setNormal(0, 1, 0);

                t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMinZ(), maxX, minZ);// top
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMinZ(), minX, minZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ(), minX, maxZ);
                t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMaxZ(), maxX, maxZ);

                t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMinZ(), minX, minZ);// top
                t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMaxZ(), minX, maxZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ(), maxX, maxZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMinZ(), maxX, minZ);
            }

            if (!connections[4]) {
                double minY = icon.getInterpolatedU(aabb.getMinY() * 16);
                double maxY = icon.getInterpolatedU(aabb.getMaxY() * 16);
                double minZ = icon.getInterpolatedV(aabb.getMinZ() * 16);
                double maxZ = icon.getInterpolatedV(aabb.getMaxZ() * 16);
                t.setNormal(-1, 0, 0);

                t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), minY, minZ);// minX
                t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMinZ(), maxY, minZ);
                t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMaxZ(), maxY, maxZ);
                t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMaxZ(), minY, maxZ);

                t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), maxY, minZ);// minX
                t.addVertexWithUV(aabb.getMinX(), aabb.getMinY(), aabb.getMaxZ(), maxY, maxZ);
                t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMaxZ(), minY, maxZ);
                t.addVertexWithUV(aabb.getMinX(), aabb.getMaxY(), aabb.getMinZ(), minY, minZ);
            }

            if (!connections[5]) {
                double minY = icon.getInterpolatedU(aabb.getMinY() * 16);
                double maxY = icon.getInterpolatedU(aabb.getMaxY() * 16);
                double minZ = icon.getInterpolatedV(aabb.getMinZ() * 16);
                double maxZ = icon.getInterpolatedV(aabb.getMaxZ() * 16);
                t.setNormal(1, 0, 0);

                t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMinZ(), minY, maxZ);// maxX
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMinZ(), maxY, maxZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ(), maxY, minZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMaxZ(), minY, minZ);

                t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMinZ(), minY, maxZ);// maxX
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMinY(), aabb.getMaxZ(), minY, minZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ(), maxY, minZ);
                t.addVertexWithUV(aabb.getMaxX(), aabb.getMaxY(), aabb.getMinZ(), maxY, maxZ);
            }
        }
        t.draw();
        renderSide();
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);
    }

    protected void renderMiddle(Vec3dCube aabb, RenderHelper renderer) {

        IIcon colorIcon = null;
        ForgeDirection d = ForgeDirection.UNKNOWN;

        if (shouldRenderNode()) {
            if (getNodeIcon() != null)
                renderTexturedCuboid(aabb, getNodeIcon(), ForgeDirection.UNKNOWN, renderer);
            colorIcon = IconSupplier.pneumaticTubeColorNode;
        } else {
            ForgeDirection dir = ForgeDirection.UNKNOWN;
            if (connections[ForgeDirection.EAST.ordinal()] && connections[ForgeDirection.WEST.ordinal()])
                dir = d = ForgeDirection.EAST;
            if (getNodeIcon() != null)
                renderTexturedCuboid(aabb, getSideIcon(), dir, renderer);
            colorIcon = IconSupplier.pneumaticTubeColorSide;
        }

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (connections[dir.ordinal()])
                continue;
            TubeColor sideColor = color[dir.ordinal()];
            if (sideColor != TubeColor.NONE) {
                renderer.setColor(ItemDye.field_150922_c[sideColor.ordinal()]);
                renderTexturedCuboid(aabb, colorIcon, d, renderer, new boolean[] { dir == ForgeDirection.DOWN, dir == ForgeDirection.UP,
                        dir == ForgeDirection.WEST, dir == ForgeDirection.EAST, dir == ForgeDirection.NORTH, dir == ForgeDirection.SOUTH });
                renderer.setColor(0xFFFFFF);
            }
        }
    }

    public void renderTexturedCuboid(Vec3dCube box, IIcon icon, ForgeDirection dir, RenderHelper helper) {

        if (dir == ForgeDirection.UNKNOWN) {
            helper.setRenderSides(!connections[ForgeDirection.DOWN.ordinal()], !connections[ForgeDirection.UP.ordinal()],
                    !connections[ForgeDirection.WEST.ordinal()], !connections[ForgeDirection.EAST.ordinal()],
                    !connections[ForgeDirection.NORTH.ordinal()], !connections[ForgeDirection.SOUTH.ordinal()]);
        } else {
            helper.setRenderSide(dir, false);
            helper.setRenderSide(dir.getOpposite(), false);
        }

        renderTexturedCuboid(box, icon, dir, helper, null);
    }

    public void renderTexturedCuboid(Vec3dCube box, IIcon icon, ForgeDirection dir, RenderHelper helper, boolean[] sides) {

        if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST)
            helper.setTextureRotations(1, 1, 1, 1, 0, 0);
        else if (dir == ForgeDirection.NORTH
                || dir == ForgeDirection.SOUTH
                || (dir == ForgeDirection.UNKNOWN && (connections[ForgeDirection.NORTH.ordinal()] || connections[ForgeDirection.SOUTH
                                                                                                                 .ordinal()])))
            helper.setTextureRotations(0, 0, 0, 0, 1, 1);

        if (sides != null)
            helper.setRenderSides(sides[0], sides[1], sides[2], sides[3], sides[4], sides[5]);

        helper.renderBox(box, icon);
        helper.setRenderFromInside(true);
        helper.renderBox(box, icon);
        helper.setRenderFromInside(false);

        helper.resetTextureRotations();
        helper.resetRenderedSides();
    }

    protected boolean shouldRenderNode() {

        boolean shouldRenderNode = false;
        int connectionCount = 0;
        for (int i = 0; i < 6; i += 2) {
            if (connections[i] != connections[i + 1]) {
                shouldRenderNode = true;
                break;
            }
            if (connections[i])
                connectionCount++;
            if (connections[i + 1])
                connectionCount++;
        }
        return shouldRenderNode || connectionCount == 0 || connectionCount > 2;
    }

    /**
     * This render method gets called whenever there's a block update in the chunk. You should use this to remove load from the renderer if a part of
     * the rendering code doesn't need to get called too often or just doesn't change at all. To call a render update to re-render this just call
     * {@link BPPart#markPartForRenderUpdate()}
     *
     * @param loc
     *            Distance from the player's position
     * @param pass
     *            Render pass (0 or 1)
     * @return Whether or not it rendered something
     */
    @Override
    public boolean renderStatic(Vec3i loc, RenderHelper renderer, RenderBlocks renderBlocks, int pass) {

        Tessellator t = Tessellator.instance;
        t.setColorOpaque_F(1, 1, 1);

        List<Vec3dCube> aabbs = getTubeBoxes();

        renderMiddle(aabbs.get(0), renderer);

        for (int i = 1; i < aabbs.size(); i++) {
            ForgeDirection dir = ForgeDirection.getOrientation(getSideFromAABBIndex(i));
            renderTexturedCuboid(aabbs.get(i), getSideIcon(dir), dir, renderer);
            TubeColor sideColor = color[dir.ordinal()];
            if (sideColor != TubeColor.NONE) {
                renderer.setColor(ItemDye.field_150922_c[sideColor.ordinal()]);
                renderTexturedCuboid(aabbs.get(i), IconSupplier.pneumaticTubeColorSide, dir, renderer);
                renderer.setColor(0xFFFFFF);
            }
        }

        return true;
    }

    protected void renderSide() {

    }

    /**
     * Hacky method to get the right side
     *
     * @return
     */
    private int getSideFromAABBIndex(int index) {

        int curIndex = 0;
        for (int side = 0; side < 6; side++) {
            if (connections[side]) {
                curIndex++;
                if (index == curIndex)
                    return side;
            }
        }
        return 0;
    }

    protected IIcon getSideIcon(ForgeDirection side) {

        return getSideIcon();
    }

    protected IIcon getSideIcon() {

        return IconSupplier.pneumaticTubeSide;
    }

    protected IIcon getNodeIcon() {

        return IconSupplier.pneumaticTubeNode;
    }

    /**
     * Adds information to the waila tooltip
     *
     * @author amadornes
     *
     * @param info
     */
    @Override
    public void addWailaInfo(List<String> info) {

        boolean addTooltip = false;
        for (TubeColor col : color) {
            if (col != TubeColor.NONE) {
                addTooltip = true;
                break;
            }
        }
        if (addTooltip) {
            info.add(Color.YELLOW + I18n.format("waila.pneumaticTube.color"));
            for (int i = 0; i < 6; i++) {
                if (color[i] != TubeColor.NONE) {
                    if (color[i] != TubeColor.NONE)
                        info.add(EnumChatFormatting.DARK_AQUA
                                + I18n.format("rotation." + ForgeDirection.getOrientation(i).toString().toLowerCase()) + ": "
                                + EnumChatFormatting.WHITE + I18n.format("gui.widget.color." + ItemDye.field_150923_a[color[i].ordinal()]));
                }
            }
        }
    }
}
