package com.cgessinger.creaturesandbeasts.client.entity.model;

import com.cgessinger.creaturesandbeasts.CreaturesAndBeasts;
import com.cgessinger.creaturesandbeasts.entities.SporelingEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SporelingModel extends AnimatedGeoModel<SporelingEntity> {
    private static final ResourceLocation SPORELING_ANIMATIONS = new ResourceLocation(CreaturesAndBeasts.MOD_ID, "animations/sporeling.json");

    @Override
    public ResourceLocation getModelLocation(SporelingEntity entity) {
        return entity.getSporelingType().getModelLocation();
    }

    @Override
    public ResourceLocation getTextureLocation(SporelingEntity entity) {
        return entity.getSporelingType().getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationFileLocation(SporelingEntity entity) {
        return SPORELING_ANIMATIONS;
    }
}
