package data.kaysaar.aotd.tot.stablestructures.impl;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.ui.starsystems.stuctures.map.ChooseOrbitForBuildingMap;
import data.kaysaar.aotd.tot.ui.starsystems.stuctures.map.StructureLocationPicker;

public class AoTDScavGuild extends AoTDStableStructureImpl {
    public static int getAmountOfDebrisFieldFromSystem(StarSystemAPI systemAPI){
        int am = 0 ;
        for (CampaignTerrainAPI campaignTerrainAPI : systemAPI.getTerrainCopy()) {
            if(campaignTerrainAPI.getPlugin() instanceof DebrisFieldTerrainPlugin){
                am++;
            }
        }
        return am;
    }

    @Override
    public StructureLocationPicker createLocationPicker(float width, float height, StarSystemAPI systemAPI) {
        return new ChooseOrbitForBuildingMap(width,height,systemAPI,id);
    }

    @Override
    public boolean isAvailableToBuild(StarSystemAPI systemAPI, FactionAPI factionAttemptingToBuildIt) {
        return getAmountOfDebrisFieldFromSystem(systemAPI)>0;
    }

    @Override
    public String getUnavailableReason(StarSystemAPI systemAPI, FactionAPI factionAttemptingToBuildIt) {
        if(getAmountOfDebrisFieldFromSystem(systemAPI)<=0){
            return "There is no Debris field to be scavenged.";
        }
        return super.getUnavailableReason(systemAPI, factionAttemptingToBuildIt);
    }

    @Override
    public void createTooltipForEffectSection(TooltipMakerAPI tooltip, AoTDStableStructureTooltipMode mode) {
        super.createTooltipForEffectSection(tooltip, mode);
        tooltip.addPara("For each debris field planet with highest accessibility gets random amount of production each month, as long as Scavenger Enclave is operational.",5f);

    }
}
