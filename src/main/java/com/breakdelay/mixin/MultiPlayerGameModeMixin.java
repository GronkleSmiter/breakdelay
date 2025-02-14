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

    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ClientPacketListener connection;
    @Shadow private BlockPos destroyBlockPos;
    @Shadow private ItemStack destroyingItem;
    @Shadow private float destroyProgress;
    @Shadow private float destroyTicks;
    @Shadow private int destroyDelay;
    @Shadow private boolean isDestroying;
    @Shadow private GameType localPlayerMode;
    @Shadow @Nullable private GameType previousLocalPlayerMode;
    @Shadow private int carriedIndex;

    @Shadow public abstract void adjustPlayer(Player p_105222_);

    @Shadow public abstract void setLocalMode(GameType p_171806_, @org.jetbrains.annotations.Nullable GameType p_171807_);

    @Shadow public abstract void setLocalMode(GameType p_105280_);

    @Shadow public abstract boolean canHurtPlayer();

    @Shadow public abstract boolean destroyBlock(BlockPos p_105268_);

    @Shadow public abstract boolean startDestroyBlock(BlockPos p_105270_, Direction p_105271_);

    @Shadow public abstract void stopDestroyBlock();

    @Shadow protected abstract void startPrediction(ClientLevel p_233730_, PredictiveAction p_233731_);

    @Shadow public abstract float getPickRange();

    @Shadow public abstract void tick();

    @Shadow protected abstract boolean sameDestroyTarget(BlockPos p_105282_);

    @Shadow protected abstract void ensureHasSentCarriedItem();

    @Shadow public abstract InteractionResult useItemOn(LocalPlayer p_233733_, InteractionHand p_233734_, BlockHitResult p_233735_);

    @Shadow protected abstract InteractionResult performUseItemOn(LocalPlayer p_233747_, InteractionHand p_233748_, BlockHitResult p_233749_);

    @Shadow public abstract InteractionResult useItem(Player p_233722_, InteractionHand p_233723_);

    @Shadow public abstract LocalPlayer createPlayer(ClientLevel p_105247_, StatsCounter p_105248_, ClientRecipeBook p_105249_);

    @Shadow public abstract LocalPlayer createPlayer(ClientLevel p_105251_, StatsCounter p_105252_, ClientRecipeBook p_105253_, boolean p_105254_, boolean p_105255_);

    @Shadow public abstract void attack(Player p_105224_, Entity p_105225_);

    @Shadow public abstract InteractionResult interact(Player p_105227_, Entity p_105228_, InteractionHand p_105229_);

    @Shadow public abstract InteractionResult interactAt(Player p_105231_, Entity p_105232_, EntityHitResult p_105233_, InteractionHand p_105234_);

    @Shadow public abstract void handleInventoryMouseClick(int p_171800_, int p_171801_, int p_171802_, ClickType p_171803_, Player p_171804_);

    @Shadow public abstract void handleInventoryButtonClick(int p_105209_, int p_105210_);

    @Shadow public abstract void handleCreativeModeItemAdd(ItemStack p_105242_, int p_105243_);

    @Shadow public abstract void handleCreativeModeItemDrop(ItemStack p_105240_);

    @Shadow public abstract void releaseUsingItem(Player p_105278_);

    @Shadow public abstract boolean hasExperience();

    @Shadow public abstract boolean hasMissTime();

    @Shadow public abstract boolean hasInfiniteItems();

    @Shadow public abstract boolean hasFarPickRange();

    @Shadow public abstract boolean isServerControlledInventory();

    @Shadow public abstract boolean isAlwaysFlying();

    @Shadow @Nullable public abstract GameType getPreviousPlayerMode();

    @Shadow public abstract GameType getPlayerMode();

    @Shadow public abstract boolean isDestroying();

    @Shadow public abstract int getDestroyStage();

    @Shadow public abstract void handlePickItem(int p_105207_);

    @Shadow protected abstract Packet lambda$useItem$5(InteractionHand par1, Player par2, MutableObject par3, int par4);

    @Shadow protected abstract Packet lambda$useItemOn$4(MutableObject par1, LocalPlayer par2, InteractionHand par3, BlockHitResult par4, int par5);

    @Shadow protected abstract Packet lambda$continueDestroyBlock$3(BlockPos par1, Direction par2, int par3);

    @Shadow protected abstract Packet lambda$continueDestroyBlock$2(BlockPos par1, Direction par2, int par3);

    @Shadow protected abstract Packet lambda$startDestroyBlock$1(BlockState par1, PlayerInteractEvent.LeftClickBlock par2, BlockPos par3, Direction par4, int par5);

    @Shadow protected abstract Packet lambda$startDestroyBlock$0(BlockPos par1, Direction par2, int par3);
}
