package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.init.DASounds;

import java.util.HashSet;
import java.util.Set;

public class ChainBiteSkill extends EotsSkill {
    private static final int APPROACH = 0;
    private static final int WINDUP = 1;
    private static final int LUNGE = 2;
    private static final int RECOIL = 3;
    private static final int SWEEP = 4;
    private static final double BITE_HEIGHT = 1.4;
    private static final int SWEEP_TICKS = 16;
    private final Set<Integer> sweepHit = new HashSet<>();
    private int bites;
    private int done;
    private int windup;
    private Vec3 dir = Vec3.ZERO;
    private Vec3 flat = Vec3.ZERO;
    private boolean landed;
    private boolean curve;

    public ChainBiteSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.bites = EotsConfig.CHAIN_BITE_COUNT.get();
        this.windup = 8;
        if (this.g.stage() >= 3) {
            this.bites++;
            this.windup = 6;
        }
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 2.5F, 1.2F);
        this.setState(APPROACH);
    }

    @Override
    protected void tick() {
        LivingEntity target = this.g.target();
        if (this.state == APPROACH) {
            if (target == null) {
                this.finish();
                return;
            }
            Vec3 head = this.g.pos();
            double floor = this.g.floorBelow(target.position());
            this.g.steerToward(new Vec3(target.getX(), floor + BITE_HEIGHT, target.getZ()), 1.1, 16.0F);
            if (this.st % 3 == 0) {
                this.g.particle(ParticleTypes.CLOUD, head, this.g.look().scale(-1.0), 0.25);
            }
            Vec3 to = new Vec3(target.getX() - head.x, 0, target.getZ() - head.z);
            double horiz = to.length();
            Vec3 look = this.g.look();
            Vec3 lookFlat = new Vec3(look.x, 0, look.z);
            boolean facing = horiz > 1.0E-4 && lookFlat.lengthSqr() > 1.0E-4 && lookFlat.normalize().dot(to.scale(1.0 / horiz)) > 0.75;
            boolean low = head.y - this.g.floorBelow(head) < BITE_HEIGHT + 2.5;
            if ((horiz < 7.0 && horiz > 2.5 && facing && low) || this.st >= 40) {
                this.g.anim(EotsAnim.BITE_WINDUP);
                this.g.seg.setMouthOpen(true);
                this.g.sound(DASounds.EOTS_AMBIENT.get(), 2.0F, 1.4F);
                this.setState(WINDUP);
            }
        } else if (this.state == WINDUP) {
            if (target != null) {
                this.g.lookAtTarget(20.0F);
            }
            Vec3 look = this.g.look();
            this.g.holdHeight(new Vec3(look.x, 0, look.z).scale(0.15), BITE_HEIGHT + 0.8);
            this.g.burst(ParticleTypes.CLOUD, this.g.mouth(), 1, 0.2, 0.01);
            if (this.st >= this.windup) {
                Vec3 aim = this.g.look();
                if (target != null) {
                    aim = target.getEyePosition().subtract(this.g.pos());
                }
                double y = Mth.clamp(aim.normalize().y, -0.35, 0.35);
                Vec3 h = new Vec3(aim.x, 0, aim.z);
                if (h.lengthSqr() < 1.0E-4) {
                    h = new Vec3(this.g.look().x, 0, this.g.look().z);
                }
                if (h.lengthSqr() < 1.0E-4) {
                    h = new Vec3(1, 0, 0);
                }
                this.flat = h.normalize();
                this.dir = this.flat.add(0, y, 0).normalize();
                this.landed = false;
                this.g.anim(EotsAnim.BITE_SNAP);
                this.g.sound(SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.6F);
                this.setState(LUNGE);
            }
        } else if (this.state == LUNGE) {
            this.g.fly(this.dir.scale(1.6));
            this.g.particle(ParticleTypes.CLOUD, this.g.pos(), this.dir.scale(-1.0), 0.25);
            for (LivingEntity e : this.g.touching(this.g.seg, 0.9)) {
                EotsCombat.hurt(this.g.seg, e, EotsConfig.CHAIN_BITE.damageMultiplier.get());
                EotsCombat.push(e, this.flat, 0.6, 0.2);
                this.landed = true;
            }
            if (this.landed || this.st >= 6) {
                this.g.seg.setMouthOpen(false);
                this.curve = this.g.seg.getRandom().nextBoolean();
                this.setState(RECOIL);
            }
        } else if (this.state == RECOIL) {
            Vec3 side = new Vec3(-this.flat.z, 0, this.flat.x);
            if (this.curve) {
                side = side.scale(-1.0);
            }
            Vec3 look = this.g.look();
            Vec3 lookFlat = new Vec3(look.x, 0, look.z);
            if (lookFlat.lengthSqr() < 1.0E-4) {
                lookFlat = this.flat;
            }
            this.g.turnToward(lookFlat.normalize().add(side.scale(0.6)), 14.0F);
            look = this.g.look();
            double speed = 1.1 - this.st * 0.05;
            this.g.holdHeight(new Vec3(look.x, 0, look.z).normalize().scale(speed), BITE_HEIGHT);
            if (this.st >= 10) {
                this.done++;
                if (target == null) {
                    this.finish();
                } else if (this.done >= this.bites) {
                    this.g.anim(EotsAnim.SWEEP);
                    this.g.seg.setMouthOpen(true);
                    this.g.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 3.0F, 0.6F);
                    this.g.sound(DASounds.EOTS_SHOOT.get(), 2.0F, 1.3F);
                    Vec3 to = target.position().subtract(this.g.pos());
                    if (to.x * to.x + to.z * to.z > 1.0E-4) {
                        this.flat = new Vec3(to.x, 0, to.z).normalize();
                    }
                    this.setState(SWEEP);
                } else {
                    this.g.anim(EotsAnim.IDLE);
                    this.setState(APPROACH);
                }
            }
        } else {
            float f = Math.min(1.0F, (this.st + 1) / (float) (SWEEP_TICKS - 6));
            this.g.headSweep(this.flat, f, 5.0, EotsConfig.CHAIN_BITE_SWEEP_MULTIPLIER.get(), this.sweepHit);
            Vec3 look = this.g.look();
            this.g.holdHeight(new Vec3(look.x, 0, look.z).scale(0.75), BITE_HEIGHT);
            if (this.st >= SWEEP_TICKS) {
                this.g.seg.setMouthOpen(false);
                this.finish();
            }
        }
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
