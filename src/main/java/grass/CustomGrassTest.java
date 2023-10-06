package grass;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeVersion;
import com.jme3.texture.Texture;

import java.awt.*;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.prefs.BackingStoreException;

import jme3tools.optimize.GeometryBatchFactory;

public class CustomGrassTest extends SimpleApplication {
    private static SplittableRandom random = new SplittableRandom();

    public static float getRandomNumberInRange(float min, float max) {
        return (float) random.doubles(min, max).findAny().getAsDouble();
    }

    public static void main(String[] args) {
        CustomGrassTest app = new CustomGrassTest();
        app.start();
    }

    private float elapsedTime = 0;

    private Material grassShader;

    private Geometry grassGeom;

    private Node allGrass = new Node("all grass");

    private Vector2f windDirection = new Vector2f();
    private float windStrength;

    private Geometry ground;

    public static void applyDefaultSettings(AppSettings settings) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        settings.setFullscreen(device.isFullScreenSupported());
        DisplayMode[] modes = device.getDisplayModes();
        DisplayMode mode = modes[modes.length-1];
        settings.setDepthBits(mode.getBitDepth());
        settings.setResolution(mode.getWidth(),mode.getHeight());
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        settings.setFrequency(mode.getRefreshRate());
        settings.setGammaCorrection(false);
        settings.setStencilBits(8);
        settings.setTitle(JmeVersion.FULL_NAME);
    }

    public static void saveSettings(AppSettings settings) {
        try {
            settings.save(JmeVersion.FULL_NAME);
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }

    public static AppSettings loadSettings() {
        AppSettings settings = new AppSettings(false);

        try {
            settings.load(JmeVersion.FULL_NAME);
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }

        if(settings.size() == 0) {
            settings = new AppSettings(true);
            applyDefaultSettings(settings);
        }

        //Native launcher BUG workaround - otherwise it will drop to some weird resolution
        settings.setSettingsDialogImage(null);
        if (!JmeSystem.showSettingsDialog(settings, true)) {
            return null;
        }

        return settings;
    }

    @Override
    public void start() {
        if (settings == null) {
            AppSettings loadedSettings = loadSettings();
            if(loadedSettings == null)
                return;
            setSettings(loadedSettings);
            saveSettings(settings);
        }
        start(JmeContext.Type.Display, true);
    }

    public void simpleInitApp() {

        DirectionalLight light = new DirectionalLight();
        light.setColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.9f));
        light.setDirection(new Vector3f(0.7f, 0.8f, 0.7f));
        this.getRootNode().addLight(light);

        DirectionalLight otherlight = new DirectionalLight();
        otherlight.setColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.9f));
        otherlight.setDirection(new Vector3f(-0.7f, 0.8f, -0.7f));
        this.getRootNode().addLight(otherlight);

        this.getFlyByCamera().setMoveSpeed(15);
        ground = new Geometry("ground", new Quad(10, 10));
        ground.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        ground.getMaterial().setColor("Color", ColorRGBA.Brown);
//Texture t = assetManager.loadTexture("assets/Textures/grass.jpg");
//t.setWrap(Texture.WrapMode.Repeat);
//ground.getMaterial().setTexture("ColorMap", t );

        ground.setLocalTranslation(0, 0, 10);
        ground.rotate(-90 * FastMath.DEG_TO_RAD, 0, 0);
        rootNode.attachChild(ground);

        windDirection.x = random.nextFloat();
        windDirection.y = random.nextFloat();
        windDirection.normalize();

        grassGeom = new Geometry("grass", new Quad(2, 2));

        grassShader = new Material(assetManager, "assets/MatDefs/Grass/MovingGrass.j3md");
        Texture grass = assetManager.loadTexture("assets/Textures/grass_3.png");
        grass.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
        grassShader.setTexture("Texture", grass);
//        grassShader.setFloat("AlphaDiscardThreshold", 1.0f);
        grassShader.setTexture("Noise", assetManager.loadTexture("assets/Textures/normal.jpg"));


// set wind direction
        grassShader.setVector2("WindDirection", windDirection);
        windStrength = 0.8f;
        grassShader.setFloat("WindStrength", windStrength);

        grassShader.setTransparent(true);
        grassShader.getAdditionalRenderState().setDepthTest(true);
        grassShader.getAdditionalRenderState().setDepthWrite(true);
        grassShader.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        grassShader.setColor("Color", new ColorRGBA(0.53f, 0.83f, 0.53f, 1f));
        grassShader.setFloat("Time", 0);

        grassShader.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        grassGeom.setQueueBucket(Bucket.Translucent);
        grassGeom.setCullHint(Spatial.CullHint.Never);
        grassGeom.setMaterial(grassShader);
        grassGeom.center();

        Vector3f grassBladePosition = null;
        for (int y = 0; y < 50; y++) {
            Geometry grassInstance = null;
            for (int x = 0; x < 50; x++) {
                grassBladePosition = new Vector3f(x + (float) (Math.random() * 1f), 0, y + (float) (Math.random() * 1f));;

                grassInstance = grassGeom.clone();
                grassInstance.setLocalTranslation(grassBladePosition);
                grassInstance.scale(0.4f, 0.4f + random.nextFloat() * .2f, 0.4f);
                grassInstance.rotate(CustomGrassTest.getRandomNumberInRange(0f, .55f), CustomGrassTest.getRandomNumberInRange(0f, 3.55f), CustomGrassTest.getRandomNumberInRange(0f, .55f));
                Node grassBladeNode = new Node();
                grassBladeNode.attachChild(grassInstance);
                grassBladeNode = GeometryBatchFactory.optimize(grassBladeNode, true);
                grassBladeNode.setQueueBucket(Bucket.Translucent);
                grassBladeNode.setCullHint(Spatial.CullHint.Never);
                grassBladeNode.setMaterial(grassShader);
                grassBladeNode.updateModelBound();

                allGrass.attachChild(grassBladeNode);
            }
        }

        rootNode.attachChild(allGrass);
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        cam.setLocation(new Vector3f(8.378951f, 5.4324f, 8.795956f));
        cam.setRotation(new Quaternion(-0.083419204f, 0.90370524f, -0.20599906f, -0.36595422f));

    }

    @Override
    public void simpleUpdate(float tpf) {
        elapsedTime += 0.01;
        grassShader.setFloat("Time", elapsedTime);
    }
}