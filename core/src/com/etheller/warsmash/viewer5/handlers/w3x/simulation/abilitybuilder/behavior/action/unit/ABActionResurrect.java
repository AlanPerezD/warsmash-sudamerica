package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.action.unit;

import java.util.Map;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.unitcallbacks.ABUnitCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABAction;

public class ABActionResurrect implements ABAction {

	private ABUnitCallback target;

	@Override
	public void runAction(CSimulation game, CUnit caster, Map<String, Object> localStore) {
		CUnit targetUnit = target.callback(game, caster, localStore);
		if (targetUnit.isDead()) {
			targetUnit.resurrect(game);
		}
	}

}
