package net.sorenon.mcxr.play.rendering;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;

public class UnownedTexture extends AbstractTexture {

    public UnownedTexture(int glID) {
        this.glId = glID;
    }

    @Override
    public void load(ResourceManager manager) {

    }

    @Override
    public void clearGlId() {

    }
}
