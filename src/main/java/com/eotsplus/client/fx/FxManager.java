package com.eotsplus.client.fx;

import com.eotsplus.config.EotsClientConfig;
import com.eotsplus.fx.FxType;
import com.eotsplus.network.FxPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FxManager {
    private static final List<Fx> ACTIVE = new ArrayList<>();

    private FxManager() {
    }

    public static void accept(FxPacket p) {
        if (p.kind() == FxType.SHAKE) {
            CameraShake.add(new Vec3(p.x(), p.y(), p.z()), p.a(), p.radius(), p.duration());
            return;
        }
        if (!EotsClientConfig.ENABLE_FX.get()) {
            return;
        }
        ACTIVE.add(new Fx(p));
    }

    public static void tick() {
        Iterator<Fx> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Fx fx = it.next();
            fx.age++;
            if (fx.age >= fx.p.duration()) {
                it.remove();
            }
        }
        CameraShake.tick();
    }

    public static void clear() {
        ACTIVE.clear();
        CameraShake.clear();
    }

    public static void render(PoseStack ps, Vec3 cam, float partial) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(FxRenderTypes.FX);
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        for (Fx fx : ACTIVE) {
            float age = fx.age + partial;
            float p = Mth.clamp(age / Math.max(1, fx.p.duration()), 0.0F, 1.0F);
            float fade = Math.min(1.0F, age / 4.0F) * Math.min(1.0F, (fx.p.duration() - age) / 5.0F);
            fade = Mth.clamp(fade, 0.0F, 1.0F);
            switch (fx.p.kind()) {
                case FxType.CIRCLE -> drawCircle(vc, m, fx.p, age, p, fade);
                case FxType.LINE -> drawLine(vc, m, fx.p, age, p, fade);
                case FxType.FAN -> drawFan(vc, m, fx.p, age, p, fade);
                case FxType.SHOCKWAVE -> drawShockwave(vc, m, fx.p, p);
                case FxType.COLUMN -> drawColumn(vc, m, fx.p, age, fade);
                default -> {
                }
            }
        }
        ps.popPose();
        buffers.endBatch(FxRenderTypes.FX);
    }

    private static void drawCircle(VertexConsumer vc, Matrix4f m, FxPacket f, float age, float p, float fade) {
        double r = f.radius();
        int seg = Math.max(24, (int) (r * 10));
        float pulse = 0.0F;
        if (p > 0.75F) {
            pulse = (float) (0.5 + 0.5 * Math.sin(age * 1.6));
        }
        FxDraw.ring(vc, m, f.x(), f.y(), f.z(), r - 0.16, r, seg, f.color(), 0.9F * fade, 0.9F * fade);
        FxDraw.ring(vc, m, f.x(), f.y(), f.z(), 0.0, r * p, seg, f.color(), 0.08F * fade, (0.28F + 0.3F * pulse) * fade);
        FxDraw.ring(vc, m, f.x(), f.y() + 0.01, f.z(), Math.max(0.0, r * p - 0.12), r * p, seg, f.color(), 0.7F * fade, 0.7F * fade);
        FxDraw.wall(vc, m, f.x(), f.y(), f.z(), r, 0.45 + 0.4 * pulse, seg, f.color(), 0.35F * fade, 0.0F);
    }

    private static void drawLine(VertexConsumer vc, Matrix4f m, FxPacket f, float age, float p, float fade) {
        double sx = f.x();
        double sy = f.y();
        double sz = f.z();
        double dx = f.a() - sx;
        double dz = f.c() - sz;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-3) {
            return;
        }
        double fx = dx / len;
        double fz = dz / len;
        double half = f.radius() * 0.5;
        double px = -fz * half;
        double pz = fx * half;
        double ey = f.b();
        int color = f.color();
        FxDraw.quad(vc, m, new double[]{sx + px, sy, sz + pz}, new double[]{f.a() + px, ey, f.c() + pz}, new double[]{f.a() - px, ey, f.c() - pz}, new double[]{sx - px, sy, sz - pz}, color, 0.16F * fade);
        double ex = px * 0.12;
        double ez = pz * 0.12;
        FxDraw.quad(vc, m, new double[]{sx + px, sy, sz + pz}, new double[]{f.a() + px, ey, f.c() + pz}, new double[]{f.a() + px - ex, ey, f.c() + pz - ez}, new double[]{sx + px - ex, sy, sz + pz - ez}, color, 0.85F * fade);
        FxDraw.quad(vc, m, new double[]{sx - px, sy, sz - pz}, new double[]{f.a() - px, ey, f.c() - pz}, new double[]{f.a() - px + ex, ey, f.c() - pz + ez}, new double[]{sx - px + ex, sy, sz - pz + ez}, color, 0.85F * fade);
        double spacing = 2.2;
        double shift = (age * 0.45) % spacing;
        for (double d = shift; d < len; d += spacing) {
            double t = d / len;
            double cx = sx + dx * t;
            double cy = sy + (ey - sy) * t + 0.01;
            double cz = sz + dz * t;
            double tipX = cx + fx * 0.9;
            double tipZ = cz + fz * 0.9;
            double w = 0.18;
            float a = 0.6F * fade * (float) Math.sin(Math.PI * t);
            FxDraw.quad(vc, m, new double[]{cx + px * 0.7, cy, cz + pz * 0.7}, new double[]{tipX, cy, tipZ}, new double[]{tipX - fx * w, cy, tipZ - fz * w}, new double[]{cx + px * 0.7 - fx * w, cy, cz + pz * 0.7 - fz * w}, color, a);
            FxDraw.quad(vc, m, new double[]{cx - px * 0.7, cy, cz - pz * 0.7}, new double[]{tipX, cy, tipZ}, new double[]{tipX - fx * w, cy, tipZ - fz * w}, new double[]{cx - px * 0.7 - fx * w, cy, cz - pz * 0.7 - fz * w}, color, a);
        }
    }

    private static void drawFan(VertexConsumer vc, Matrix4f m, FxPacket f, float age, float p, float fade) {
        Vec3 dir = new Vec3(f.a(), f.b(), f.c());
        if (dir.lengthSqr() < 1.0E-6) {
            return;
        }
        dir = dir.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up);
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 normalUp = side.cross(dir).normalize();
        double half = Math.toRadians(f.extra());
        double length = f.radius();
        Vec3 o = new Vec3(f.x(), f.y(), f.z());
        boolean outward = f.color() == FxType.DANGER;
        drawSector(vc, m, o, dir, side, half, length, age, fade, f.color(), outward);
        drawSector(vc, m, o, dir, normalUp, half, length, age, fade, f.color(), outward);
    }

    private static void drawSector(VertexConsumer vc, Matrix4f m, Vec3 o, Vec3 dir, Vec3 axis, double half, double length, float age, float fade, int color, boolean outward) {
        int seg = 16;
        for (int i = 0; i < seg; i++) {
            double a0 = -half + 2.0 * half * i / seg;
            double a1 = -half + 2.0 * half * (i + 1) / seg;
            Vec3 d0 = dir.scale(Math.cos(a0)).add(axis.scale(Math.sin(a0)));
            Vec3 d1 = dir.scale(Math.cos(a1)).add(axis.scale(Math.sin(a1)));
            Vec3 p0 = o.add(d0.scale(length));
            Vec3 p1 = o.add(d1.scale(length));
            FxDraw.v(vc, m, o.x, o.y, o.z, color, 0.2F * fade);
            FxDraw.v(vc, m, p0.x, p0.y, p0.z, color, 0.02F * fade);
            FxDraw.v(vc, m, p1.x, p1.y, p1.z, color, 0.02F * fade);
            FxDraw.v(vc, m, o.x, o.y, o.z, color, 0.2F * fade);
            for (int k = 0; k < 4; k++) {
                double phase = (k / 4.0 + age * 0.04) % 1.0;
                if (!outward) {
                    phase = 1.0 - phase;
                }
                double rr = length * phase;
                double w = 0.25;
                Vec3 q0 = o.add(d0.scale(rr));
                Vec3 q1 = o.add(d1.scale(rr));
                Vec3 q2 = o.add(d1.scale(rr + w));
                Vec3 q3 = o.add(d0.scale(rr + w));
                float a = (float) (0.55 * fade * Math.sin(Math.PI * phase));
                FxDraw.v(vc, m, q0.x, q0.y, q0.z, color, a);
                FxDraw.v(vc, m, q1.x, q1.y, q1.z, color, a);
                FxDraw.v(vc, m, q2.x, q2.y, q2.z, color, a);
                FxDraw.v(vc, m, q3.x, q3.y, q3.z, color, a);
            }
        }
    }

    private static void drawShockwave(VertexConsumer vc, Matrix4f m, FxPacket f, float p) {
        float ease = 1.0F - (1.0F - p) * (1.0F - p) * (1.0F - p);
        double r = f.radius() * ease;
        double width = 0.25 + 1.2 * (1.0F - p);
        float a = 1.0F - p;
        int seg = Math.max(32, (int) (f.radius() * 8));
        FxDraw.ring(vc, m, f.x(), f.y(), f.z(), Math.max(0.0, r - width), r, seg, f.color(), 0.0F, 0.85F * a);
        FxDraw.wall(vc, m, f.x(), f.y(), f.z(), r, 1.6 * (1.0F - p) + 0.2, seg, f.color(), 0.6F * a, 0.0F);
    }

    private static void drawColumn(VertexConsumer vc, Matrix4f m, FxPacket f, float age, float fade) {
        double r = f.radius();
        double h = f.extra();
        FxDraw.spiral(vc, m, f.x(), f.y(), f.z(), r * 0.55, r * 1.25, h, 3, 0.32, age * 0.22, 1.4, 48, f.color(), 0.55F * fade);
        FxDraw.spiral(vc, m, f.x(), f.y(), f.z(), r * 0.9, r * 1.6, h, 4, -0.22, -age * 0.15, 0.8, 48, FxType.WIND, 0.3F * fade);
        FxDraw.wall(vc, m, f.x(), f.y(), f.z(), r * 1.3, h * 0.35, 48, f.color(), 0.2F * fade, 0.0F);
        FxDraw.ring(vc, m, f.x(), f.y() + 0.03, f.z(), r * 0.2, r * 1.3, 48, f.color(), 0.0F, 0.35F * fade);
    }

    private static final class Fx {
        final FxPacket p;
        int age;

        Fx(FxPacket p) {
            this.p = p;
        }
    }
}
