package com.breakdelay.mixin;

import com.breakdelay.Config;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {


    /**
     * @author mern
     * @reason change a number
     */
    @Overwrite
    public boolean continueDestroyBlock(BlockPos pPosBlock, Direction pDirectionFacing) {
        this.ensureHasSentCarriedItem();
        if (this.destroyDelay > 0) {
            --this.destroyDelay;
            return true;
        } else {
            BlockState blockstate;
            if (this.localPlayerMode.isCreative() && this.minecraft.level.getWorldBorder().isWithinBounds(pPosBlock)) {
                this.destroyDelay = 5;
                blockstate = this.minecraft.level.getBlockState(pPosBlock);
                this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pPosBlock, blockstate, 1.0F);
                this.startPrediction(this.minecraft.level, (p_233753_) -> {
                    if (!ForgeHooks.onLeftClickBlock(this.minecraft.player, pPosBlock, pDirectionFacing, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK).isCanceled()) {
                        this.destroyBlock(pPosBlock);
                    }

                    return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pPosBlock, pDirectionFacing, p_233753_);
                });
                return true;
            } else if (this.sameDestroyTarget(pPosBlock)) {
                blockstate = this.minecraft.level.getBlockState(pPosBlock);
                if (blockstate.isAir()) {
                    this.isDestroying = false;
                    return false;
                } else {
                    this.destroyProgress += blockstate.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), pPosBlock);
                    if (this.destroyTicks % 4.0F == 0.0F) {
                        SoundType soundtype = blockstate.getSoundType(this.minecraft.level, pPosBlock, this.minecraft.player);
                        this.minecraft.getSoundManager().play(new SimpleSoundInstance(soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F, SoundInstance.createUnseededRandom(), pPosBlock));
                    }

                    ++this.destroyTicks;
                    this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pPosBlock, blockstate, Mth.clamp(this.destroyProgress, 0.0F, 1.0F));
                    if (ForgeHooks.onClientMineHold(this.minecraft.player, pPosBlock, pDirectionFacing).getUseItem() == Event.Result.DENY) {
                        return true;
                    } else {
                        if (this.destroyProgress >= 1.0F) {
                            this.isDestroying = false;
                            this.startPrediction(this.minecraft.level, (p_233739_) -> {
                                this.destroyBlock(pPosBlock);
                                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pPosBlock, pDirectionFacing, p_233739_);
                            });
                            this.destroyProgress = 0.0F;
                            this.destroyTicks = 0.0F;
                            this.destroyDelay = Config.breakingDelay;
                        }

                        this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
                        return true;
                    }
                }
            } else {
                return this.startDestroyBlock(pPosBlock, pDirectionFacing);
            }
        }
    }

    @Shadow public abstract void handlePlaceRecipe(int p_105218_, Recipe<?> p_105219_, boolean p_105220_);

    @Shadow @Final private Minecraft minecraft;
    @Shadow private BlockPos destroyBlockPos;
    @Shadow private float destroyProgress;
    @Shadow private float destroyTicks;
    @Shadow private int destroyDelay;
    @Shadow private boolean isDestroying;
    @Shadow private GameType localPlayerMode;


    @Shadow public abstract boolean destroyBlock(BlockPos p_105268_);

    @Shadow public abstract boolean startDestroyBlock(BlockPos p_105270_, Direction p_105271_);


    @Shadow protected abstract void startPrediction(ClientLevel p_233730_, PredictiveAction p_233731_);


    @Shadow protected abstract boolean sameDestroyTarget(BlockPos p_105282_);

    @Shadow protected abstract void ensureHasSentCarriedItem();


    @Shadow public abstract int getDestroyStage();
}
