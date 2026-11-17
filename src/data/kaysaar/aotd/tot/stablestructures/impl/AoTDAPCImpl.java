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

import java.util.ArrayList;

public class AoTDAPCImpl extends AoTDLinkedStructureImpl {

    public static final String STRUCTURE_ID = "aotd_apc";
    public static final float BELT_PLACEMENT_MARGIN = 100f;
    private static final float CENTER_TOLERANCE = 2f;

    public static final float VISIBLE_BELT_WIDTH = 300f;

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

    @Override
    public boolean isAvailableToBuild(StarSystemAPI system, FactionAPI faction) {
        return faction != null && faction.isPlayerFaction() && !hasExistingAPC(system) && hasSelectableBelt(system);
    }

    @Override
    public String getUnavailableReason(StarSystemAPI system, FactionAPI faction) {
        if (faction == null || !faction.isPlayerFaction()) return "Requires a player-controlled star system.";
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
        tooltip.addPara("Can be constructed within an asteroid belt or up to %s beyond its outer edge. Limited to %s per star system.",
                5f, Misc.getHighlightColor(), "100 units", "one");
    }
}
