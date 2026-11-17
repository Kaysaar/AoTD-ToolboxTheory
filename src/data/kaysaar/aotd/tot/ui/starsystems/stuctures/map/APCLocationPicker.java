package data.kaysaar.aotd.tot.ui.starsystems.stuctures.map;

import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapEntityComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.stablestructures.impl.AoTDAPCImpl;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class APCLocationPicker extends ChooseOrbitForBuildingMap {
    private final StarSystemAPI system;
    private final ArrayList<BeltPlacementBand> belts = new ArrayList<>();
    private BeltPlacementBand selectedBelt;
    private boolean selectionChanged = true;

    public APCLocationPicker(float width, float height, StarSystemAPI system) {
        super(width, height, system, AoTDAPCImpl.STRUCTURE_ID);
        this.system = system;
        
        getOrbitComponent().setClampPlanetOrbitRadius(false);
        addBeltAreas();

        getMapZoom().resizableComponents.remove(getOrbitComponent());
        getMapZoom().getPluginPanel().removeComponent(getOrbitComponent().getComponentPanel());
        getMapZoom().addComponent(getOrbitComponent(), WORLD_X, WORLD_X);
    }

    @Override
    protected void afterAddPlanets() {
    }

    private void addBeltAreas() {
        if (system == null) return;
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            if (terrain == null || terrain.isExpired()
                    || !(terrain.getPlugin() instanceof AsteroidBeltTerrainPlugin belt)
                    || belt.params == null || !Float.isFinite(belt.params.middleRadius)
                    || belt.params.middleRadius <= 0f) continue;

            float inner = Math.max(0f, belt.params.middleRadius - AoTDAPCImpl.VISIBLE_BELT_WIDTH * 0.5f);
            float outer = belt.params.middleRadius + AoTDAPCImpl.VISIBLE_BELT_WIDTH * 0.5f
                    + AoTDAPCImpl.BELT_PLACEMENT_MARGIN;
            if (outer <= inner) continue;

            SectorEntityToken orbitFocus = findOrbitFocus(terrain);
            if (orbitFocus == null) continue;

            MapEntityComponent center = new MapEntityComponent(terrain);
            center.componentPanel = Global.getSettings().createCustom(1f, 1f, center);
            Vector2f mapPoint = translateCoordinatesToUI(terrain.getLocation());
            getMapZoom().addComponent(center, mapPoint.x, mapPoint.y);
            center.scale = getMapZoom().currScale;

            BeltPlacementBand area = new BeltPlacementBand(terrain, orbitFocus, center, inner, outer,
                    belt.params.middleRadius, getMapZoom().getPluginPanel());
            area.scale = getMapZoom().currScale;
            getMapZoom().addComponent(area, WORLD_X, WORLD_X);
            belts.add(area);
        }
    }

    private SectorEntityToken findOrbitFocus(CampaignTerrainAPI terrain) {
        SectorEntityToken focus = terrain.getOrbitFocus();
        if (focus != null && !focus.isExpired() && focus.getStarSystem() == system
                && !(focus instanceof CampaignTerrainAPI)
                && Misc.getDistance(terrain.getLocation(), focus.getLocation()) <= 2f) {
            return focus;
        }
        for (PlanetAPI planet : system.getPlanets()) {
            if (!planet.isExpired()
                    && Misc.getDistance(terrain.getLocation(), planet.getLocation()) <= 2f) {
                return planet;
            }
        }
        return null;
    }

    @Override
    public void onClick(Vector2f inUIWorldCoordinates, InputEventAPI event) {
        if (!event.isLMBUpEvent() || system == null) return;
        Vector2f position = translateCoordinatesFromUIToWorld(inUIWorldCoordinates);

        BeltPlacementBand best = null;
        float bestDistance = Float.MAX_VALUE;
        for (BeltPlacementBand candidate : belts) {
            if (!candidate.containsForSelection(position)) continue;
            float distance = Math.abs(Misc.getDistance(position,
                    candidate.terrain.getLocation()) - candidate.middleRadius);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }

        if (best != null && best != selectedBelt) {
            selectedBelt = best;
            getOrbitComponent().unlockFollow();
            getOrbitComponent().setCurrentlyCenteredAround(best.center);
            getOrbitComponent().setOrbitRadiusLimits(best.innerRadius, best.outerRadius);
        } else if (selectedBelt != null) {
            
            getOrbitComponent().toggleLock();
        } else {
            return;
        }

        selectionChanged = true;
        Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
    }

    private Vector2f getLockedPosition() {
        if (selectedBelt == null || !getOrbitComponent().isLocked()
                || getOrbitComponent().getSavedRadiusWorld() == null
                || getOrbitComponent().getSavedGateAngleRad() == null) return null;
        float radius = getOrbitComponent().getSavedRadiusWorld();
        float angle = getOrbitComponent().getSavedGateAngleRad();
        Vector2f center = selectedBelt.orbitFocus.getLocation();
        return new Vector2f(center.x + (float) Math.cos(angle) * radius,
                center.y + (float) Math.sin(angle) * radius);
    }

    @Override
    public boolean hasSelection() {
        Vector2f position = getLockedPosition();
        return position != null && !selectedBelt.orbitFocus.isExpired()
                && selectedBelt.contains(position)
                && AoTDAPCImpl.isWithinAsteroidBelt(system,
                        selectedBelt.orbitFocus, position);
    }

    @Override
    public SectorEntityToken getSelectedEntity() {
        return hasSelection() ? selectedBelt.orbitFocus : null;
    }

    @Override
    public Vector2f getSelectedLocation() {
        return hasSelection() ? getLockedPosition() : null;
    }

    @Override
    public boolean consumeSelectionTextUpdate() {
        boolean changed = selectionChanged || super.consumeSelectionTextUpdate();
        selectionChanged = false;
        return changed;
    }

    @Override
    public void createSelectionText(TooltipMakerAPI tooltip) {
        if (belts.isEmpty()) {
            tooltip.addPara("No asteroid belts are available in this star system.",
                    Misc.getNegativeHighlightColor(), 0f);
        } else if (hasSelection()) {
            tooltip.addPara("%s: orbit position locked. Click again to adjust, select another green belt, or press %s to clear.",
                    0f, Color.ORANGE, selectedBelt.terrain.getName(), "D");
        } else if (selectedBelt != null) {
            tooltip.addPara("%s selected. Adjust the orbit within the green band, then click to lock it. Press %s to clear.",
                    0f, Color.ORANGE, selectedBelt.terrain.getName(), "D");
        } else {
            tooltip.addPara("Click an asteroid belt to select it, then adjust the orbit within the green band and lock it. Press %s to clear.",
                    0f, Color.ORANGE, "D");
        }
    }

    private void clearSelection() {
        selectedBelt = null;
        getOrbitComponent().unlockFollow();
        getOrbitComponent().setCurrentlyCenteredAround(null);
        getOrbitComponent().clearOrbitRadiusLimits();
        selectionChanged = true;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (!event.isConsumed() && event.isKeyUpEvent()
                    && event.getEventValue() == Keyboard.KEY_D) {
                clearSelection();
                event.consume();
            }
        }
        super.processInput(events);
    }

    private final class BeltPlacementBand extends ResizableComponent {
        private final CampaignTerrainAPI terrain;
        private final SectorEntityToken orbitFocus;
        private final MapEntityComponent center;
        private final float innerRadius;
        private final float outerRadius;
        private final float middleRadius;
        private final CustomPanelAPI viewport;

        private BeltPlacementBand(CampaignTerrainAPI terrain, SectorEntityToken orbitFocus,
                                  MapEntityComponent center, float innerRadius, float outerRadius,
                                  float middleRadius, CustomPanelAPI viewport) {
            this.terrain = terrain;
            this.orbitFocus = orbitFocus;
            this.center = center;
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
            this.middleRadius = middleRadius;
            this.viewport = viewport;
            this.componentPanel = Global.getSettings().createCustom(1f, 1f, this);
        }

        private boolean contains(Vector2f point) {
            if (terrain.isExpired()) return false;
            float distance = Misc.getDistance(point, terrain.getLocation());
            return distance >= innerRadius && distance <= outerRadius;
        }

        private boolean containsForSelection(Vector2f point) {
            if (terrain.isExpired()) return false;
            float distance = Misc.getDistance(point, terrain.getLocation());
            return distance >= innerRadius
                    && distance <= outerRadius;
        }

        @Override
        public void render(float alphaMult) {
            if (terrain.isExpired()) return;
            float cx = center.getComponentPanel().getPosition().getCenterX();
            float cy = center.getComponentPanel().getPosition().getCenterY();
            float inner = innerRadius * scale;
            float outer = outerRadius * scale;
            if (outer <= inner) return;
            int segments = Math.max(72, Math.min(360, (int) (2.0 * Math.PI * outer / 5f)));
            boolean selected = selectedBelt == this;

            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                    | GL11.GL_LINE_BIT | GL11.GL_SCISSOR_BIT);
            try {
                PositionAPI area = viewport.getPosition();
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(Math.round(area.getX()), Math.round(area.getY()),
                        Math.max(1, Math.round(area.getWidth())),
                        Math.max(1, Math.round(area.getHeight())));
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                GL11.glColor4f(0.24f, 0.91f, 0.43f,
                        (selected ? 0.32f : 0.18f) * alphaMult);
                GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                for (int i = 0; i <= segments; i++) {
                    double a = i * 2.0 * Math.PI / segments;
                    float cos = (float) Math.cos(a);
                    float sin = (float) Math.sin(a);
                    GL11.glVertex2f(cx + cos * inner, cy + sin * inner);
                    GL11.glVertex2f(cx + cos * outer, cy + sin * outer);
                }
                GL11.glEnd();

                GL11.glLineWidth(selected ? 2f : 1.2f);
                GL11.glColor4f(0.30f, 1f, 0.55f,
                        (selected ? 0.95f : 0.50f) * alphaMult);
                for (float radius : new float[] { inner, outer }) {
                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    for (int i = 0; i < segments; i++) {
                        double a = i * 2.0 * Math.PI / segments;
                        GL11.glVertex2f(cx + (float) Math.cos(a) * radius,
                                cy + (float) Math.sin(a) * radius);
                    }
                    GL11.glEnd();
                }
            } finally {
                GL11.glPopAttrib();
            }
        }
    }
}
