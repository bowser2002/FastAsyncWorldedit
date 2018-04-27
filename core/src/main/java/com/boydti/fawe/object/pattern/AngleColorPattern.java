package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.DataAnglePattern;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import java.io.IOException;

public class AngleColorPattern extends DataAnglePattern {
    private static final double FACTOR = 1d / 256;
    private transient TextureUtil util;

    private final boolean randomize;
    private final int complexity;

    public AngleColorPattern(Extent extent, int complexity, boolean randomize, int distance) {
        super(extent, distance);
        this.complexity = complexity;
        this.randomize = randomize;
        this.util = Fawe.get().getCachedTextureUtil(randomize, 0, complexity);
    }

    public int getColor(int color, int slope) {
        if (slope == 0) return color;
        double newFactor = (256 - Math.min(256, slope)) * FACTOR;
        int newRed = (int) (((color >> 16) & 0xFF) * newFactor);
        int newGreen = (int) (((color >> 8) & 0xFF) * newFactor);
        int newBlue = (int) (((color >> 0) & 0xFF) * newFactor);
        return (((color >> 24) & 0xFF) << 24) + (newRed << 16) + (newGreen << 8) + (newBlue << 0);
    }

    @Override
    public BaseBlock apply(Vector position) {
        BaseBlock block = extent.getBlock(position);
        int slope = getSlope(block, position);
        if (slope == -1) return block;
        int color = util.getColor(block);
        if (color == 0) return block;
        int newColor = getColor(color, slope);
        return util.getNearestBlock(newColor);
    }

    @Override
    public int getSlope(BaseBlock block, Vector vector) {
        int slope = super.getSlope(block, vector);
        if (slope != -1) {
            int x = vector.getBlockX();
            int y = vector.getBlockY();
            int z = vector.getBlockZ();
            int height = extent.getNearestSurfaceTerrainBlock(x, z, y, 0, maxY);
            if (height > 0) {
                BaseBlock below = extent.getLazyBlock(x, height - 1, z);
                if (FaweCache.canPassThrough(block.getId(), block.getData())) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return slope;
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        BaseBlock block = extent.getBlock(getPosition);
        int slope = getSlope(block, getPosition);
        if (slope == -1) return false;
        int color = util.getColor(block);
        if (color == 0) return false;
        int newColor = getColor(color, slope);
        BaseBlock newBlock = util.getNearestBlock(newColor);
        if (newBlock == null) return false;
        return extent.setBlock(setPosition, newBlock);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        util = Fawe.get().getCachedTextureUtil(randomize, 0, complexity);
    }
}