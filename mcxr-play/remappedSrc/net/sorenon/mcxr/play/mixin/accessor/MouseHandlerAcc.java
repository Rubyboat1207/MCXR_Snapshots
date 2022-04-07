package net.sorenon.mcxr.play.mixin.accessor;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mouse.class)
public interface MouseHandlerAcc {

    @Invoker()
    void callOnMove(long l, double d, double e);

    @Invoker()
    void callOnPress(long l, int i, int j, int k);

    @Invoker()
    void callOnScroll(long l, double d, double e);
}
