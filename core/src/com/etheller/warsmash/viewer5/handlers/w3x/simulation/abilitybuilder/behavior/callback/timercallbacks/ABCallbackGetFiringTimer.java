package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.timercallbacks;

import java.util.Map;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

public class ABCallbackGetFiringTimer extends ABTimerCallback {

	@Override
	public CTimer callback(CSimulation game, CUnit caster, Map<String, Object> localStore) {
		return (CTimer) localStore.get(ABLocalStoreKeys.FIRINGTIMER);
	}

}
