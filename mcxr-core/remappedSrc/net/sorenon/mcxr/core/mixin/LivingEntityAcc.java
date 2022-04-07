package net.sorenon.mcxr.core.mixin;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAcc {

    @Invoker
    float callGetStandingEyeHeight(EntityPose pose, EntityDimensions entityDimensions);
}
