package com.cgessinger.creaturesandbeasts.entities;

import com.cgessinger.creaturesandbeasts.entities.ai.FindWaterOneDeepGoal;
import com.cgessinger.creaturesandbeasts.init.CNBLilytadTypes;
import com.cgessinger.creaturesandbeasts.init.CNBSoundEvents;
import com.cgessinger.creaturesandbeasts.util.LilytadType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IForgeShearable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class LilytadEntity extends Animal implements IForgeShearable, IAnimatable {
    public static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(LilytadEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(LilytadEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    private int shearedTimer;

    public LilytadEntity(EntityType<LilytadEntity> type, Level worldIn) {
        super(type, worldIn);
        this.shearedTimer = 0;

        this.lookControl = new LookControl(this) {
            @Override
            public void tick() {
                LilytadEntity lilytad = (LilytadEntity) this.mob;
                if (lilytad.shouldLookAround()) {
                    super.tick();
                }
            }
        };
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TYPE, CNBLilytadTypes.PINK.getId().toString());
        this.entityData.define(SHEARED, false);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        LilytadType type = LilytadType.getById(compound.getString("LilytadType"));
        if (type == null) {
            type = CNBLilytadTypes.PINK;
        }
        this.setLilytadType(type);
        this.shearedTimer = compound.getInt("ShearedTimer");
        this.setSheared(this.shearedTimer > 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("ShearedTimer", this.shearedTimer);
        compound.putString("LilytadType", this.getLilytadType().getId().toString());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FindWaterOneDeepGoal(this));
        this.goalSelector.addGoal(2, new LilytadPanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !this.mob.level.getFluidState(this.mob.blockPosition()).is(FluidTags.WATER) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !this.mob.level.getFluidState(this.mob.blockPosition()).is(FluidTags.WATER) && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level.isClientSide() && --this.shearedTimer == 0) {
            this.setSheared(false);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag tag) {
        switch (this.random.nextInt(3)) {
            case 0:
            default:
                this.setLilytadType(CNBLilytadTypes.PINK);
                break;
            case 1:
                this.setLilytadType(CNBLilytadTypes.LIGHT_PINK);
                break;
            case 2:
                this.setLilytadType(CNBLilytadTypes.YELLOW);
                break;
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
    }

    public static boolean checkLilytadSpawnRules(EntityType<LilytadEntity> animal, LevelAccessor worldIn, MobSpawnType reason, BlockPos pos, RandomSource randomIn) {
        return true;
    }

    @Override
    protected void pushEntities() {
        List<Entity> list = this.level.getEntities(this, this.getBoundingBox().inflate(0.2, 0, 0.2), EntitySelector.pushableBy(this));
        if (!list.isEmpty()) {
            int i = this.level.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
                int j = 0;

                for (Entity entity : list) {
                    if (!entity.isPassenger()) {
                        ++j;
                    }
                }

                if (j > i - 1) {
                    this.hurt(DamageSource.CRAMMING, 6.0F);
                }
            }

            for (Entity entity : list) {
                this.doPush(entity);
            }
        }

    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob entity) {
        return null;
    }

    public boolean getSheared() {
        return this.entityData.get(SHEARED);
    }

    public void setSheared(boolean sheared) {
        this.shearedTimer = sheared ? 18000 : 0;
        this.entityData.set(SHEARED, sheared);
    }

    public void setLilytadType(LilytadType lilytadType) {
        this.entityData.set(TYPE, lilytadType.getId().toString());
    }

    public LilytadType getLilytadType() {
        return LilytadType.getById(this.entityData.get(TYPE));
    }

    @Override
    public boolean isShearable(@Nonnull ItemStack item, Level world, BlockPos pos) {
        return !this.getSheared();
    }

    @Nonnull
    @Override
    public List<ItemStack> onSheared(@Nullable Player player, @Nonnull ItemStack item, Level world, BlockPos pos, int fortune) {
        world.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 1.0F, 1.0F);
        this.gameEvent(GameEvent.SHEAR, player);
        if (!world.isClientSide) {
            this.setSheared(true);
            java.util.List<ItemStack> items = new java.util.ArrayList<>();
            items.add(new ItemStack(this.getLilytadType().getShearItem()));

            return items;
        }
        return java.util.Collections.emptyList();
    }

    public boolean shouldLookAround() {
        return !this.level.getFluidState(this.blockPosition()).is(FluidTags.WATER);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return CNBSoundEvents.LILYTAD_HURT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return CNBSoundEvents.LILYTAD_AMBIENT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return CNBSoundEvents.LILYTAD_DEATH.get();
    }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        if (!(animationSpeed > -0.05F && animationSpeed < 0.05F)) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("lilytad.walk", ILoopType.EDefaultLoopTypes.LOOP));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    static class LilytadPanicGoal extends PanicGoal {
        private final LilytadEntity lilytad;

        public LilytadPanicGoal(LilytadEntity lilytad, double speedModifier) {
            super(lilytad, speedModifier);
            this.lilytad = lilytad;
        }

        @Override
        public void start() {
            this.lilytad.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
            this.isRunning = true;
        }

        @Override
        protected boolean findRandomPosition() {
            boolean flag = GoalUtils.mobRestricted(this.lilytad, 5);
            Vec3 vec3 = RandomPos.generateRandomPos(this.lilytad, () -> {
                BlockPos blockpos = RandomPos.generateRandomDirection(this.lilytad.getRandom(), 5, 4);
                return generateRandomPosTowardDirection(this.lilytad, 5, flag, blockpos);
            });
            if (vec3 == null) {
                return false;
            }

            this.posX = vec3.x;
            this.posY = vec3.y;
            this.posZ = vec3.z;
            return true;
        }

        @Nullable
        private static BlockPos generateRandomPosTowardDirection(LilytadEntity lilytad, int horizontalRange, boolean flag, BlockPos posTowards) {
            BlockPos blockpos = RandomPos.generateRandomPosTowardDirection(lilytad, horizontalRange, lilytad.getRandom(), posTowards);
            return !GoalUtils.isOutsideLimits(blockpos, lilytad) && !GoalUtils.isRestricted(flag, lilytad, blockpos) && !GoalUtils.hasMalus(lilytad, blockpos) && (!GoalUtils.isNotStable(lilytad.getNavigation(), blockpos) || (GoalUtils.isWater(lilytad, blockpos) && lilytad.level.getBlockState(blockpos.below()).canOcclude() && lilytad.level.getBlockState(blockpos.above()).isAir())) ? blockpos : null;
        }
    }
}
