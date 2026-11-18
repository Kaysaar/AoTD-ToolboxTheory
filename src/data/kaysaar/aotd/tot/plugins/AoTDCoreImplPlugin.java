package data.kaysaar.aotd.tot.plugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.CoreCampaignPluginImpl;
import com.fs.starfarer.api.impl.campaign.PlanetInteractionDialogPluginImpl;

public class AoTDCoreImplPlugin extends CoreCampaignPluginImpl {
    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        PluginPick<InteractionDialogPlugin> pluginPluginPick =  super.pickInteractionDialogPlugin(interactionTarget);
        if(pluginPluginPick==null||pluginPluginPick.plugin==null)return null;
        if (pluginPluginPick.plugin instanceof PlanetInteractionDialogPluginImpl ) {
            return new PluginPick<InteractionDialogPlugin>(new AoTDPlanetInteractionDialogPluginImpl(), PickPriority.MOD_GENERAL);
        }
        return pluginPluginPick;
    }

    @Override
    public String getId() {
        return "AoTDCoreImplPlugin";
    }
}
