package com.eotsplus.client.anim;

import com.eotsplus.anim.EotsAnim;
import com.eotsplus.duck.EotsSegmentDuck;
import net.minecraft.util.Mth;
import teamrazor.deepaether.entity.eots.EOTSSegment;

public final class EotsPose {
    public static final EotsPose INSTANCE = new EotsPose();

    public boolean active;
    public boolean head;
    public float headPitch;
    public float headYaw;
    public float headPull;
    public float jaw;
    public float frill;
    public float fin;
    public float headScale;
    public float finFlare;
    public float bodyPitch;
    public float bodyRoll;
    public float bodyScale;
    public float shake;
    public float overlay;

    private EotsPose() {
    }

    private void reset() {
        this.active = false;
        this.headPitch = 0.0F;
        this.headYaw = 0.0F;
        this.headPull = 0.0F;
        this.jaw = 0.0F;
        this.frill = 0.0F;
        this.fin = 0.0F;
        this.headScale = 1.0F;
        this.finFlare = 0.0F;
        this.bodyPitch = 0.0F;
        this.bodyRoll = 0.0F;
        this.bodyScale = 1.0F;
        this.shake = 0.0F;
        this.overlay = 0.0F;
    }

    private static float smooth(float x) {
        float c = Mth.clamp(x, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    public EotsPose compute(EOTSSegment seg, float partial) {
        this.reset();
        EotsSegmentDuck duck = EotsSegmentDuck.of(seg);
        this.head = seg.isControllingSegment();
        int stage = duck.eotsplus$getStage();
        if (stage >= 3) {
            this.overlay = 0.35F;
        }
        int id = duck.eotsplus$getAnimId();
        if (id == EotsAnim.IDLE) {
            return this;
        }
        this.active = true;
        float t = Math.max(0.0F, duck.eotsplus$getAnimTicks(partial));
        float idx = duck.eotsplus$getChainIndex();
        switch (id) {
            case EotsAnim.REAR -> {
                float k = smooth(t / 10.0F);
                this.headPitch = -0.55F * k + 0.03F * Mth.sin(t * 2.5F) * k;
                this.jaw = 0.45F * k;
                this.frill = 0.55F * k + 0.05F * Mth.sin(t * 3.0F) * k;
                this.fin = -0.45F * k;
                this.bodyPitch = -16.0F * k;
                float wave = Mth.sin(t * 0.6F - idx * 0.5F);
                this.finFlare = 0.25F * k * (0.5F + 0.5F * wave);
            }
            case EotsAnim.DASH -> {
                this.jaw = 0.75F;
                this.frill = -0.35F;
                this.fin = 0.35F;
                this.bodyRoll = 8.0F * Mth.sin(t * 0.8F - idx * 0.4F);
            }
            case EotsAnim.BITE_WINDUP -> {
                float k = smooth(t / 6.0F);
                this.headPitch = -0.35F * k;
                this.headPull = 2.5F * k;
                this.jaw = 0.95F * k;
                this.frill = 0.6F * k + 0.06F * Mth.sin(t * 3.0F);
                this.bodyPitch = -10.0F * k;
            }
            case EotsAnim.BITE_SNAP -> {
                float close = 1.0F - smooth(t / 2.5F);
                float settle = 1.0F - smooth(t / 10.0F);
                this.jaw = 0.95F * close;
                this.headPitch = 0.28F * settle;
                this.headPull = -2.0F * settle;
                this.frill = 0.3F * settle;
                this.shake = 0.04F * settle;
            }
            case EotsAnim.CHARGE -> {
                float k = smooth(t / 25.0F);
                this.jaw = 0.25F + 0.45F * k;
                this.headYaw = 0.08F * k * Mth.sin(t * 1.7F);
                this.frill = 0.6F * k + 0.08F * k * Mth.sin(t * 4.0F);
                this.headScale = 1.0F + 0.12F * k;
                this.fin = -0.3F * k;
                this.shake = 0.015F * k;
            }
            case EotsAnim.FIRE -> {
                float r = 1.0F - smooth(t / 10.0F);
                this.headPitch = -0.45F * r;
                this.jaw = 0.1F + 0.85F * r;
                this.headScale = 1.0F + 0.12F * r - 0.08F * Mth.sin(Mth.clamp(t / 6.0F, 0.0F, 1.0F) * Mth.PI);
                this.frill = 0.7F * r;
                this.bodyPitch = -8.0F * r;
            }
            case EotsAnim.SCALE_RAISE -> {
                float k = smooth(t / 12.0F);
                float wave = Mth.sin(t * 0.35F - idx * 0.55F);
                this.headPitch = -0.4F * k;
                this.jaw = 0.3F + 0.05F * Mth.sin(t * 0.5F);
                this.frill = 0.8F * k + 0.1F * Mth.sin(t * 0.6F);
                this.fin = -0.5F * k;
                this.finFlare = k * (0.5F + 0.5F * wave);
                this.bodyScale = 1.0F + 0.06F * this.finFlare;
            }
            case EotsAnim.INHALE -> {
                float k = smooth(t / 30.0F);
                float wave = Mth.sin(t * 0.5F + idx * 0.6F);
                this.jaw = 0.5F + 0.55F * k;
                this.headScale = 1.0F + 0.25F * k + 0.02F * Mth.sin(t * 1.5F);
                this.frill = -0.3F * k;
                this.headPitch = -0.15F * k;
                this.bodyScale = 1.0F + 0.08F * wave * k;
                this.finFlare = 0.2F * Math.max(0.0F, wave) * k;
            }
            case EotsAnim.EXHALE -> {
                float r = 1.0F - smooth(t / 18.0F);
                float snap = Math.max(0.0F, 1.0F - t / 3.0F);
                this.jaw = 0.1F + 1.0F * r;
                this.headPitch = 0.2F * r;
                this.headScale = 1.0F + 0.25F * snap - 0.06F * Mth.sin(Mth.clamp(t / 10.0F, 0.0F, 1.0F) * Mth.PI);
                this.frill = 0.9F * r;
                this.headPull = -1.5F * r;
                this.shake = 0.06F * r;
            }
            case EotsAnim.SHED -> {
                float decay = 1.0F - smooth((t - 20.0F) / 12.0F);
                float sh = Mth.sin(t * 2.2F + idx * 0.9F) * decay;
                this.bodyRoll = 14.0F * sh;
                this.finFlare = 0.3F * Math.abs(sh);
                this.shake = 0.03F * decay;
                this.jaw = 0.4F;
                this.headPitch = -0.25F + 0.06F * Mth.sin(t * 2.6F);
                this.frill = 0.5F;
            }
            case EotsAnim.ULT_SPIRAL -> {
                this.jaw = 0.6F + 0.08F * Mth.sin(t * 0.9F);
                this.frill = 0.9F + 0.06F * Mth.sin(t * 3.0F);
                this.fin = -0.6F;
                this.bodyRoll = 22.0F * Mth.sin(t * 0.3F + idx * 0.4F);
                this.finFlare = 0.3F * (0.5F + 0.5F * Mth.sin(t * 0.4F - idx * 0.5F));
                this.overlay = Math.max(this.overlay, 0.9F);
            }
            case EotsAnim.ULT_DIVE -> {
                this.jaw = 1.15F;
                this.frill = -0.5F;
                this.fin = 0.5F;
                this.shake = 0.03F;
                this.overlay = 1.0F;
            }
            case EotsAnim.ROAR -> {
                float k = smooth(t / 8.0F) * (1.0F - smooth((t - 30.0F) / 10.0F));
                this.headPitch = -0.7F * k + 0.04F * k * Mth.sin(t * 3.1F);
                this.jaw = 1.2F * k;
                this.frill = 1.0F * k + 0.12F * k * Mth.sin(t * 5.0F);
                this.fin = -0.6F * k;
                this.headScale = 1.0F + 0.1F * k;
                this.bodyPitch = -20.0F * k;
                this.shake = 0.05F * k;
                this.finFlare = 0.4F * k * (0.5F + 0.5F * Mth.sin(t * 0.8F - idx * 0.6F));
                this.overlay = Math.max(this.overlay, k);
            }
            case EotsAnim.COIL -> {
                float k = smooth(t / 10.0F);
                float wave = Mth.sin(t * 0.5F - idx * 0.6F);
                this.jaw = 0.35F * k + 0.05F * Mth.sin(t * 0.7F);
                this.frill = 0.5F * k + 0.06F * Mth.sin(t * 2.4F) * k;
                this.fin = -0.3F * k;
                this.headYaw = 0.22F * k;
                this.bodyRoll = 14.0F * k + 4.0F * wave * k;
                this.finFlare = 0.2F * k * (0.5F + 0.5F * wave);
            }
            case EotsAnim.CONSTRICT -> {
                float k = smooth(t / 6.0F);
                this.jaw = 0.85F * k;
                this.headPitch = 0.25F * k;
                this.headYaw = 0.3F * k;
                this.frill = 0.9F * k + 0.1F * Mth.sin(t * 4.0F) * k;
                this.fin = -0.5F * k;
                this.bodyScale = 1.0F - 0.1F * k;
                this.bodyRoll = 20.0F * k;
                this.finFlare = 0.6F * k;
                this.shake = 0.03F * k;
                this.overlay = Math.max(this.overlay, 0.5F * k);
            }
            case EotsAnim.BLADE_WINDUP -> {
                float k = smooth(t / 12.0F);
                float wave = Mth.sin(t * 0.6F - idx * 0.5F);
                this.headYaw = -0.45F * k;
                this.headPitch = -0.15F * k;
                this.jaw = 0.3F * k;
                this.frill = 0.6F * k + 0.05F * Mth.sin(t * 3.0F) * k;
                this.fin = -0.4F * k;
                this.bodyRoll = -10.0F * k;
                this.finFlare = 0.3F * k * (0.5F + 0.5F * wave);
            }
            case EotsAnim.BLADE_THROW -> {
                float r = 1.0F - smooth(t / 10.0F);
                float snap = smooth(t / 3.0F);
                this.headYaw = (-0.45F + 0.85F * snap) * r;
                this.jaw = 0.7F * r;
                this.frill = 0.7F * r;
                this.fin = 0.3F * r;
                this.bodyRoll = 12.0F * snap * r;
                this.shake = 0.02F * r;
            }
            case EotsAnim.SERPENT -> {
                float wave = Mth.sin(t * 0.45F - idx * 0.7F);
                this.jaw = 0.55F + 0.05F * Mth.sin(t * 1.3F);
                this.frill = -0.2F;
                this.fin = 0.3F;
                this.headYaw = 0.25F * Mth.sin(t * 0.45F);
                this.bodyRoll = 16.0F * wave;
                this.finFlare = 0.25F * (0.5F + 0.5F * wave);
            }
            case EotsAnim.STUCK -> {
                float k = smooth(t / 3.0F);
                float struggle = Mth.sin(t * 1.9F);
                this.headPitch = 0.55F * k + 0.06F * struggle;
                this.headYaw = 0.12F * Mth.sin(t * 1.3F);
                this.jaw = 0.55F + 0.15F * Mth.sin(t * 2.4F);
                this.frill = 0.4F + 0.2F * Math.abs(struggle);
                this.fin = 0.35F;
                this.bodyRoll = 10.0F * Mth.sin(t * 0.9F - idx * 0.5F);
                this.shake = 0.05F * k;
            }
            case EotsAnim.SWEEP -> {
                float swing = smooth(t / 10.0F);
                float settle = 1.0F - smooth((t - 10.0F) / 8.0F);
                this.headYaw = (-0.7F + 1.4F * swing) * settle;
                this.jaw = 0.7F * settle;
                this.frill = 0.8F * settle;
                this.fin = -0.3F * settle;
                this.bodyRoll = (-18.0F + 36.0F * swing) * settle;
                this.finFlare = 0.4F * settle * (0.5F + 0.5F * Mth.sin(t * 0.8F - idx * 0.5F));
                this.shake = 0.02F * settle;
            }
            case EotsAnim.DOWNED -> {
                float k = smooth(t / 6.0F);
                float breathe = Mth.sin(t * 0.25F);
                this.headPitch = 0.4F * k;
                this.jaw = 0.25F + 0.1F * breathe;
                this.frill = -0.45F * k;
                this.fin = 0.4F * k;
                this.headScale = 1.0F + 0.03F * breathe;
                this.bodyScale = 1.0F + 0.04F * Mth.sin(t * 0.25F - idx * 0.4F);
                this.bodyRoll = 6.0F * Mth.sin(t * 0.18F + idx * 0.3F);
            }
            case EotsAnim.PROWL -> {
                float k = smooth(t / 8.0F);
                float wave = Mth.sin(t * 0.3F - idx * 0.6F);
                this.headPitch = 0.12F * k;
                this.jaw = 0.25F * k + 0.08F * Math.max(0.0F, Mth.sin(t * 0.7F));
                this.frill = 0.45F * k + 0.08F * Mth.sin(t * 2.8F) * k;
                this.fin = -0.2F * k;
                this.headYaw = 0.1F * Mth.sin(t * 0.2F);
                this.bodyRoll = 8.0F * wave * k;
            }
            default -> this.active = false;
        }
        if (!this.head) {
            this.bodyPitch = 0.0F;
        }
        return this;
    }
}
