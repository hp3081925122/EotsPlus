package com.eotsplus.ai;

import com.eotsplus.ai.skill.ChainBiteSkill;
import com.eotsplus.ai.skill.CrescentBladeSkill;
import com.eotsplus.ai.skill.EotsSkill;
import com.eotsplus.ai.skill.GaleCoilSkill;
import com.eotsplus.ai.skill.InhaleSkill;
import com.eotsplus.ai.skill.RoarSkill;
import com.eotsplus.ai.skill.ScaleRainSkill;
import com.eotsplus.ai.skill.SerpentTrailSkill;
import com.eotsplus.ai.skill.ShatterCrystalSkill;
import com.eotsplus.ai.skill.ShedTornadoSkill;
import com.eotsplus.ai.skill.SkyRendSkill;
import com.eotsplus.ai.skill.StormCollapseSkill;
import com.eotsplus.config.EotsConfig;

import java.util.function.Function;

public enum SkillType {
    SKY_REND(EotsConfig.SKY_REND, false, SkyRendSkill::new),
    CHAIN_BITE(EotsConfig.CHAIN_BITE, false, ChainBiteSkill::new),
    SHATTER_CRYSTAL(EotsConfig.SHATTER_CRYSTAL, false, ShatterCrystalSkill::new),
    GALE_COIL(EotsConfig.GALE_COIL, true, GaleCoilSkill::new),
    CRESCENT_BLADE(EotsConfig.CRESCENT_BLADE, false, CrescentBladeSkill::new),
    SERPENT_TRAIL(EotsConfig.SERPENT_TRAIL, false, SerpentTrailSkill::new),
    SCALE_RAIN(EotsConfig.SCALE_RAIN, true, ScaleRainSkill::new),
    INHALE(EotsConfig.INHALE, true, InhaleSkill::new),
    SHED_TORNADO(EotsConfig.SHED_TORNADO, true, ShedTornadoSkill::new),
    STORM_COLLAPSE(EotsConfig.STORM_COLLAPSE, true, StormCollapseSkill::new),
    ROAR(null, true, RoarSkill::new);

    public final EotsConfig.SkillConfig config;
    public final boolean big;
    private final Function<EotsSkillGoal, EotsSkill> factory;

    SkillType(EotsConfig.SkillConfig config, boolean big, Function<EotsSkillGoal, EotsSkill> factory) {
        this.config = config;
        this.big = big;
        this.factory = factory;
    }

    public EotsSkill create(EotsSkillGoal goal) {
        return this.factory.apply(goal);
    }

    public double damage() {
        if (this.config == null) {
            return 0.0;
        }
        return this.config.damageMultiplier.get();
    }
}
