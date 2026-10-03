package com.hoghunter.item;

import com.hoghunter.HogHunterMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Damage preserves player attribution and restores armor even when a damage listener rejects a hit. */
public final class HogWeaponDamage {
    private static final ResourceLocation PIERCE = HogHunterMod.id("weapon_armor_pierce");
    private HogWeaponDamage() {}

    public static boolean apply(LivingEntity target, Entity attacker, float damage, float armorPierce) {
        if (target.level().isClientSide() || !target.isAlive() || damage <= 0) return false;
        DamageSource source = attacker instanceof Player player ? target.damageSources().playerAttack(player)
                : attacker instanceof LivingEntity living ? target.damageSources().mobAttack(living)
                : target.damageSources().generic();
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        boolean piercing = armor != null && armorPierce > 0 && armor.getModifier(PIERCE) == null;
        if (piercing) armor.addTransientModifier(new AttributeModifier(PIERCE,
                -Math.min(armor.getValue(), armorPierce), AttributeModifier.Operation.ADD_VALUE));
        try {
            return target.hurt(source, damage);
        } finally {
            if (piercing) armor.removeModifier(PIERCE);
        }
    }
}
