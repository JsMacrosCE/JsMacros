package com.jsmacrosce.jsmacros.client.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.jsmacrosce.jsmacros.api.PlayerInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("EntityConstructor")
public class MovementDummy extends LivingEntity {

    private List<Vec3> coordsHistory = new ArrayList<>();
    private List<PlayerInput> inputs = new ArrayList<>();

    // Is used for checking the depthstrider enchant
    private Map<EquipmentSlot, ItemStack> equippedStack = new HashMap<>(6);
    private int jumpingCooldown;

    public MovementDummy(MovementDummy player) {
        this(player.level(), player.position(), player.getDeltaMovement(), player.getBoundingBox(), player.onGround(), player.isSprinting(), player.isShiftKeyDown());
        this.inputs = new ArrayList<>(player.getInputs());
        this.coordsHistory = new ArrayList<>(player.getCoordsHistory());
        this.jumpingCooldown = player.jumpingCooldown;
        this.equippedStack = player.equippedStack;
    }

    public MovementDummy(LocalPlayer player) {
        this(player.level(), player.position(), player.getDeltaMovement(), player.getBoundingBox(), player.onGround(), player.isSprinting(), player.isShiftKeyDown());
        for (EquipmentSlot value : EquipmentSlot.values()) {
            equippedStack.put(value, player.getItemBySlot(value).copy());
        }
    }

    public MovementDummy(Level world, Vec3 pos, Vec3 velocity, AABB hitBox, boolean onGround, boolean isSprinting, boolean isSneaking) {
        super(EntityType.PLAYER, world);
        this.setPosRaw(pos.x(), pos.y(), pos.z());
        this.setDeltaMovement(velocity);
        this.setBoundingBox(hitBox);
        this.setSprinting(isSprinting);
        this.setShiftKeyDown(isSneaking);
//        this.getStepHeight(0.6F);
        this.setOnGround(onGround);
        this.coordsHistory.add(this.position());

        for (EquipmentSlot value : EquipmentSlot.values()) {
            equippedStack.put(value, new ItemStack(Items.AIR));
        }
    }

    public List<Vec3> getCoordsHistory() {
        return coordsHistory;
    }

    public List<PlayerInput> getInputs() {
        return inputs;
    }

    public Vec3 applyInput(PlayerInput input) {
        inputs.add(input); // We use this and not the clone, since the clone may be modified?
        PlayerInput currentInput = input.clone();
        this.setYRot(currentInput.yaw);

        Vec3 velocity = this.getDeltaMovement();
        double velX = velocity.x;
        double velY = velocity.y;
        double velZ = velocity.z;
        if (Math.abs(velocity.x) < 0.003D) {
            velX = 0.0D;
        }
        if (Math.abs(velocity.y) < 0.003D) {
            velY = 0.0D;
        }
        if (Math.abs(velocity.z) < 0.003D) {
            velZ = 0.0D;
        }
        this.setDeltaMovement(velX, velY, velZ);

        /** Sneaking start **/
        if (this.isShiftKeyDown() && this.canChangeIntoPose(Pose.CROUCHING)) {
            // Yeah this looks dumb, but that is the way minecraft does it
            currentInput.movementSideways = (float) ((double) currentInput.movementSideways * 0.3D);
            currentInput.movementForward = (float) ((double) currentInput.movementForward * 0.3D);
        }
        this.setShiftKeyDown(currentInput.sneaking);
        /** Sneaking end **/

        /** Sprinting start **/
        boolean hasHungerToSprint = true;
        if (!this.isSprinting() && !currentInput.sneaking && hasHungerToSprint && !this.hasEffect(MobEffects.BLINDNESS) && currentInput.sprinting) {
            this.setSprinting(true);
        }

        if (this.isSprinting() && (currentInput.movementForward <= 1.0E-5F || this.horizontalCollision)) {
            this.setSprinting(false);
        }
        /** Sprinting end **/

        /** Jumping start **/
        if (this.jumpingCooldown > 0) {
            --this.jumpingCooldown;
        }

        if (currentInput.jumping) {
            if (this.onGround() && this.jumpingCooldown == 0) {
                this.jumpFromGround();
                this.jumpingCooldown = 10;
            }
        } else {
            this.jumpingCooldown = 0;
        }
        /** Jumping END **/

        this.travel(new Vec3(currentInput.movementSideways * 0.98, 0.0, currentInput.movementForward * 0.98));

// TODO: airStrafingSpeed was removed and replaced with getOffGroundSpeed()
//        /* flyingSpeed only gets set after travel */
//        this.airStrafingSpeed = this.isSprinting() ? 0.026F : 0.02F;

        return this.position();
    }

    /**
     * We have to do this "inject" since the the applyClimbingSpeed() method
     * in LivingEntity is checking if we are a PlayerEntity, we want to apply the outcome of this check,
     * so this is why we need to set the y-velocity to 0.<p>
     */
    @Override
    public void travel(Vec3 movementInput) {
        if (this.onClimbable() && this.getDeltaMovement().y() < 0.0D && !this.getInBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder()) {
            this.setDeltaMovement(this.getDeltaMovement().x(), 0, this.getDeltaMovement().z());
        }
        super.travel(movementInput);
    }

    //TODO: relink?
    protected boolean canClimb() {
        return !this.onGround() || !this.isShiftKeyDown();
    }

    @Override
    public boolean canSimulateMovement() {
        return true;
    }

    @Override
    public void setSprinting(boolean sprinting) {
        super.setSprinting(sprinting);
        this.setSpeed(sprinting ? 0.13F : 0.1F);
    }

    @Override
    public ItemStack getMainHandItem() {
        return new ItemStack(Items.AIR);
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return equippedStack.get(slot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        // This is just for rendering
        return HumanoidArm.RIGHT;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public MovementDummy clone() {
        return new MovementDummy(this);
    }

    protected boolean canChangeIntoPose(Pose pose) {
        return this.level().noCollision(this, this.getDimensions(pose).makeBoundingBox(this.position()).deflate(1.0E-7));
    }
}
