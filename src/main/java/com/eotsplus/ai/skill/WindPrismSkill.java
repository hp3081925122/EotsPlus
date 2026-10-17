package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsHazards;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.entity.GaleShard;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.entity.eots.EOTSSegment;
import teamrazor.deepaether.init.DASounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindPrismSkill extends EotsSkill {
    private static final int CHARGE = 18;
    private static final int MAX_TARGETS = 4;

    private int warn;

    public WindPrismSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        if (this.g.target() == null) {
            this.finish();
            return;
        }
        this.warn = EotsConfig.WIND_PRISM_WARN.get();
        if (this.g.stage() >= 3) {
            this.warn = Math.max(10, this.warn * 3 / 4);
        }
        this.g.anim(EotsAnim.SCALE_RAISE);
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 3.0F, 1.3F);
    }

    @Override
    protected void tick() {
        this.g.holdHeight(Vec3.ZERO, EotsConfig.HOVER_HEIGHT.get() + 1.0);
        this.g.lookAtTarget(8.0F);
        if (this.t < CHARGE) {
            if (this.t % 3 == 0) {
                Vec3 head = this.g.pos();
                this.g.level().sendParticles(ParticleTypes.END_ROD, head.x, head.y + 1.0, head.z, 4, 1.2, 0.8, 1.2, 0.05);
                this.g.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, head.x, head.y + 0.5, head.z, 3, 0.8, 0.6, 0.8, 0.1);
            }
            return;
        }
        if (this.t == CHARGE) {
            this.cast();
        }
        if (this.t >= CHARGE + 14) {
            this.finish();
        }
    }

    private void cast() {
        ServerLevel level = this.g.level();
        List<LivingEntity> targets = EotsCombat.enemies(level, this.g.pos(), EotsConfig.COMBAT_RANGE.get());
        int pillars = EotsConfig.WIND_PRISM_PILLARS.get();
        if (this.g.stage() >= 3) {
            pillars++;
        }
        double radius = EotsConfig.WIND_PRISM_RADIUS.get();
        float damage = (float) (EotsCombat.attack(this.g.seg) * EotsConfig.WIND_PRISM.damageMultiplier.get());
        int duration = EotsConfig.WIND_PRISM_DURATION.get();
        int used = 0;
        for (LivingEntity e : targets) {
            if (used >= MAX_TARGETS) {
                break;
            }
            used++;
            double offset = this.g.seg.getRandom().nextDouble() * Math.PI * 2.0;
            List<Vec3> points = new ArrayList<>();
            for (int i = 0; i < pillars; i++) {
                double a = offset + Math.PI * 2.0 * i / pillars;
                Vec3 p = e.position().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
                points.add(EotsCombat.onGround(level, p.add(0, 2.0, 0)));
            }
            List<int[]> edges = new ArrayList<>();
            for (int i = 0; i < pillars; i++) {
                edges.add(new int[]{i, (i + 1) % pillars});
            }
            if (pillars == 4) {
                edges.add(new int[]{0, 2});
                edges.add(new int[]{1, 3});
            } else if (pillars >= 5) {
                for (int i = 0; i < pillars; i++) {
                    edges.add(new int[]{i, (i + 2) % pillars});
                }
            }
            for (Vec3 p : points) {
                EotsFx.circle(level, p.add(0, 0.04, 0), 0.9F, this.warn, FxType.STORM);
            }
            for (int[] edge : edges) {
                EotsFx.line(level, points.get(edge[0]).add(0, 0.05, 0), points.get(edge[1]).add(0, 0.05, 0), 1.0F, this.warn, FxType.DANGER);
            }
            EotsHazards.add(level, new Prism(this.g.seg, points, edges, damage, this.warn, duration));
        }
        this.g.anim(EotsAnim.FIRE);
        this.g.sound(DASounds.EOTS_SHOOT.get(), 3.0F, 1.4F);
        this.g.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5F, 0.7F);
        EotsFx.shockwave(level, this.g.pos(), 4.0F, 10, FxType.STORM);
    }

    private static final class Prism implements EotsHazards.Hazard {
        private static final double EDGE_RADIUS = 0.9;
        private final EOTSSegment owner;
        private final List<Vec3> points;
        private final List<int[]> edges;
        private final float damage;
        private final int duration;
        private final Map<Integer, Integer> lastHit = new HashMap<>();
        private int delay;
        private int active;

        Prism(EOTSSegment owner, List<Vec3> points, List<int[]> edges, float damage, int delay, int duration) {
            this.owner = owner;
            this.points = points;
            this.edges = edges;
            this.damage = damage;
            this.delay = delay;
            this.duration = duration;
        }

        @Override
        public boolean tick(ServerLevel level) {
            if (this.delay > 0) {
                if (this.delay == 10) {
                    for (Vec3 p : this.points) {
                        GaleShard.spawn(level, this.owner, p.add(0, 12.0, 0), new Vec3(0, -1.2, 0), 0.0F, 12);
                    }
                }
                this.delay--;
                if (this.delay == 0) {
                    this.ignite(level);
                }
                return false;
            }
            if (this.owner.isRemoved()) {
                return true;
            }
            if (this.active % 4 == 0) {
                for (int[] edge : this.edges) {
                    Vec3 a = this.points.get(edge[0]).add(0, 1.0, 0);
                    Vec3 b = this.points.get(edge[1]).add(0, 1.0, 0);
                    Vec3 mid = a.add(b).scale(0.5);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y, mid.z, 3, Math.abs(b.x - a.x) * 0.3, 0.3, Math.abs(b.z - a.z) * 0.3, 0.05);
                }
            }
            if (this.active % 10 == 0) {
                this.zap(level);
            }
            this.active++;
            return this.active >= this.duration;
        }

        private void ignite(ServerLevel level) {
            for (Vec3 p : this.points) {
                EotsFx.column(level, p, 0.7F, 3.2F, this.duration, FxType.STORM);
                level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.5, p.z, 12, 0.3, 0.5, 0.3, 0.15);
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.5, p.z, 6, 0.1, 1.0, 0.1, 0.02);
            }
            for (int[] edge : this.edges) {
                Vec3 a = this.points.get(edge[0]).add(0, 1.0, 0);
                Vec3 b = this.points.get(edge[1]).add(0, 1.0, 0);
                EotsFx.beam(level, a, b, 0.22F, 0.35F, this.duration, FxType.STORM);
                EotsFx.line(level, this.points.get(edge[0]).add(0, 0.05, 0), this.points.get(edge[1]).add(0, 0.05, 0), (float) (EDGE_RADIUS * 2.0), this.duration, FxType.STORM);
            }
            Vec3 c = this.points.get(0);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.0F, 1.4F);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F, 0.8F);
            EotsFx.shake(level, c, 0.3F, 14.0F, 8);
        }

        private void zap(ServerLevel level) {
            Vec3 center = Vec3.ZERO;
            for (Vec3 p : this.points) {
                center = center.add(p);
            }
            center = center.scale(1.0 / this.points.size());
            double reach = 0.0;
            for (Vec3 p : this.points) {
                reach = Math.max(reach, p.distanceTo(center));
            }
            for (LivingEntity e : EotsCombat.enemies(level, center.add(0, 1.0, 0), reach + 3.0)) {
                Vec3 push = null;
                for (int[] edge : this.edges) {
                    Vec3 a = this.points.get(edge[0]);
                    Vec3 b = this.points.get(edge[1]);
                    double dx = b.x - a.x;
                    double dz = b.z - a.z;
                    double len2 = dx * dx + dz * dz;
                    if (len2 < 1.0E-4) {
                        continue;
                    }
                    double t = ((e.getX() - a.x) * dx + (e.getZ() - a.z) * dz) / len2;
                    t = Math.max(0.0, Math.min(1.0, t));
                    double px = a.x + dx * t;
                    double pz = a.z + dz * t;
                    double py = a.y + (b.y - a.y) * t;
                    double ox = e.getX() - px;
                    double oz = e.getZ() - pz;
                    if (ox * ox + oz * oz > EDGE_RADIUS * EDGE_RADIUS || e.getY() < py - 1.0 || e.getY() > py + 2.5) {
                        continue;
                    }
                    push = new Vec3(ox, 0, oz);
                    break;
                }
                if (push == null) {
                    continue;
                }
                Integer last = this.lastHit.get(e.getId());
                if (last != null && this.active - last < 10) {
                    continue;
                }
                this.lastHit.put(e.getId(), this.active);
                EotsCombat.hurtIndirect(this.owner, this.owner, e, this.damage);
                EotsCombat.push(e, push, 0.25, 0.2);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1.0, e.getZ(), 10, 0.3, 0.5, 0.3, 0.2);
            }
        }
    }
}
