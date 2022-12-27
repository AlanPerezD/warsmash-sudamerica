package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks;

import java.util.EnumSet;
import java.util.Random;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CWeaponType;

/**
 * The base class for unit-data-based combat attacks.
 *
 * I really wanted to split this out into sub classes based on weapon type, but
 * I came to realize the Ballista in RoC probably had the spill distance effect
 * & area of effect both after it upgrades Impaling Bolt, and this would point
 * out that the behaviors were not mutually exclusive.
 *
 * Then I reviewed it and decided that in RoC, the Impaling Bolts upgrade did
 * not interact with the damage spill combat settings from the UnitWeapons.slk,
 * because many of those settings did not exist. So I will attempt to emulate
 * these attacks as best as possible.
 */
public abstract class CUnitAttack {
	private float animationBackswingPoint;
	private float animationDamagePoint;
	private CAttackType attackType;
	private float cooldownTime;
	private int damageBase;
	private int damageDice;
	private int damageSidesPerDie;
	private int damageUpgradeAmount;
	private int range;
	private float rangeMotionBuffer;
	private boolean showUI;
	private EnumSet<CTargetType> targetsAllowed;
	private String weaponSound;
	private CWeaponType weaponType;

	private int primaryAttributePermanentDamageBonus;
	private int primaryAttributeTemporaryDamageBonus;
	private int permanentDamageBonus;
	private int temporaryDamageBonus;

	// calculate
	private int totalBaseDamage;
	private int totalDamageDice;
	private int minDamageDisplay;
	private int maxDamageDisplay;
	private int totalTemporaryDamageBonus;

	public CUnitAttack(final float animationBackswingPoint, final float animationDamagePoint,
			final CAttackType attackType, final float cooldownTime, final int damageBase, final int damageDice,
			final int damageSidesPerDie, final int damageUpgradeAmount, final int range, final float rangeMotionBuffer,
			final boolean showUI, final EnumSet<CTargetType> targetsAllowed, final String weaponSound,
			final CWeaponType weaponType) {
		this.animationBackswingPoint = animationBackswingPoint;
		this.animationDamagePoint = animationDamagePoint;
		this.attackType = attackType;
		this.cooldownTime = cooldownTime;
		this.damageBase = damageBase;
		this.damageDice = damageDice;
		this.damageSidesPerDie = damageSidesPerDie;
		this.damageUpgradeAmount = damageUpgradeAmount;
		this.range = range;
		this.rangeMotionBuffer = rangeMotionBuffer;
		this.showUI = showUI;
		this.targetsAllowed = targetsAllowed;
		this.weaponSound = weaponSound;
		this.weaponType = weaponType;
		computeDerivedFields();
	}

	public CUnitAttack(final CUnitAttack other) {
		this.animationBackswingPoint = other.animationBackswingPoint;
		this.animationDamagePoint = other.animationDamagePoint;
		this.attackType = other.attackType;
		this.cooldownTime = other.cooldownTime;
		this.damageBase = other.damageBase;
		this.damageDice = other.damageDice;
		this.damageSidesPerDie = other.damageSidesPerDie;
		this.damageUpgradeAmount = other.damageUpgradeAmount;
		this.range = other.range;
		this.rangeMotionBuffer = other.rangeMotionBuffer;
		this.showUI = other.showUI;
		this.targetsAllowed = other.targetsAllowed;
		this.weaponSound = other.weaponSound;
		this.weaponType = other.weaponType;

		this.primaryAttributePermanentDamageBonus = other.primaryAttributePermanentDamageBonus;
		this.primaryAttributeTemporaryDamageBonus = other.primaryAttributeTemporaryDamageBonus;
		this.permanentDamageBonus = other.permanentDamageBonus;
		this.temporaryDamageBonus = other.temporaryDamageBonus;
		computeDerivedFields();
	}

	public abstract CUnitAttack copy();

	private void computeDerivedFields() {
		this.totalBaseDamage = this.damageBase + this.primaryAttributePermanentDamageBonus + this.permanentDamageBonus;
		this.totalDamageDice = this.damageDice;
		this.minDamageDisplay = this.totalBaseDamage + this.totalDamageDice;
		this.maxDamageDisplay = this.totalBaseDamage + (this.totalDamageDice * this.damageSidesPerDie);
		if (this.minDamageDisplay < 0) {
			this.minDamageDisplay = 0;
		}
		if (this.maxDamageDisplay < 0) {
			this.maxDamageDisplay = 0;
		}
		this.totalTemporaryDamageBonus = this.primaryAttributeTemporaryDamageBonus + this.temporaryDamageBonus;
	}

	public float getAnimationBackswingPoint() {
		return this.animationBackswingPoint;
	}

	public float getAnimationDamagePoint() {
		return this.animationDamagePoint;
	}

	public CAttackType getAttackType() {
		return this.attackType;
	}

	public float getCooldownTime() {
		return this.cooldownTime;
	}

	public int getDamageBase() {
		return this.damageBase;
	}

	public int getDamageDice() {
		return this.damageDice;
	}

	public int getDamageSidesPerDie() {
		return this.damageSidesPerDie;
	}

	public int getDamageUpgradeAmount() {
		return this.damageUpgradeAmount;
	}

	public int getRange() {
		return this.range;
	}

	public float getRangeMotionBuffer() {
		return this.rangeMotionBuffer;
	}

	public boolean isShowUI() {
		return this.showUI;
	}

	public EnumSet<CTargetType> getTargetsAllowed() {
		return this.targetsAllowed;
	}

	public String getWeaponSound() {
		return this.weaponSound;
	}

	public CWeaponType getWeaponType() {
		return this.weaponType;
	}

	public void setAnimationBackswingPoint(final float animationBackswingPoint) {
		this.animationBackswingPoint = animationBackswingPoint;
	}

	public void setAnimationDamagePoint(final float animationDamagePoint) {
		this.animationDamagePoint = animationDamagePoint;
	}

	public void setAttackType(final CAttackType attackType) {
		this.attackType = attackType;
	}

	public void setCooldownTime(final float cooldownTime) {
		this.cooldownTime = cooldownTime;
	}

	public void setDamageBase(final int damageBase) {
		this.damageBase = damageBase;
		computeDerivedFields();
	}

	public void setDamageDice(final int damageDice) {
		this.damageDice = damageDice;
		computeDerivedFields();
	}

	public void setDamageSidesPerDie(final int damageSidesPerDie) {
		this.damageSidesPerDie = damageSidesPerDie;
		computeDerivedFields();
	}

	public void setDamageUpgradeAmount(final int damageUpgradeAmount) {
		this.damageUpgradeAmount = damageUpgradeAmount;
	}

	public void setRange(final int range) {
		this.range = range;
	}

	public void setRangeMotionBuffer(final float rangeMotionBuffer) {
		this.rangeMotionBuffer = rangeMotionBuffer;
	}

	public void setShowUI(final boolean showUI) {
		this.showUI = showUI;
	}

	public void setTargetsAllowed(final EnumSet<CTargetType> targetsAllowed) {
		this.targetsAllowed = targetsAllowed;
	}

	public void setWeaponSound(final String weaponSound) {
		this.weaponSound = weaponSound;
	}

	public void setWeaponType(final CWeaponType weaponType) {
		this.weaponType = weaponType;
	}

	public int getMinDamageDisplay() {
		return this.minDamageDisplay;
	}

	public int getMaxDamageDisplay() {
		return this.maxDamageDisplay;
	}

	public void setPrimaryAttributePermanentDamageBonus(final int primaryAttributeDamageBonus) {
		this.primaryAttributePermanentDamageBonus = primaryAttributeDamageBonus;
		computeDerivedFields();
	}

	public void setPrimaryAttributeTemporaryDamageBonus(final int primaryAttributeDamageBonus) {
		this.primaryAttributeTemporaryDamageBonus = primaryAttributeDamageBonus;
		computeDerivedFields();
	}

	public void setPermanentDamageBonus(final int permanentDamageBonus) {
		this.permanentDamageBonus = permanentDamageBonus;
		computeDerivedFields();
	}

	public void setTemporaryDamageBonus(final int temporaryDamageBonus) {
		this.temporaryDamageBonus = temporaryDamageBonus;
		computeDerivedFields();
	}

	public int getPrimaryAttributePermanentDamageBonus() {
		return this.primaryAttributePermanentDamageBonus;
	}

	public int getPrimaryAttributeTemporaryDamageBonus() {
		return this.primaryAttributeTemporaryDamageBonus;
	}

	public int getPermanentDamageBonus() {
		return this.permanentDamageBonus;
	}

	public int getTemporaryDamageBonus() {
		return this.temporaryDamageBonus;
	}

	public int getTotalDamageDice() {
		return totalDamageDice;
	}

	public int getTotalBaseDamage() {
		return totalBaseDamage;
	}

	public int getTotalTemporaryDamageBonus() {
		return this.totalTemporaryDamageBonus;
	}

	public abstract void launch(CSimulation simulation, CUnit unit, AbilityTarget target, float damage,
			CUnitAttackListener attackListener);

	public int roll(Random seededRandom) {
		int damage = getTotalBaseDamage();
		int dice = getTotalDamageDice();
		int sidesPerDie = getDamageSidesPerDie();
		for (int i = 0; i < dice; i++) {
			damage += seededRandom.nextInt(sidesPerDie) + 1;
		}
		return damage + getTotalTemporaryDamageBonus();
	}
}
