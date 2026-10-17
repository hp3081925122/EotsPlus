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
import io.github.razordevs.deep_aether.init.DASounds;

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
            Vec3 away = new Vec3(head.x - target.getX(), 0, head.z - target.getZ());
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(1, 0, 0);
            }
            Vec3 spot = target.position().add(away.normalize().scale(3.5));
            double floor = EotsCombat.groundY(this.g.level(), spot.x, Math.max(spot.y, head.y) + 1.0, spot.z);
            this.g.steerToward(new Vec3(spot.x, floor + BITE_HEIGHT, spot.z), 1.2, 18.0F);
            if (this.st % 3 == 0) {
                this.g.particle(ParticleTypes.CLOUD, head, this.g.look().scale(-1.0), 0.25);
            }
            double horiz = Math.sqrt(away.x * away.x + away.z * away.z);
            boolean low = head.y - floor < BITE_HEIGHT + 2.5;
            if ((horiz < 6.0 && low) || this.st >= 40) {
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
            this.g.holdHeight(new Vec3(look.x, 0, look.z).scale(-0.12), BITE_HEIGHT);
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
                this.setState(RECOIL);
            }
        } else if (this.state == RECOIL) {
            this.g.holdHeight(this.flat.scale(-0.4), BITE_HEIGHT);
            this.g.face(this.flat);
            if (this.st >= 8) {
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
            this.g.holdHeight(Vec3.ZERO, BITE_HEIGHT);
            float f = Math.min(1.0F, (this.st + 1) / (float) (SWEEP_TICKS - 6));
            this.g.headSweep(this.flat, f, 5.0, EotsConfig.CHAIN_BITE_SWEEP_MULTIPLIER.get(), this.sweepHit);
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
