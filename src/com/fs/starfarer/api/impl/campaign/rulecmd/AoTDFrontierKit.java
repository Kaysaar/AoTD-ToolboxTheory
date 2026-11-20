package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.abilities.DeployFrontierKit;
import data.kaysaar.aotd.tot.entities.FrontierDeviceExpiry;

import java.util.List;
import java.util.Map;

public class AoTDFrontierKit extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) return false;
        String type = params.get(0).getString(memoryMap);
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        SectorEntityToken target = dialog.getInteractionTarget();
        if (target != fleet || !DeployFrontierKit.canDeploy(fleet)) {
            dialog.getTextPanel().addPara("Deployment requires a Frontier Development Kit and a star system without colonies.");
            return false;
        }
        if (!"comm_relay".equals(type) && !"sensor_array".equals(type) && !"nav_buoy".equals(type)) return false;
        StarSystemAPI system = fleet.getStarSystem();
        SectorEntityToken focus = system.getCenter();
        SectorEntityToken device = system.addCustomEntity(null, null, "aotd_makeshift_" + type, Factions.PLAYER);
        float radius = Math.max(Misc.getDistance(focus, fleet), focus.getRadius() + device.getRadius() + 30f);
        device.setCircularOrbit(focus, Misc.getAngleInDegrees(focus.getLocation(), fleet.getLocation()), radius, Math.max(1f, radius / 15f));
        Global.getSector().addScript(new FrontierDeviceExpiry(device));
        fleet.getCargo().removeCommodity(DeployFrontierKit.KIT, 1);
        AddRemoveCommodity.addCommodityLossText(DeployFrontierKit.KIT, 1, dialog.getTextPanel());
        dialog.getTextPanel().addPara(device.getName() + " deployed. It will remain in orbit for 210 days.");
        return true;
    }
}
