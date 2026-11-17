package data.kaysaar.aotd.tot.ui.starsystems.stuctures.map;

import ashlib.data.plugins.ui.models.resizable.map.MapEntityComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapMainComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapOrbitRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ChooseOrbitConstructionComponent extends MapOrbitRenderer {
    // --- Inputs / context ---
    private MapEntityComponent currentlyCenteredAround;
    private final MapMainComponent mainComponent;


    private  SpriteAPI gateSprite;
    private  float spriteW, spriteH;


    private boolean clampPlanetOrbitRadius = true;
    private Float minOrbitRadiusWorld;
    private Float maxOrbitRadiusWorld;

    /** Optional world-space orbit limits for pickers with a permitted radial band. */
    public void setOrbitRadiusLimits(float min, float max) {
        minOrbitRadiusWorld = Math.max(0f, min);
        maxOrbitRadiusWorld = Math.max(minOrbitRadiusWorld, max);
    }

    public void clearOrbitRadiusLimits() {
        minOrbitRadiusWorld = null;
        maxOrbitRadiusWorld = null;
    }

    public void setClampPlanetOrbitRadius(boolean clampPlanetOrbitRadius) {
        this.clampPlanetOrbitRadius = clampPlanetOrbitRadius;
    }

    // --- Follow lock state ---
    private boolean followLocked = false;
    private Float savedGateAngleRad = null;

    private Float savedRadiusWorld = null;
    String customEntitySpecId;
    public ChooseOrbitConstructionComponent(float radius, MapMainComponent mainComponent,String customEntitySpecId) {
        super(radius);
        this.mainComponent = mainComponent;
        this.color = new Color(255, 255, 255);
        this.customEntitySpecId = customEntitySpecId;

    }

    public void setCustomEntitySpecId(String customEntitySpecId) {
        this.customEntitySpecId = customEntitySpecId;
        this.gateSprite = Global.getSettings().getSprite(
                Global.getSettings().getCustomEntitySpec(customEntitySpecId).getSpriteName()
        );
        this.spriteW = Global.getSettings().getCustomEntitySpec(customEntitySpecId).getSpriteWidth();
        this.spriteH = Global.getSettings().getCustomEntitySpec(customEntitySpecId).getSpriteHeight();
    }
    // ------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------

    public MapEntityComponent getCurrentlyCenteredAround() {
        return currentlyCenteredAround;
    }

    public void setCurrentlyCenteredAround(MapEntityComponent center) {
        this.currentlyCenteredAround = center;
    }

    /** Legacy offset used by older callers; ignored for pointing-down orbits. */
    @Deprecated
    public void setSpriteAngleOffsetDeg(float unused) {
    }

    public boolean isLocked() {
        return followLocked;
    }

    public void toggleLock() {
        if (followLocked) unlockFollow();
        else lockToCurrentFollower();
    }

    public Float getSavedGateAngleRad() {
        return savedGateAngleRad;
    }

    public Float getSavedRadiusWorld() {
        return savedRadiusWorld;
    }

    public Vector2f savedCordsOfGate;

    public Vector2f getSavedCordsOfGate() {
        return savedCordsOfGate;
    }

    /**
     * Freeze the selection (stores angle and radius in world-space).
     */
    public void lockToCurrentFollower() {
        if (currentlyCenteredAround == null) return;

        float cxUI = currentlyCenteredAround.getComponentPanel().getPosition().getCenterX();
        float cyUI = currentlyCenteredAround.getComponentPanel().getPosition().getCenterY();
        float fxUI = mainComponent.getFollower().getComponentPanel().getPosition().getCenterX();
        float fyUI = mainComponent.getFollower().getComponentPanel().getPosition().getCenterY();

        // PositionAPI coordinates are bottom-left based, just like campaign/GL coordinates.
        // Store the world-space angle directly; do not flip the Y direction.
        savedGateAngleRad = (float) Math.atan2(fyUI - cyUI, fxUI - cxUI);

        // Keep radius in WORLD units so it’s robust to zoom changes
        float radiusUI = Misc.getDistance(new Vector2f(cxUI, cyUI), new Vector2f(fxUI, fyUI));
        savedRadiusWorld = radiusUI / Math.max(1e-6f, scale);
        if (minOrbitRadiusWorld != null) savedRadiusWorld = Math.max(savedRadiusWorld, minOrbitRadiusWorld);
        if (maxOrbitRadiusWorld != null) savedRadiusWorld = Math.min(savedRadiusWorld, maxOrbitRadiusWorld);
        savedCordsOfGate = mainComponent.getMapZoom().calculateMouseToWorldCords();
        followLocked = true;
    }

    /**
     * Resume following the follower/mouse.
     */
    public void unlockFollow() {
        followLocked = false;
        savedGateAngleRad = null;
        savedRadiusWorld = null;
        savedCordsOfGate = null;
    }

    // ------------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------------

    @Override
    public void render(float alphaMult) {
        if (currentlyCenteredAround == null) return;

        // --- Orbit center (UI) ---
        float cxUI = currentlyCenteredAround.getComponentPanel().getPosition().getCenterX();
        float cyUI = currentlyCenteredAround.getComponentPanel().getPosition().getCenterY();

        // --- Compute orbit radius in UI ---
        float radiusUI;
        if (followLocked && savedRadiusWorld != null) {
            radiusUI = savedRadiusWorld * scale;
        } else {
            float fxUI = mainComponent.getFollower().getComponentPanel().getPosition().getCenterX();
            float fyUI = mainComponent.getFollower().getComponentPanel().getPosition().getCenterY();
            radiusUI = Misc.getDistance(new Vector2f(cxUI, cyUI), new Vector2f(fxUI, fyUI));
        }

        // Clamp for planets/stars like your original logic
        if (clampPlanetOrbitRadius && currentlyCenteredAround.getToken() instanceof PlanetAPI planet) {
            if (!planet.isStar() && !planet.isBlackHole()) {
                radiusUI = Math.max((currentlyCenteredAround.getToken().getRadius() * 1.5f) * scale, radiusUI);
                radiusUI = Math.min((currentlyCenteredAround.getToken().getRadius() * 3.0f) * scale, radiusUI);
            } else {
                radiusUI = Math.max((currentlyCenteredAround.getToken().getRadius() * 2.0f) * scale, radiusUI);
                radiusUI = Math.min(25000f * scale, radiusUI);
            }
        }
        if (minOrbitRadiusWorld != null) radiusUI = Math.max(radiusUI, minOrbitRadiusWorld * scale);
        if (maxOrbitRadiusWorld != null) radiusUI = Math.min(radiusUI, maxOrbitRadiusWorld * scale);
        if (radiusUI <= 0f) return;

        // --- Orbit line (UI) ---
        float circumference = (float) (2f * Math.PI * radiusUI);
        int segments = Math.max(32, Math.min(512, (int) (circumference / 4f)));

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            float lw = Math.max(1f, lineWidth * scale);
            GL11.glLineWidth(lw);

            float a = (color.getAlpha() / 255f) * alphaMult;
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, a);

            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < segments; i++) {
                float t = (float) (i * (2 * Math.PI / segments));
                float x = cxUI + (float) Math.cos(t) * radiusUI;
                float y = cyUI + (float) Math.sin(t) * radiusUI;
                GL11.glVertex2f(x, y);
            }
            GL11.glEnd();
        } finally {
            GL11.glPopAttrib();
        }

        // --- Gate placement (UI) ---
        // PositionAPI's center coordinates and SpriteAPI.renderAtCenter both use
        // bottom-left origins. Use the same world-space angle for selection and preview.
        float angleRadToUse;
        if (followLocked && savedGateAngleRad != null) {
            angleRadToUse = savedGateAngleRad;
        } else {
            float fxUI = mainComponent.getFollower().getComponentPanel().getPosition().getCenterX();
            float fyUI = mainComponent.getFollower().getComponentPanel().getPosition().getCenterY();
            angleRadToUse = (float) Math.atan2(fyUI - cyUI, fxUI - cxUI);
        }

        float cos = (float) Math.cos(angleRadToUse);
        float sin = (float) Math.sin(angleRadToUse);
        float px = cxUI + cos * radiusUI;
        float py = cyUI + sin * radiusUI;

        // Vanilla campaign entity rendering uses sprite.setAngle(facing - 90f).
        // A pointing-down orbit faces radially outward (bottom toward the focus).
        // Thus, the preview's SpriteAPI angle is orbitAngle - 90 degrees.
        float faceCenterDeg = (float) Math.toDegrees(angleRadToUse) - 90f;
        if (isLocked()) {
            savedRadiusWorld = radiusUI / scale;
            gateSprite.setSize(spriteW * scale, spriteH * scale);
            gateSprite.setColor(Color.cyan);
            gateSprite.setAlphaMult(0.9f * alphaMult);
            gateSprite.setAngle(faceCenterDeg);
            gateSprite.renderAtCenter(px, py);

        } else {
            MapEntityComponent.drawFilledCircle(new Vector2f(px, py), 55 * scale, new Color(118, 255, 144), alphaMult);
        }

    }
}

