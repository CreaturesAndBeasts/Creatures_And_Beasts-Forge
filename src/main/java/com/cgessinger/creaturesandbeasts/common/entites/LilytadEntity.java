package com.cgessinger.creaturesandbeasts.common.entites;

import com.cgessinger.creaturesandbeasts.common.init.ModItems;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.IShearable;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.IForgeShearable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class LilytadEntity extends AnimalEntity implements IForgeShearable, IAnimatable
{
	private final AnimationFactory factory = new AnimationFactory(this);
	private static final DataParameter<Boolean> SHEARED = EntityDataManager.createKey(LilytadEntity.class, DataSerializers.BOOLEAN);

	public LilytadEntity (EntityType<? extends AnimalEntity> type, World worldIn)
	{
		super(type, worldIn);
	}

	@Override
	protected void registerData ()
	{
		this.dataManager.register(SHEARED, false);
		super.registerData();
	}

	public static AttributeModifierMap.MutableAttribute setCustomAttributes ()
	{
		return MobEntity.func_233666_p_()
				.createMutableAttribute(Attributes.MAX_HEALTH, 20.0D) // Max Health
				.createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.2D); // Movement Speed
	}

	@Override
	protected void registerGoals()
	{
		//this.goalSelector.addGoal(0, new SwimGoal(this));
		//this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
		this.goalSelector.addGoal(4, new LookAtGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
	}

	private <E extends IAnimatable> PlayState animationPredicate (AnimationEvent<E> event)
	{
		if (!(limbSwingAmount > -0.15F && limbSwingAmount < 0.15F))
		{
			event.getController().setAnimation(new AnimationBuilder().addAnimation("lilytad.walk", true));
			return PlayState.CONTINUE;
		}
		return PlayState.STOP;
	}

	@Nullable
	@Override
	public AgeableEntity func_241840_a (ServerWorld p_241840_1_, AgeableEntity p_241840_2_)
	{
		return null;
	}

	@Override
	public boolean isShearable (@Nonnull ItemStack item, World world, BlockPos pos)
	{
		return !this.dataManager.get(SHEARED);
	}

	public void setSheared (boolean sheared)
	{
		this.dataManager.set(SHEARED, sheared);
	}

	@Nonnull
	@Override
	public List<ItemStack> onSheared (@Nullable PlayerEntity player, @Nonnull ItemStack item, World world, BlockPos pos, int fortune)
	{
		//world.playMovingSound(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 1.0F, 1.0F);
		if (!world.isRemote)
		{
			this.setSheared(true);
			java.util.List<ItemStack> items = new java.util.ArrayList<>();
			items.add(new ItemStack(ModItems.LILYTAD_FLOWER.get()));

			return items;
		}
		return java.util.Collections.emptyList();
	}

	@Override
	public void registerControllers (AnimationData animationData)
	{
		animationData.addAnimationController(new AnimationController<LilytadEntity>(this, "controller", 0, this::animationPredicate));
	}

	@Override
	public AnimationFactory getFactory ()
	{
		return this.factory;
	}
}