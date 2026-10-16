package com.eotsplus.validation;

import com.eotsplus.ai.*;
import com.eotsplus.ai.skill.EotsSkill;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.duck.EotsSegmentDuck;
import com.eotsplus.network.FxPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;
import io.github.razordevs.deep_aether.init.DAEntities;
import java.util.*;

@GameTestHolder("eotsplus")
public final class EotsMigrationTests {
    @GameTestGenerator
    public static Collection<TestFunction> skills() {
        List<TestFunction> tests = new ArrayList<>();
        for (SkillType type : SkillType.values()) {
            for (int stage = 1; stage <= 3; stage++) {
                int phase = stage;
                tests.add(new TestFunction("eotsplus", "eotsplus." + type.name().toLowerCase(Locale.ROOT) + "_stage_" + stage,
                        "eotsplus:empty", 1000, 0L, true, helper -> exercise(helper, type, phase)));
            }
        }
        tests.add(new TestFunction("eotsplus", "eotsplus.damage_resistance_packet", "eotsplus:empty", 40, 0L, true, EotsMigrationTests::fundamentals));
        tests.add(new TestFunction("eotsplus", "eotsplus.phase_thresholds", "eotsplus:empty", 80, 0L, true, EotsMigrationTests::phases));
        tests.add(new TestFunction("eotsplus", "eotsplus.boss_start_chain", "eotsplus:empty", 80, 0L, true, EotsMigrationTests::bossStart));
        return tests;
    }

    private static void exercise(GameTestHelper helper, SkillType type, int stage) {
        EOTSSegment head = helper.spawn(DAEntities.EOTS_SEGMENT.get(), new BlockPos(8, 7, 8));
        head.setControllingSegment(true);
        head.setNoAi(true);
        head.setNoGravity(true);
        head.noPhysics = true;
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(14, 2, 8));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
        target.setHealth(10000);
        head.setTarget(target);
        EotsSegmentDuck duck = EotsSegmentDuck.of(head);
        duck.eotsplus$setStage(stage);
        duck.eotsplus$setManual(true);
        EotsSkillGoal goal = new EotsSkillGoal(head) {
            @Override public int stage() { return stage; }
            @Override public EotsBrain brain() { return EotsBrain.of(head); }
            @Override public List<EOTSSegment> chain() { return List.of(head); }
            @Override public int idleY() { return (int) head.getY(); }
        };
        EotsSkill skill = type.create(goal);
        skill.start();
        int[] ticks = {0};
        helper.onEachTick(() -> {
            if (skill.isFinished()) {
                skill.stop();
                helper.assertTrue(ticks[0] > 0, "Skill did not tick");
                helper.assertTrue(Double.isFinite(head.getX()) && Double.isFinite(head.getY()) && Double.isFinite(head.getZ()), "Non-finite skill position");
                head.discard();
                target.discard();
                helper.succeed();
                return;
            }
            ticks[0]++;
            target.setHealth(10000);
            target.invulnerableTime = 0;
            skill.tickSkill();
        });
    }

    private static void bossStart(GameTestHelper helper) {
        var controller = helper.spawn(DAEntities.EOTS_CONTROLLER.get(), new BlockPos(16, 18, 16));
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        controller.hurt(controller.damageSources().playerAttack(player), 1);
        helper.assertTrue(controller.isBossFight(), "Boss did not awaken");
        helper.runAtTickTime(5, () -> {
            var segments = helper.getLevel().getEntitiesOfClass(EOTSSegment.class, controller.getBoundingBox().inflate(100),
                    segment -> segment.getController() == controller);
            helper.assertTrue(segments.size() >= 18, "Boss chain is incomplete");
            for (var segment : segments) {
                helper.assertTrue(segment.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof EotsSkillGoal), "Skill goal missing on real boss segment");
                helper.assertTrue(Math.abs(segment.getMaxHealth()-EotsConfig.SEGMENT_HEALTH.get()) < 0.01, "Segment health config not applied");
                segment.discard();
            }
            controller.discard();
            helper.succeed();
        });
    }

    private static void phases(GameTestHelper helper) {
        var controller = helper.spawn(DAEntities.EOTS_CONTROLLER.get(), new BlockPos(8, 10, 8));
        controller.setNoAi(true);
        EOTSSegment head = helper.spawn(DAEntities.EOTS_SEGMENT.get(), new BlockPos(8, 7, 8));
        head.setNoAi(true);
        head.setControllingSegment(true);
        head.setController(controller);
        EotsBrain brain = EotsBrain.of(head);
        brain.update(head);
        helper.assertTrue(brain.stage == 1, "Full health must be phase one");
        controller.setHealth((float)(controller.getMaxHealth() * (EotsConfig.STAGE_TWO_HEALTH.get() - 0.01)));
        helper.runAtTickTime(12, () -> {
            brain.update(head);
            helper.assertTrue(brain.stage == 2 && brain.pendingRoar == 2, "Phase two threshold or roar failed");
            controller.setHealth((float)(controller.getMaxHealth() * (EotsConfig.STAGE_THREE_HEALTH.get() - 0.01)));
        });
        helper.runAtTickTime(24, () -> {
            brain.update(head);
            helper.assertTrue(brain.stage == 3 && brain.pendingRoar == 3, "Phase three threshold or roar failed");
            helper.assertTrue(brain.cooldownMultiplier() == EotsConfig.STAGE_THREE_COOLDOWN_MULTIPLIER.get(), "Phase cooldown changed");
            head.discard();
            controller.discard();
            helper.succeed();
        });
    }

    private static void fundamentals(GameTestHelper helper) {
        EOTSSegment head = helper.spawn(DAEntities.EOTS_SEGMENT.get(), new BlockPos(3, 2, 3));
        head.setControllingSegment(true);
        head.setNoAi(true);
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 3));
        target.setNoAi(true);
        head.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        float health = target.getHealth();
        helper.assertTrue(EotsCombat.hurt(head, target, 1.5), "Scaled attack did not hit");
        helper.assertTrue(Math.abs((health - target.getHealth()) - 12) < 0.01, "Attack multiplier changed");
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        target.setDeltaMovement(Vec3.ZERO);
        EotsCombat.push(target, new Vec3(1,0,0), 1, 0.5);
        helper.assertTrue(target.getDeltaMovement().lengthSqr() == 0, "Knockback resistance ignored");
        EotsSegmentDuck.of(head).eotsplus$setStage(3);
        helper.assertTrue(EotsSegmentDuck.of(head).eotsplus$getStage() == 3, "Stage data did not synchronize");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FxPacket packet = new FxPacket(4, 1, 2, 3, 4, 5, 6, 7, 8, 90, 0xA8ECFF);
            FxPacket.encode(packet, buf);
            helper.assertTrue(packet.equals(FxPacket.decode(buf)), "FX packet roundtrip mismatch");
        } finally {
            buf.release();
        }
        head.discard();
        target.discard();
        helper.succeed();
    }
}
