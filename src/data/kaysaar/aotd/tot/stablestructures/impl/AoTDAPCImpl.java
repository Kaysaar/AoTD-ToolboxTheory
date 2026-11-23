package data.kaysaar.aotd.tot.stablestructures.impl;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.stablestructures.AoTDLinkedStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpecManager;
import data.kaysaar.aotd.tot.ui.starsystems.stuctures.map.APCLocationPicker;
import data.kaysaar.aotd.tot.ui.starsystems.stuctures.map.StructureLocationPicker;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import kaysaar.bmo.buildingmenu.BuildingMenuMisc;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDCommodityOnMarket;
import data.kaysaar.aotd.tot.scripts.economy.AoTDIndustryData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AoTDAPCImpl extends AoTDLinkedStructureImpl {

    public static final String STRUCTURE_ID = "aotd_apc";
    public static final float BELT_PLACEMENT_MARGIN = 100f;
    private static final float CENTER_TOLERANCE = 2f;

    public static final float VISIBLE_BELT_WIDTH = 300f;


    private static final String ACTIVE_APC = "$aotd_active_apc";
    private Set<Industry> operating;
    private Set<Industry> heavyIndustries;
    private List<MarketAPI> affected;
    private MarketAPI supplyMarket;

    public static AoTDAPCImpl getAt(Industry industry) {
        if (industry == null || industry.getMarket() == null) return null;
        return (AoTDAPCImpl) industry.getMarket().getMemoryWithoutUpdate().get(ACTIVE_APC);
    }

    public boolean isIntegrated(Industry industry) {
        return operating != null && operating.contains(industry);
    }

    @Override
    public void apply() {
        if (currentlyAssignedToken == null) return;
        affected = new ArrayList<>(Global.getSector().getEconomy().getMarkets(currentlyAssignedToken.getStarSystem()));
        operating = new HashSet<>();
        heavyIndustries = new HashSet<>();
        for (MarketAPI market : affected) {
            market.getMemoryWithoutUpdate().set(ACTIVE_APC, this);
            if (!market.isPlayerOwned() || !isPlayerOwned()) continue;
            boolean participating = false;
            for (Industry industry : market.getIndustries()) {
                if (inIndustryTree(industry, Industries.REFINING) && industry.isFunctional()
                        && !AoTDIndustryData.getInstance(market).isPending(industry.getId())) {
                    operating.add(industry);
                    participating = true;
                }
            }
            if (participating) {
                for (Industry industry : market.getIndustries()) {
                    if (inIndustryTree(industry, Industries.HEAVYINDUSTRY)) heavyIndustries.add(industry);
                }
                market.getAccessibilityMod().modifyFlat(getModId(), -0.10f, "Integrated with APC");
            }
        }
    }

    @Override
    public void unapply() {
        if (supplyMarket != null) {
            if (supplyMarket.getCommodityData(Commodities.VOLATILES) instanceof AoTDCommodityOnMarket volatiles) {
                volatiles.getSupplyDemandData().getAdditionalDemand().unmodify(getModId());
            }
            supplyMarket = null;
        }
        if (affected == null) return;
        for (MarketAPI market : affected) {
            if (market.getMemoryWithoutUpdate().get(ACTIVE_APC) == this) market.getMemoryWithoutUpdate().unset(ACTIVE_APC);
            market.getAccessibilityMod().unmodify(getModId());
        }
        affected = null;
    }

    private static boolean inIndustryTree(Industry industry, String root) {
        return root.equals(industry.getId()) || BuildingMenuMisc.getIndustryTree(root).contains(industry.getSpec());
    }

    public static float demandMultiplier(Industry industry, String commodity) {
        AoTDAPCImpl apc = getAt(industry);
        if (apc == null) return 1f;
        float change = Commodities.ORE.equals(commodity) || Commodities.RARE_ORE.equals(commodity) ? -0.10f : 0f;
        int count = Math.min(6, apc.operating.size());
        if (apc.operating.contains(industry)) change += 0.05f * count;
        if (apc.heavyIndustries.contains(industry)) {
            change -= 0.025f * count;
        }
        return 1f + change;
    }

    public static float outputSizeBonus(Industry industry) {
        AoTDAPCImpl apc = getAt(industry);
        if (apc == null || !apc.operating.contains(industry)) return 0f;
        return 0.5f * Math.min(6, apc.operating.size());
    }

    public static boolean isWithinAsteroidBelt(StarSystemAPI system, SectorEntityToken focus, Vector2f position) {
        if (system == null || focus == null || focus.isExpired() || focus.getStarSystem() != system
                || position == null || !Float.isFinite(position.x) || !Float.isFinite(position.y)) return false;

        float radius = Vector2f.sub(position, focus.getLocation(), null).length();
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            if (terrain == null || terrain.isExpired()
                    || !(terrain.getPlugin() instanceof AsteroidBeltTerrainPlugin belt)
                    || belt.params == null || !Float.isFinite(belt.params.middleRadius)
                    || belt.params.middleRadius <= 0f) continue;

            if (focus instanceof CampaignTerrainAPI && terrain != focus) continue;
            if (Misc.getDistance(terrain.getLocation(), focus.getLocation()) > CENTER_TOLERANCE) continue;

            float inner = Math.max(0f, belt.params.middleRadius - VISIBLE_BELT_WIDTH * 0.5f);
            float outer = belt.params.middleRadius + VISIBLE_BELT_WIDTH * 0.5f + BELT_PLACEMENT_MARGIN;
            if (radius >= inner && radius <= outer) return true;
        }
        return false;
    }

    public boolean hasExistingAPC(StarSystemAPI system) {
        if (system == null) return false;
        for (SectorEntityToken token : system.getAllEntities()) {
            if (token == null || token.isExpired() || token == currentlyAssignedToken) continue;
            if (STRUCTURE_ID.equals(token.getCustomEntityType())) return true;
            Object existing = token.getMemoryWithoutUpdate().get(AoTDStableStructureSpecManager.memKeyInEntities);
            if (existing instanceof AoTDStableStructureImpl impl && STRUCTURE_ID.equals(impl.id)) return true;
        }
        return false;
    }

    private boolean hasSelectableBelt(StarSystemAPI system) {
        if (system == null) return false;
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            if (terrain == null || terrain.isExpired()
                    || !(terrain.getPlugin() instanceof AsteroidBeltTerrainPlugin belt)
                    || belt.params == null || !Float.isFinite(belt.params.middleRadius)
                    || belt.params.middleRadius <= 0f) continue;
            return true;
        }
        return false;
    }

    private boolean hasRequiredResearch(FactionAPI faction) {
        return !Global.getSettings().getModManager().isModEnabled("aotd_vok")
                || AoTDMainResearchManager.getInstance().isAvailableForFaction("aotd_tech_advanced_mining_equipment", faction);
    }

    @Override
    public boolean isAvailableToBuild(StarSystemAPI system, FactionAPI faction) {
        return faction != null && faction.isPlayerFaction() && hasRequiredResearch(faction) && !hasExistingAPC(system) && hasSelectableBelt(system);
    }

    @Override
    public boolean showWhenUnavailable(StarSystemAPI system, FactionAPI faction) {
        return hasRequiredResearch(faction) && super.showWhenUnavailable(system, faction);
    }

    @Override
    public String getUnavailableReason(StarSystemAPI system, FactionAPI faction) {
        if (faction == null || !faction.isPlayerFaction()) return "Requires a player-controlled star system.";
        if (!hasRequiredResearch(faction)) return "Requires Advanced Mining Equipment research.";
        if (hasExistingAPC(system)) return "Only one Asteroid Processing Center may be constructed in each star system.";
        if (!hasSelectableBelt(system)) return "Requires at least one asteroid belt in this system.";
        return super.getUnavailableReason(system, faction);
    }

    @Override
    public boolean canPlaceAt(StarSystemAPI system, SectorEntityToken focus, Vector2f position) {
        return !hasExistingAPC(system) && isWithinAsteroidBelt(system, focus, position);
    }

    @Override
    public StructureLocationPicker createLocationPicker(float width, float height, StarSystemAPI system) {
        return new APCLocationPicker(width, height, system);
    }

    @Override
    public void setConstructionFocus(SectorEntityToken focus) {
        if (focus == null || currentlyAssignedToken == null) return;

        float radius = currentlyAssignedToken.getCircularOrbitRadius();
        float closest = Float.MAX_VALUE;
        float orbitDays = 0f;
        for (CampaignTerrainAPI terrain : focus.getStarSystem().getTerrainCopy()) {
            if (terrain.isExpired() || !(terrain.getPlugin() instanceof AsteroidBeltTerrainPlugin belt)
                    || belt.params == null || Misc.getDistance(terrain.getLocation(), focus.getLocation()) > CENTER_TOLERANCE) continue;

            float distance = Math.abs(belt.params.middleRadius - radius);
            if (distance < closest) {
                closest = distance;
                orbitDays = (belt.params.minOrbitDays + belt.params.maxOrbitDays) * 0.5f;
            }
        }
        if (Float.isFinite(orbitDays) && orbitDays > 0f) {
            currentlyAssignedToken.setCircularOrbitPointingDown(focus, currentlyAssignedToken.getCircularOrbitAngle(), radius, orbitDays);
        }
    }

    @Override
    public ArrayList<LinkedEntityData> getLinkedEntityData() {
        return new ArrayList<>();
    }

    @Override
    public void placeLinkedEntity(SectorEntityToken token, LinkedEntityData data) {

    }

    @Override
    public void createTooltipForEffectSection(TooltipMakerAPI tooltip, AoTDStableStructureTooltipMode mode) {
        super.createTooltipForEffectSection(tooltip, mode);
        tooltip.addPara("The APC processes asteroids to reduce system-wide demand for ore and transplutonic ore by %s.",
                5f, Misc.getHighlightColor(), "10%");
        tooltip.addPara("Operating refineries and their upgrades on player colonies in this system connect automatically. Each operating refinery increases input demand by %s and effective production size by %s for every connected refinery, up to %s and %s sizes.",
                5f, Misc.getHighlightColor(), "5%", "0.5", "30%", "3");
        tooltip.addPara("On colonies with a connected, operating refinery, Heavy Industry and its upgrades use %s fewer inputs per refinery in the network, up to %s. Each participating colony loses %s accessibility.",
                5f, Misc.getHighlightColor(), "2.5%", "15%", "10 percentage points");
        if (affected != null && operating != null) {
            tooltip.addPara("Operating refineries: %s", 5f, Misc.getHighlightColor(), String.valueOf(operating.size()));
        }
    }
}
