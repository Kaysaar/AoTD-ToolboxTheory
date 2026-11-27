package data.kaysaar.aotd.tot.ui.starsystems.stuctures.map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpec;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpecManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Random;

public class StructurePlacementHologram {
    private static final int SLICE_COUNT = 16;
    private final SpriteAPI sprite;
    private final Color hologramColor;
    private final boolean shiftEnabled;
    final float spriteWidth, spriteHeight;
    private final Random random = new Random();
    private final float[] sliceShifts = new float[SLICE_COUNT];
    private final float[] timeUntilShift = new float[SLICE_COUNT];
    private final float[] firstRow = new float[4];
    private final float[] secondRow = new float[4];

    public StructurePlacementHologram(String customEntitySpecId) {
        CustomEntitySpecAPI entitySpec = Global.getSettings().getCustomEntitySpec(customEntitySpecId);
        sprite = Global.getSettings().getSprite(entitySpec.getSpriteName());
        spriteWidth = entitySpec.getSpriteWidth();
        spriteHeight = entitySpec.getSpriteHeight();
        AoTDStableStructureSpec spec = AoTDStableStructureSpecManager.getSpec(customEntitySpecId);
        AoTDStableStructureImpl impl = spec == null ? null : spec.getImplClassInstance();
        hologramColor = impl == null ? null : impl.getHologramColor();
        shiftEnabled = impl != null && impl.hasHologramRipple();
        if (shiftEnabled) {
            for (int i = 0; i < SLICE_COUNT; i++) timeUntilShift[i] = random.nextFloat() * 0.35f;
        }
    }

    public void advance(float amount) {
        if (!shiftEnabled || amount <= 0f) return;
        for (int i = 0; i < SLICE_COUNT; i++) {
            timeUntilShift[i] -= amount;
            if (timeUntilShift[i] > 0f) continue;
            if (sliceShifts[i] == 0f) {
                float distance = 0.55f + random.nextFloat() * 0.75f;
                sliceShifts[i] = random.nextBoolean() ? distance : -distance;
                timeUntilShift[i] = 0.11f + random.nextFloat() * 0.16f;
            } else {
                sliceShifts[i] = 0f;
                timeUntilShift[i] = 0.12f + random.nextFloat() * 0.24f;
            }
        }
    }

    public void render(float x, float y, float scale, float angle, float alphaMult) {
        float width = spriteWidth * scale;
        float height = spriteHeight * scale;
        if (width <= 0f || height <= 0f || alphaMult <= 0f) return;
        Color tint = hologramColor == null ? Color.cyan : hologramColor;
        if (hologramColor == null && !shiftEnabled) {
            sprite.setSize(width, height);
            sprite.setColor(tint);
            sprite.setAlphaMult(0.9f * alphaMult);
            sprite.setAngle(angle);
            sprite.renderAtCenter(x, y);
            return;
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT
                | GL11.GL_CURRENT_BIT | GL11.GL_TRANSFORM_BIT);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0f);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            sprite.bindTexture();
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE,
                    hologramColor == null ? GL11.GL_MODULATE : GL11.GL_ADD);
            float opacity = hologramColor == null ? 0.9f : 0.76f;
            GL11.glColor4f(tint.getRed() / 255f, tint.getGreen() / 255f, tint.getBlue() / 255f,
                    alphaMult * opacity * tint.getAlpha() / 255f);
            if (shiftEnabled) {
                drawSlices(width, height, angle);
            } else {
                GL11.glRotatef(angle, 0f, 0f, 1f);
                drawHologram(width, height);
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void drawSlices(float width, float height, float angle) {
        double radians = Math.toRadians(angle);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float riseU = width * sin;
        float riseV = height * cos;
        float widthX = width * cos;
        float heightX = -height * sin;
        float span = Math.abs(riseU) + Math.abs(riseV);
        float amplitude = Math.max(2f, Math.min(14f, width * 0.16f));
        GL11.glTranslatef(0f, -span * 0.5f, 0f);
        GL11.glDisable(GL11.GL_CULL_FACE);

        float[] lower = firstRow;
        float[] upper = secondRow;
        float bottom = 0f;
        findRowEnds(lower, bottom, riseU, riseV);
        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < SLICE_COUNT; i++) {
            int shift = Math.round(sliceShifts[i] * amplitude);
            float sliceTop = span * ((i + 1f) / SLICE_COUNT);
            while (bottom < sliceTop) {
                float top = sliceTop;
                if (bottom < Math.abs(riseU)) top = Math.min(top, Math.abs(riseU));
                if (bottom < Math.abs(riseV)) top = Math.min(top, Math.abs(riseV));
                findRowEnds(upper, top, riseU, riseV);
                drawRowEnd(lower, 0, widthX, heightX, bottom, shift);
                drawRowEnd(lower, 2, widthX, heightX, bottom, shift);
                drawRowEnd(upper, 2, widthX, heightX, top, shift);
                drawRowEnd(upper, 0, widthX, heightX, top, shift);
                float[] swap = lower;
                lower = upper;
                upper = swap;
                bottom = top;
            }
        }
        GL11.glEnd();
    }

    private void findRowEnds(float[] uv, float y, float riseU, float riseV) {
        float u = Math.abs(riseU);
        float v = Math.abs(riseV);
        uv[0] = u == 0f ? 1f : Math.min(1f, y / u);
        uv[1] = v == 0f ? 0f : Math.min(1f, Math.max(0f, (y - u) / v));
        uv[2] = u == 0f ? 0f : Math.min(1f, Math.max(0f, (y - v) / u));
        uv[3] = v == 0f ? 1f : Math.min(1f, y / v);
        if (riseU < 0f) {
            uv[0] = 1f - uv[0];
            uv[2] = 1f - uv[2];
        }
        if (riseV < 0f) {
            uv[1] = 1f - uv[1];
            uv[3] = 1f - uv[3];
        }
    }

    private void drawRowEnd(float[] uv, int end, float widthX, float heightX, float y, float shift) {
        float u = uv[end];
        float v = uv[end + 1];
        GL11.glTexCoord2f(u * sprite.getTextureWidth(), v * sprite.getTextureHeight());
        GL11.glVertex2f((u - 0.5f) * widthX + (v - 0.5f) * heightX + shift, y);
    }

    private void drawHologram(float width, float height) {
        float right = width * 0.5f;
        float top = height * 0.5f;
        float u = sprite.getTextureWidth();
        float v = sprite.getTextureHeight();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(-right, -top);
        GL11.glTexCoord2f(u, 0f);
        GL11.glVertex2f(right, -top);
        GL11.glTexCoord2f(u, v);
        GL11.glVertex2f(right, top);
        GL11.glTexCoord2f(0f, v);
        GL11.glVertex2f(-right, top);
        GL11.glEnd();
    }
}
