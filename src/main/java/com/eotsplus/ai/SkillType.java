package com.eotsplus.ai;

import com.eotsplus.ai.skill.CrescentBladeSkill;
import com.eotsplus.ai.skill.EotsSkill;
import com.eotsplus.ai.skill.GaleCoilSkill;
import com.eotsplus.ai.skill.GaleVolleySkill;
import com.eotsplus.ai.skill.RoarSkill;
import com.eotsplus.ai.skill.ScaleRainSkill;
import com.eotsplus.ai.skill.SerpentTrailSkill;
import com.eotsplus.ai.skill.ShatterCrystalSkill;
import com.eotsplus.ai.skill.ShedTornadoSkill;
import com.eotsplus.ai.skill.SkyRendSkill;
import com.eotsplus.ai.skill.StormCollapseSkill;
import com.eotsplus.ai.skill.WindLanceSkill;
import com.eotsplus.ai.skill.WindPrismSkill;
import com.eotsplus.config.EotsConfig;

import java.util.function.Function;

public enum SkillType {
    SKY_REND(EotsConfig.SKY_REND, false, true, SkyRendSkill::new),
    SHATTER_CRYSTAL(EotsConfig.SHATTER_CRYSTAL, false, false, ShatterCrystalSkill::new),
    GALE_COIL(EotsConfig.GALE_COIL, true, true, GaleCoilSkill::new),
    CRESCENT_BLADE(EotsConfig.CRESCENT_BLADE, false, false, CrescentBladeSkill::new),
    SERPENT_TRAIL(EotsConfig.SERPENT_TRAIL, false, false, SerpentTrailSkill::new),
    SCALE_RAIN(EotsConfig.SCALE_RAIN, true, false, ScaleRainSkill::new),
    GALE_VOLLEY(EotsConfig.GALE_VOLLEY, false, false, GaleVolleySkill::new),
    WIND_LANCE(EotsConfig.WIND_LANCE, false, false, WindLanceSkill::new),
    WIND_PRISM(EotsConfig.WIND_PRISM, true, false, WindPrismSkill::new),
    SHED_TORNADO(EotsConfig.SHED_TORNADO, true, false, ShedTornadoSkill::new),
    STORM_COLLAPSE(EotsConfig.STORM_COLLAPSE, true, true, StormCollapseSkill::new),
    ROAR(null, true, false, RoarSkill::new);

    public final EotsConfig.SkillConfig config;
    public final boolean big;
    public final boolean melee;
    private final Function<EotsSkillGoal, EotsSkill> factory;

    SkillType(EotsConfig.SkillConfig config, boolean big, boolean melee, Function<EotsSkillGoal, EotsSkill> factory) {
        this.config = config;
        this.big = big;
        this.melee = melee;
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
