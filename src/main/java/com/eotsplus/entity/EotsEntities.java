package com.eotsplus.entity;

import com.eotsplus.EotsPlus;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class EotsEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, EotsPlus.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GaleOrb>> GALE_ORB = ENTITIES.register("gale_orb", () -> EntityType.Builder.<GaleOrb>of(GaleOrb::new, MobCategory.MISC).sized(0.9F, 0.9F).clientTrackingRange(8).updateInterval(1).build("gale_orb"));
    public static final DeferredHolder<EntityType<?>, EntityType<GaleShard>> GALE_SHARD = ENTITIES.register("gale_shard", () -> EntityType.Builder.<GaleShard>of(GaleShard::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(1).build("gale_shard"));
    public static final DeferredHolder<EntityType<?>, EntityType<GaleTornado>> GALE_TORNADO = ENTITIES.register("gale_tornado", () -> EntityType.Builder.<GaleTornado>of(GaleTornado::new, MobCategory.MISC).sized(2.4F, 6.0F).clientTrackingRange(10).updateInterval(1).build("gale_tornado"));

    public static final DeferredHolder<EntityType<?>, EntityType<GaleCrescent>> GALE_CRESCENT = ENTITIES.register("gale_crescent", () -> EntityType.Builder.<GaleCrescent>of(GaleCrescent::new, MobCategory.MISC).sized(2.2F, 0.5F).clientTrackingRange(8).updateInterval(1).build("gale_crescent"));

    private EotsEntities() {
    }
}
