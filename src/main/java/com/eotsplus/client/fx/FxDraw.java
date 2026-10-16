package com.eotsplus.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class FxDraw {
    private FxDraw() {
    }

    public static float r(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    public static float g(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    public static float b(int color) {
        return (color & 0xFF) / 255.0F;
    }

    public static void v(VertexConsumer vc, Matrix4f m, double x, double y, double z, int color, float alpha) {
        vc.vertex(m, (float) x, (float) y, (float) z).color(r(color), g(color), b(color), Mth.clamp(alpha, 0.0F, 1.0F)).endVertex();
    }

    public static void quad(VertexConsumer vc, Matrix4f m, double[] a, double[] b, double[] c, double[] d, int color, float alpha) {
        v(vc, m, a[0], a[1], a[2], color, alpha);
        v(vc, m, b[0], b[1], b[2], color, alpha);
        v(vc, m, c[0], c[1], c[2], color, alpha);
        v(vc, m, d[0], d[1], d[2], color, alpha);
    }

    public static void tri(VertexConsumer vc, Matrix4f m, double[] a, double[] b, double[] c, int color, float alpha) {
        quad(vc, m, a, b, c, c, color, alpha);
    }

    public static void ring(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double rIn, double rOut, int segments, int color, float alphaIn, float alphaOut) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            double c0 = Math.cos(a0);
            double s0 = Math.sin(a0);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            v(vc, m, cx + c0 * rIn, cy, cz + s0 * rIn, color, alphaIn);
            v(vc, m, cx + c0 * rOut, cy, cz + s0 * rOut, color, alphaOut);
            v(vc, m, cx + c1 * rOut, cy, cz + s1 * rOut, color, alphaOut);
            v(vc, m, cx + c1 * rIn, cy, cz + s1 * rIn, color, alphaIn);
        }
    }

    public static void wall(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double radius, double height, int segments, int color, float alphaBottom, float alphaTop) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            double x0 = cx + Math.cos(a0) * radius;
            double z0 = cz + Math.sin(a0) * radius;
            double x1 = cx + Math.cos(a1) * radius;
            double z1 = cz + Math.sin(a1) * radius;
            v(vc, m, x0, cy, z0, color, alphaBottom);
            v(vc, m, x1, cy, z1, color, alphaBottom);
            v(vc, m, x1, cy + height, z1, color, alphaTop);
            v(vc, m, x0, cy + height, z0, color, alphaTop);
        }
    }

    public static void spiral(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double rBottom, double rTop, double height, int bands, double twist, double phase, double thickness, int steps, int color, float alpha) {
        for (int band = 0; band < bands; band++) {
            double base = Math.PI * 2.0 * band / bands + phase;
            for (int i = 0; i < steps; i++) {
                double f0 = i / (double) steps;
                double f1 = (i + 1) / (double) steps;
                double h0 = f0 * height;
                double h1 = f1 * height;
                double r0 = Mth.lerp(f0, rBottom, rTop);
                double r1 = Mth.lerp(f1, rBottom, rTop);
                double a0 = base + h0 * twist;
                double a1 = base + h1 * twist;
                float fade0 = (float) (alpha * Math.sin(Math.PI * f0));
                float fade1 = (float) (alpha * Math.sin(Math.PI * f1));
                double x0 = cx + Math.cos(a0) * r0;
                double z0 = cz + Math.sin(a0) * r0;
                double x1 = cx + Math.cos(a1) * r1;
                double z1 = cz + Math.sin(a1) * r1;
                v(vc, m, x0, cy + h0, z0, color, fade0);
                v(vc, m, x1, cy + h1, z1, color, fade1);
                v(vc, m, x1, cy + h1 + thickness, z1, color, fade1 * 0.2F);
                v(vc, m, x0, cy + h0 + thickness, z0, color, fade0 * 0.2F);
            }
        }
    }

    public static void octahedron(VertexConsumer vc, Matrix4f m, double rx, double ry, double rz, int color, float alpha, int tipColor, float tipAlpha) {
        double[] top = {0, ry, 0};
        double[] bottom = {0, -ry, 0};
        double[][] mid = {{rx, 0, 0}, {0, 0, rz}, {-rx, 0, 0}, {0, 0, -rz}};
        for (int i = 0; i < 4; i++) {
            double[] a = mid[i];
            double[] b = mid[(i + 1) % 4];
            v(vc, m, top[0], top[1], top[2], tipColor, tipAlpha);
            v(vc, m, a[0], a[1], a[2], color, alpha);
            v(vc, m, b[0], b[1], b[2], color, alpha);
            v(vc, m, b[0], b[1], b[2], color, alpha);
            v(vc, m, bottom[0], bottom[1], bottom[2], tipColor, tipAlpha);
            v(vc, m, b[0], b[1], b[2], color, alpha);
            v(vc, m, a[0], a[1], a[2], color, alpha);
            v(vc, m, a[0], a[1], a[2], color, alpha);
        }
    }
}
