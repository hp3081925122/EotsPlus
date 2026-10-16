package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.entity.GaleCrescent;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.init.DASounds;

import java.util.ArrayList;
import java.util.List;

public class CrescentBladeSkill extends EotsSkill {
    private static final int VOLLEY_GAP = 24;
    private static final int WARN = 8;
    private static final double FAN_DEGREES = 70.0;

    private final List<Vec3> dirs = new ArrayList<>();
    private int windup;
    private int count;
    private int volleys;
    private int fired;
    private double cruiseY;

    public CrescentBladeSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        if (this.g.target() == null) {
            this.finish();
            return;
        }
        this.windup = 16;
        this.count = EotsConfig.CRESCENT_COUNT.get();
        this.volleys = 1;
        if (this.g.stage() >= 2) {
            this.volleys = 2;
        }
        if (this.g.stage() >= 3) {
            this.windup = 12;
            this.count += 2;
        }
        this.g.anim(EotsAnim.BLADE_WINDUP);
        this.g.seg.setMouthOpen(true);
        this.g.sound(DASounds.EOTS_BLOWING.get(), 2.5F, 1.5F);
    }

    @Override
    protected void tick() {
        this.g.hover();
        this.g.lookAtTarget(12.0F);
        int fireTick = this.windup + this.fired * VOLLEY_GAP;
        if (this.fired < this.volleys) {
            if (this.t == fireTick - WARN) {
                this.warn();
            }
            if (this.t < fireTick && this.t % 2 == 0) {
                Vec3 mouth = this.g.mouth();
                Vec3 off = this.g.randomUnit().scale(2.2);
                this.g.particle(ParticleTypes.CLOUD, mouth.add(off), off.scale(-1.0).normalize(), 0.3);
            }
            if (this.t == fireTick) {
                this.fire();
            }
            return;
        }
        if (this.t >= this.windup + (this.volleys - 1) * VOLLEY_GAP + 22) {
            this.finish();
        }
    }

    private void warn() {
        LivingEntity target = this.g.target();
        Vec3 mouth = this.g.mouth();
        Vec3 aim;
        if (target != null) {
            aim = target.position();
            this.cruiseY = target.getY() + 0.6;
        } else {
            aim = mouth.add(this.g.look().scale(10.0));
            this.cruiseY = EotsCombat.groundY(this.g.level(), mouth.x, mouth.y, mouth.z) + 0.6;
        }
        Vec3 flat = new Vec3(aim.x - mouth.x, 0, aim.z - mouth.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(this.g.look().x, 0, this.g.look().z);
        }
        flat = flat.normalize();
        this.dirs.clear();
        double offset = 0.0;
        if (this.fired % 2 == 1 && this.count > 1) {
            offset = FAN_DEGREES / (this.count - 1) * 0.5;
        }
        for (int i = 0; i < this.count; i++) {
            double deg = 0.0;
            if (this.count > 1) {
                deg = -FAN_DEGREES * 0.5 + FAN_DEGREES * i / (this.count - 1);
            }
            double r = Math.toRadians(deg + offset);
            double cos = Math.cos(r);
            double sin = Math.sin(r);
            this.dirs.add(new Vec3(flat.x * cos - flat.z * sin, 0, flat.x * sin + flat.z * cos));
        }
        ServerLevel level = this.g.level();
        double range = EotsConfig.CRESCENT_RANGE.get();
        Vec3 base = EotsCombat.onGround(level, mouth).add(0, 0.05, 0);
        for (Vec3 d : this.dirs) {
            EotsFx.line(level, base, base.add(d.scale(range)), 1.6F, WARN + 14, FxType.DANGER);
        }
        if (this.fired > 0) {
            this.g.anim(EotsAnim.BLADE_WINDUP);
        }
    }

    private void fire() {
        ServerLevel level = this.g.level();
        Vec3 mouth = this.g.mouth();
        float damage = (float) (EotsCombat.attack(this.g.seg) * EotsConfig.CRESCENT_BLADE.damageMultiplier.get());
        double speed = GaleCrescent.speedForRange(EotsConfig.CRESCENT_RANGE.get());
        for (Vec3 d : this.dirs) {
            GaleCrescent.spawn(level, this.g.seg, mouth, d.scale(speed), this.cruiseY, damage);
        }
        this.g.anim(EotsAnim.BLADE_THROW);
        this.g.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 3.0F, 0.5F);
        this.g.sound(DASounds.EOTS_SHOOT.get(), 2.5F, 1.2F);
        this.g.burst(ParticleTypes.SWEEP_ATTACK, mouth, 3, 0.6, 0.0);
        EotsFx.shake(level, mouth, 0.2F, 16.0F, 6);
        this.fired++;
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
